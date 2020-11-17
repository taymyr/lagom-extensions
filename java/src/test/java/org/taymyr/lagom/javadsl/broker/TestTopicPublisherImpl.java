package org.taymyr.lagom.javadsl.broker;

import akka.NotUsed;
import com.lightbend.lagom.javadsl.api.ServiceCall;

import javax.inject.Inject;

import static akka.NotUsed.notUsed;

class TestTopicPublisherImpl implements TestTopicPublisher {
    private SimpleTopicProducersRegistry simpleTopicProducersRegistry;

    @Inject
    public TestTopicPublisherImpl(TestTopicService testTopicService, SimpleTopicProducersRegistry simpleTopicProducersRegistry) {
        this.simpleTopicProducersRegistry = simpleTopicProducersRegistry.register(testTopicService);
    }

    @Override
    public ServiceCall<NotUsed, NotUsed> publishWithoutKey(String msg) {
        return notUsed -> simpleTopicProducersRegistry.get(TestTopicService.TOPIC_WITHOUT_KEYS).send(msg)
            .thenApply(x -> notUsed());
    }

    @Override
    public ServiceCall<NotUsed, NotUsed> publishWithKey(String msg) {
        return notUsed -> simpleTopicProducersRegistry.get(TestTopicService.TOPIC_WITH_KEYS).send(msg)
            .thenApply(x -> notUsed());
    }

    @Override
    public ServiceCall<NotUsed, NotUsed> enqueueToPublish(String msg) {
        return notUsed -> simpleTopicProducersRegistry.get(TestTopicService.TOPIC_WITH_KEYS).enqueue(msg)
            .thenApply(x -> notUsed());
    }
}
