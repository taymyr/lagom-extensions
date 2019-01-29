package org.taymyr.lagom.javadsl.broker;

import com.lightbend.lagom.javadsl.client.integration.LagomClientFactory;
import com.lightbend.lagom.javadsl.testkit.ServiceTest;
import com.lightbend.lagom.javadsl.testkit.ServiceTest.Setup;
import com.salesforce.kafka.test.junit5.SharedKafkaTestResource;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import scala.concurrent.duration.Duration;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.List;
import java.util.UUID;

import static com.lightbend.lagom.javadsl.testkit.ServiceTest.eventually;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;
import static org.taymyr.lagom.javadsl.broker.TestTopicService.TOPIC_WITHOUT_KEYS;
import static org.taymyr.lagom.javadsl.broker.TestTopicService.TOPIC_WITH_KEYS;

import static java.util.concurrent.TimeUnit.SECONDS;

class SimpleTopicProducerTest {

    @RegisterExtension
    static final SharedKafkaTestResource sharedKafkaTestResource = new SharedKafkaTestResource();

    private static ServiceTest.TestServer testServer = null;
    private static LagomClientFactory clientFactory = null;

    @BeforeAll
    static void beforeAll() throws URISyntaxException {
        int kafkaBrokerPort = URI.create(sharedKafkaTestResource.getKafkaConnectString()).getPort();
        clientFactory = LagomClientFactory.create("test-topic-service", LagomClientFactory.class.getClassLoader());
        TestTopicService testTopicService = clientFactory.createClient(TestTopicService.class, new URI("http://localhost:" + kafkaBrokerPort));
        Setup setup = ServiceTest.defaultSetup().configureBuilder(builder ->
                builder.overrides(
                        ServiceTest.bind(TestTopicService.class).toInstance(testTopicService)
                ).configure("akka.kafka.producer.kafka-clients.bootstrap.servers", "localhost:" + kafkaBrokerPort)
        );
        testServer = ServiceTest.startServer(setup);
    }

    @AfterAll
    static void afterAll() {
        if (testServer != null) testServer.stop();
        if (clientFactory != null) clientFactory.close();
    }

    @Test
    void testTopicTypeDescriptor() {
        TopicDescriptor a = TopicDescriptor.of("test-topic", String.class);
        TopicDescriptor  b = TopicDescriptor.of("test-topic", String.class);
        assertThat(a.getId()).isNotEmpty();
        assertThat(a.getType()).isNotNull();
        assertThat(a).isEqualTo(b);
    }

    @Test
    @DisplayName("Test successful publishing with no partition key strategy")
    void testPublishingWithoutKeys() {
        String uuid = UUID.randomUUID().toString();
        testServer.injector().instanceOf(TestTopicPublisher.class).publishWithoutKey(uuid).invoke().toCompletableFuture().join();
        eventually(Duration.create(5, SECONDS), () -> {
            List<ConsumerRecord<String, String>> records = sharedKafkaTestResource.getKafkaTestUtils().consumeAllRecordsFromTopic(
                TOPIC_WITHOUT_KEYS.getId(),
                StringDeserializer.class,
                StringDeserializer.class
            );
            assertThat(records).extracting("key", "value").contains(tuple(null, uuid));
        });
    }

    @Test
    @DisplayName("Test successful publishing with partition key strategy")
    void testPublishingWithKeys() {
        String uuid = UUID.randomUUID().toString();
        testServer.injector().instanceOf(TestTopicPublisher.class).publishWithKey(uuid).invoke().toCompletableFuture().join();
        eventually(Duration.create(5, SECONDS), () -> {
            List<ConsumerRecord<String, String>> records = sharedKafkaTestResource.getKafkaTestUtils().consumeAllRecordsFromTopic(
                TOPIC_WITH_KEYS.getId(),
                StringDeserializer.class,
                StringDeserializer.class
            );
            assertThat(records).extracting("key", "value").contains(tuple("" + uuid.hashCode(), uuid));
        });
    }
}
