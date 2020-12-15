package org.taymyr.lagom.javadsl.broker

import akka.actor.ActorSystem
import akka.kafka.ProducerSettings
import akka.kafka.javadsl.Producer
import akka.kafka.javadsl.SendProducer
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
import org.apache.kafka.clients.producer.RecordMetadata
import org.apache.kafka.common.serialization.Serializer
import org.apache.kafka.common.serialization.StringSerializer
import scala.compat.java8.FutureConverters.toJava
import java.time.Duration
import java.util.concurrent.CompletionStage
import java.util.concurrent.TimeUnit
import akka.kafka.scaladsl.`SendProducer$`.`MODULE$` as ScalaSendProducerCompanion

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
    private val actorSystem: ActorSystem,
    config: Config
) {
    private val producerSettings: ProducerSettings<String, T>
    private val queue: SourceQueueWithComplete<T>
    private val topicName: String

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
        topicName = if (config.hasPath(topicNameConfigPath)) config.getString(topicNameConfigPath)!! else topicId.value()

        queue = Source.queue<T>(bufferSize, overflowStrategy)
            .map { toProducerRecord(it) }
            .to(RestartSink.withBackoff(minBackoff, maxBackoff, randomFactor) { Producer.plainSink(this.producerSettings) })
            .run(materializer)
    }

    private fun toProducerRecord(data: T) =
        ProducerRecord<String, T>(topicName, partitionKeyStrategy?.computePartitionKey(data), data)

    /**
     * Enqueues an entity to be further published to the topic.
     *
     * @param data An entity to publish to the topic
     */
    @Deprecated("Use 'enqueue' method instead. This should be renamed/removed at future releases")
    fun publish(data: T): CompletionStage<QueueOfferResult> = enqueue(data)

    /**
     * Enqueues an entity to be further published to the topic.
     *
     * @param data An entity to publish to the topic
     */
    fun enqueue(data: T): CompletionStage<QueueOfferResult> = queue.offer(data)

    /**
     * Publishes an entity to the topic using [SendProducer.send].
     *
     * @param data An entity to publish to the topic
     */
    fun send(data: T): CompletionStage<RecordMetadata> = toJava(
        ScalaSendProducerCompanion.apply(producerSettings, actorSystem).send(toProducerRecord(data))
    )
}
