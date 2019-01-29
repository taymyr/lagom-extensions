package org.taymyr.lagom.javadsl.broker

import com.lightbend.lagom.javadsl.client.integration.LagomClientFactory
import com.lightbend.lagom.javadsl.testkit.ServiceTest
import com.lightbend.lagom.javadsl.testkit.ServiceTest.eventually
import com.salesforce.kafka.test.junit5.SharedKafkaTestResource
import org.apache.kafka.common.serialization.StringDeserializer
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.groups.Tuple.tuple
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.RegisterExtension
import scala.concurrent.duration.Duration
import java.net.URI
import java.util.UUID
import java.util.concurrent.TimeUnit.SECONDS

class SimpleTopicProducerTest {

    @Test
    fun testTopicTypeDescriptor() {
        val a = TopicDescriptor.of("test-topic", String::class.java)
        val b = TopicDescriptor.of("test-topic", String::class.java)
        assertThat(a.id).isNotEmpty()
        assertThat(a.type).isNotNull
        assertThat(a).isEqualTo(b)
    }

    @Test
    @DisplayName("Test successful publishing with no partition key strategy")
    fun testPublishingWithoutKeys() {
        val uuid = UUID.randomUUID().toString()
        testServer?.injector()?.instanceOf(TestTopicPublisher::class.java)?.publishWithoutKey(uuid)?.invoke()?.toCompletableFuture()?.join()
        eventually(Duration.create(5, SECONDS)) {
            val records = sharedKafkaTestResource.kafkaTestUtils.consumeAllRecordsFromTopic(
                TestTopicService.TOPIC_WITHOUT_KEYS.id,
                StringDeserializer::class.java,
                StringDeserializer::class.java
            )
            assertThat(records).extracting("key", "value").contains(tuple(null, uuid))
        }
    }

    @Test
    @DisplayName("Test successful publishing with partition key strategy")
    fun testPublishingWithKeys() {
        val uuid = UUID.randomUUID().toString()
        testServer?.injector()?.instanceOf(TestTopicPublisher::class.java)?.publishWithKey(uuid)?.invoke()?.toCompletableFuture()?.join()
        eventually(Duration.create(5, SECONDS)) {
            val records = sharedKafkaTestResource.kafkaTestUtils.consumeAllRecordsFromTopic(
                TestTopicService.TOPIC_WITH_KEYS.id,
                StringDeserializer::class.java,
                StringDeserializer::class.java
            )
            assertThat(records).extracting("key", "value").contains(tuple("${uuid.hashCode()}", uuid))
        }
    }

    companion object {
        @RegisterExtension
        @JvmField
        val sharedKafkaTestResource = SharedKafkaTestResource()

        var testServer: ServiceTest.TestServer? = null
        var clientFactory: LagomClientFactory? = null

        @BeforeAll
        @JvmStatic
        internal fun beforeAll() {
            val kafkaBrokerPort = URI.create(sharedKafkaTestResource.kafkaConnectString).port
            clientFactory = LagomClientFactory.create("test-topic-service", LagomClientFactory::class.java.classLoader)
            val testTopicService = clientFactory?.createClient(TestTopicService::class.java, URI("http://localhost:$kafkaBrokerPort"))
            val setup = ServiceTest.defaultSetup().configureBuilder { builder ->
                builder.overrides(
                    ServiceTest.bind(TestTopicService::class.java).toInstance(testTopicService)
                ).configure("akka.kafka.producer.kafka-clients.bootstrap.servers", "localhost:$kafkaBrokerPort")
            }
            testServer = ServiceTest.startServer(setup)
        }

        @AfterAll
        @JvmStatic
        internal fun afterAll() {
            testServer?.stop()
            clientFactory?.close()
        }
    }
}
