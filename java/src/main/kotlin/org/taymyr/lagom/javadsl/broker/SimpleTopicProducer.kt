package org.taymyr.lagom.javadsl.broker

import akka.actor.ActorSystem
import akka.kafka.ProducerSettings
import akka.kafka.javadsl.Producer
import akka.stream.Materializer
import akka.stream.OverflowStrategy
import akka.stream.QueueOfferResult
import akka.stream.javadsl.RestartSink
import akka.stream.javadsl.Source
import akka.stream.javadsl.SourceQueueWithComplete
import com.lightbend.lagom.javadsl.api.ServiceLocator
import com.lightbend.lagom.javadsl.api.broker.Topic.TopicId
import com.lightbend.lagom.javadsl.api.broker.kafka.PartitionKeyStrategy
import com.typesafe.config.Config
import org.apache.kafka.clients.producer.ProducerRecord
import org.apache.kafka.common.serialization.Serializer
import org.apache.kafka.common.serialization.StringSerializer
import java.time.Duration
import java.util.concurrent.CompletionStage
import java.util.concurrent.TimeUnit

/**
 * Simple producer for publishing single records to the topic.
 *
 * @param T Type of a topic record
 */
class SimpleTopicProducer<T> internal constructor(
    serviceLocator: ServiceLocator,
    private val topicId: TopicId,
    private val partitionKeyStrategy: PartitionKeyStrategy<T>?,
    messageSerializer: Serializer<T>,
    private val materializer: Materializer,
    actorSystem: ActorSystem,
    config: Config
) {
    private val producerSettings: ProducerSettings<String, T>
    private val source: SourceQueueWithComplete<T>

    init {
        val producerConfigPath = "${topicId.value()}.producer"
        val bufferSizePath = "${topicId.value()}.producer.buffer-size"
        val overflowStrategyPath = "${topicId.value()}.producer.overflow-strategy"
        val minBackoffPath = "${topicId.value()}.producer.min-backoff"
        val maxBackoffPath = "${topicId.value()}.producer.max-backoff"
        val randomFactorPath = "${topicId.value()}.producer.random-factor"
        val bufferSize = if (config.hasPath(bufferSizePath)) config.getInt(bufferSizePath) else 100
        val producerSettings = if (config.hasPath(producerConfigPath)) {
            ProducerSettings.create(
                config.getConfig(producerConfigPath),
                StringSerializer(),
                messageSerializer
            )
        } else {
            ProducerSettings.create(actorSystem, StringSerializer(), messageSerializer)
        }
        val serviceNameConfigPath = "${topicId.value()}.serviceName"
        if (config.hasPath(serviceNameConfigPath)) {
            this.producerSettings = producerSettings.withBootstrapServers(
                serviceLocator.locateAll(config.getString(serviceNameConfigPath)).thenApply { uris ->
                    uris.filter { uri -> uri.host != null && uri.port != -1 }.joinToString(",") { it.authority }
                }.toCompletableFuture().get(5, TimeUnit.SECONDS) // :'(
            )
        } else {
            this.producerSettings = producerSettings
        }
        val overflowStrategy = when (if (config.hasPath(overflowStrategyPath))
            config.getString(overflowStrategyPath) else "dropHead") {
            "dropHead" -> OverflowStrategy.dropHead()
            "backpressure" -> OverflowStrategy.backpressure()
            "dropBuffer" -> OverflowStrategy.dropBuffer()
            "dropNew" -> OverflowStrategy.dropNew()
            "dropTail" -> OverflowStrategy.dropTail()
            "fail" -> OverflowStrategy.fail()
            else -> throw IllegalArgumentException("Unknown value overflow-strategy, " +
                "expected [dropHead, backpressure, dropBuffer, dropNew, dropTail, fail]")
        }
        val minBackoff = if (config.hasPath(minBackoffPath))
            config.getDuration(minBackoffPath)
        else Duration.ofSeconds(3)
        val maxBackoff = if (config.hasPath(maxBackoffPath))
            config.getDuration(maxBackoffPath)
        else Duration.ofSeconds(30)
        val randomFactor = if (config.hasPath(randomFactorPath))
            config.getDouble(randomFactorPath)
        else 0.2

        val topicNameConfigPath = "${topicId.value()}.topic-name"
        val topicName = if (config.hasPath(topicNameConfigPath)) config.getString(topicNameConfigPath) else topicId.value()

        this.source = Source.queue<T>(bufferSize, overflowStrategy)
            .map { ProducerRecord<String, T>(topicName, partitionKeyStrategy?.computePartitionKey(it), it) }
            .to(RestartSink.withBackoff(minBackoff, maxBackoff, randomFactor) { Producer.plainSink(this.producerSettings) })
            .run(materializer)
    }

    /**
     * Publishes an entity to the topic.
     *
     * @param data An entity to publish to the topic
     */
    fun publish(data: T): CompletionStage<QueueOfferResult>? {
        return source.offer(data)
    }
}
