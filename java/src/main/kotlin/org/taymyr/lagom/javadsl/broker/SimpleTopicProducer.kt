package org.taymyr.lagom.javadsl.broker

import akka.Done
import akka.actor.ActorSystem
import akka.kafka.ProducerSettings
import akka.kafka.javadsl.Producer
import akka.stream.Materializer
import akka.stream.javadsl.Source
import com.lightbend.lagom.javadsl.api.broker.Topic.TopicId
import com.lightbend.lagom.javadsl.api.broker.kafka.PartitionKeyStrategy
import com.typesafe.config.Config
import org.apache.kafka.clients.producer.ProducerRecord
import org.apache.kafka.common.serialization.Serializer
import org.apache.kafka.common.serialization.StringSerializer

import java.util.concurrent.CompletionStage

/**
 * Simple producer for publishing single records to the topic.
 *
 * @param T Type of a topic record
 */
class SimpleTopicProducer<T> internal constructor(
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
        this.producerSettings = if (config.hasPath(producerConfigPath)) {
            ProducerSettings.create(
                config.getConfig(producerConfigPath),
                StringSerializer(),
                messageSerializer
            )
        } else {
            ProducerSettings.create(actorSystem, StringSerializer(), messageSerializer)
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
