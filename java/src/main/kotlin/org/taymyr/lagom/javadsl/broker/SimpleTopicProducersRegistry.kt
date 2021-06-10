package org.taymyr.lagom.javadsl.broker

import akka.actor.ActorSystem
import akka.stream.Materializer
import akka.util.ByteString
import com.lightbend.lagom.internal.javadsl.api.MethodTopicHolder
import com.lightbend.lagom.javadsl.api.Descriptor.TopicCall
import com.lightbend.lagom.javadsl.api.Service
import com.lightbend.lagom.javadsl.api.ServiceLocator
import com.lightbend.lagom.javadsl.api.broker.Topic
import com.lightbend.lagom.javadsl.api.broker.kafka.KafkaProperties
import com.lightbend.lagom.javadsl.api.deser.MessageSerializer.NegotiatedSerializer
import com.typesafe.config.Config
import org.apache.kafka.common.serialization.Serializer
import org.slf4j.LoggerFactory
import java.util.concurrent.ConcurrentHashMap
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Registry of [SimpleTopicProducer].
 */
@Singleton
class SimpleTopicProducersRegistry @Inject constructor(
    private val config: Config,
    private val serviceLocator: ServiceLocator,
    private val materializer: Materializer,
    private val actorSystem: ActorSystem
) {
    private val producers = ConcurrentHashMap<String, SimpleTopicProducer<*>>()

    /**
     * Register [SimpleTopicProducer]s for [Topic]s of a [Service].
     *
     * @param service Service descriptor
     * @param topics Topics for registration. It can be missed to register all topics.
     * @return [SimpleTopicProducersRegistry]
     */
    @Suppress("unchecked_cast")
    fun register(service: Service, vararg topics: Topic<*>): SimpleTopicProducersRegistry {
        service.descriptor().topicCalls()
            .filter { tc -> topics.isEmpty() || topics.map { it.topicId() }.contains(tc.topicId()) }
            .forEach { topicCall ->
                if (topicCall.topicHolder() is MethodTopicHolder) {
                    val topicId = topicCall.topicId()
                    producers[topicId.value()] = SimpleTopicProducer(
                        serviceLocator,
                        topicId,
                        (topicCall as TopicCall<Any>).properties().getValueOf(KafkaProperties.partitionKeyStrategy()),
                        TopicMessageSerializer(topicCall.messageSerializer().serializerForRequest()),
                        materializer, actorSystem, config
                    )
                } else {
                    log.error(
                        """
                    Can not register simple topic producer for topic ${topicCall.topicId()}.
                    Reason was that it was expected a topicHolder of type ${MethodTopicHolder::class.java.name}
                    but ${topicCall?.topicHolder()?.javaClass?.name} was found instead.
                    """.trimIndent()
                    )
                }
            }
        return this
    }

    /**
     * Retrieve simple topic producer from registry.
     *
     * @param topicDescriptor Topic descriptor
     * @param M Type of topic record
     * @return [SimpleTopicProducer]
     * @throws IllegalArgumentException If topic not registered
     */
    @Suppress("unchecked_cast")
    fun <M> get(topicDescriptor: TopicDescriptor<M>): SimpleTopicProducer<M> {
        val p = producers[topicDescriptor.id]
            ?: throw IllegalArgumentException("Topic with name ${topicDescriptor.id} and record type ${topicDescriptor.type} is not registered.")
        return p as SimpleTopicProducer<M>
    }

    /**
     * Implements [Serializer] reusing [NegotiatedSerializer] to serialize an entity.
     *
     * @param M type of an entity being serialized
     */
    private inner class TopicMessageSerializer<M>(private val serializer: NegotiatedSerializer<M, ByteString>) :
        Serializer<M> {

        override fun configure(configs: Map<String, *>, isKey: Boolean) {}

        override fun serialize(topic: String, data: M): ByteArray = serializer.serialize(data).toArray()

        override fun close() {}
    }

    companion object {
        private val log = LoggerFactory.getLogger(SimpleTopicProducersRegistry::class.java)
    }
}
