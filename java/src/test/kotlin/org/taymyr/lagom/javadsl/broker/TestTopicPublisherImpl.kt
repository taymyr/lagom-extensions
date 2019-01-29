package org.taymyr.lagom.javadsl.broker

import akka.NotUsed
import com.lightbend.lagom.javadsl.api.ServiceCall
import javax.inject.Inject

class TestTopicPublisherImpl @Inject constructor(
    testTopicService: TestTopicService,
    simpleTopicProducersRegistry: SimpleTopicProducersRegistry
) : TestTopicPublisher {
    private val simpleTopicProducersRegistry: SimpleTopicProducersRegistry =
        simpleTopicProducersRegistry.register(testTopicService)

    override fun publishWithoutKey(msg: String): ServiceCall<NotUsed, NotUsed> {
        return ServiceCall {
            simpleTopicProducersRegistry.get(TestTopicService.TOPIC_WITHOUT_KEYS).publish(msg)
                .thenApply { NotUsed.getInstance() }
        }
    }

    override fun publishWithKey(msg: String): ServiceCall<NotUsed, NotUsed> {
        return ServiceCall {
            simpleTopicProducersRegistry.get(TestTopicService.TOPIC_WITH_KEYS).publish(msg)
                .thenApply { NotUsed.getInstance() }
        }
    }
}