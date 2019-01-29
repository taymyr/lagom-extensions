package org.taymyr.lagom.javadsl.broker;

import com.lightbend.lagom.javadsl.api.Descriptor;
import com.lightbend.lagom.javadsl.api.Service;
import com.lightbend.lagom.javadsl.api.broker.Topic;
import com.lightbend.lagom.javadsl.api.broker.kafka.KafkaProperties;

import static com.lightbend.lagom.javadsl.api.Service.named;

public interface TestTopicService extends Service {

    TopicDescriptor<String> TOPIC_WITHOUT_KEYS = TopicDescriptor.of("topic-without-keys", String.class);
    TopicDescriptor<String> TOPIC_WITH_KEYS = TopicDescriptor.of("topic-with-keys", String.class);

    Topic<String> topicWithoutKeys();
    Topic<String> topicWithKeys();

    @Override
    default Descriptor descriptor() {
        return named("test-topic-service")
                .withTopics(
                        Service.topic(TOPIC_WITHOUT_KEYS.getId(), this::topicWithoutKeys),
                        Service.topic(TOPIC_WITH_KEYS.getId(), this::topicWithKeys)
                            .withProperty(KafkaProperties.partitionKeyStrategy(), it -> "" + it.hashCode())
            ).withAutoAcl(true);
    }

}