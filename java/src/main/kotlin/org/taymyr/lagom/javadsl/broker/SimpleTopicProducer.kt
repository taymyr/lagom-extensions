package org.taymyr.lagom.javadsl.broker

import akka.Done
import akka.actor.ActorSystem
import akka.kafka.ProducerSettings
import akka.kafka.javadsl.Producer
import akka.stream.Materializer
import akka.stream.javadsl.Source
import com.lightbend.lagom.javadsl.api.ServiceLocator
import com.lightbend.lagom.javadsl.api.broker.Topic.TopicId
import com.lightbend.lagom.javadsl.api.broker.kafka.PartitionKeyStrategy
import com.typesafe.config.Config
import org.apache.kafka.clients.producer.ProducerRecord
import org.apache.kafka.common.serialization.Serializer
import org.apache.kafka.common.serialization.StringSerializer
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

    init {
        val producerConfigPath = "${topicId.value()}.producer"
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
    }

    /**
     * Publishes an entity to the topic using [Producer.plainSink].
     *
     * @param data An entity to publish to the topic
     */
    fun publish(data: T): CompletionStage<Done> {
        val key = partitionKeyStrategy?.computePartitionKey(data)
        val record = ProducerRecord<String, T>(topicId.value(), key, data)
        return Source.single(record).runWith(Producer.plainSink(producerSettings), materializer)
    }
}
