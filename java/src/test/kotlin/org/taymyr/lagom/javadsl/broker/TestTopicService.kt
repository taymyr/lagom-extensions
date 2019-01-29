package org.taymyr.lagom.javadsl.broker

import com.lightbend.lagom.javadsl.api.Descriptor
import com.lightbend.lagom.javadsl.api.Service
import com.lightbend.lagom.javadsl.api.Service.named
import com.lightbend.lagom.javadsl.api.Service.topic
import com.lightbend.lagom.javadsl.api.broker.Topic
import com.lightbend.lagom.javadsl.api.broker.kafka.KafkaProperties
import com.lightbend.lagom.javadsl.api.broker.kafka.PartitionKeyStrategy
import kotlin.reflect.jvm.javaMethod

interface TestTopicService : Service {
    fun topicWithoutKeys(): Topic<String>
    fun topicWithKeys(): Topic<String>

    @JvmDefault
    override fun descriptor(): Descriptor {
        return named("test-topic-service")
            .withTopics(
                topic<String>(TOPIC_WITHOUT_KEYS.id, TestTopicService::topicWithoutKeys.javaMethod),
                topic<String>(TOPIC_WITH_KEYS.id, TestTopicService::topicWithKeys.javaMethod)
                    .withProperty(KafkaProperties.partitionKeyStrategy(), PartitionKeyStrategy { it -> "${it.hashCode()}" })
            ).withAutoAcl(true)
    }

    companion object {
        val TOPIC_WITHOUT_KEYS = TopicDescriptor.of("topic-without-keys", String::class.java)
        val TOPIC_WITH_KEYS = TopicDescriptor.of("topic-with-keys", String::class.java)
    }
}