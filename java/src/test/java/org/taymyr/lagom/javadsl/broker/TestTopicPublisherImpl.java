package org.taymyr.lagom.javadsl.broker;

import akka.NotUsed;
import com.lightbend.lagom.javadsl.api.ServiceCall;
import javax.inject.Inject;

class TestTopicPublisherImpl implements TestTopicPublisher {

    private SimpleTopicProducersRegistry simpleTopicProducersRegistry;

    @Inject
    public TestTopicPublisherImpl(TestTopicService testTopicService, SimpleTopicProducersRegistry simpleTopicProducersRegistry) {
        this.simpleTopicProducersRegistry = simpleTopicProducersRegistry.register(testTopicService);
    }

    @Override
    public ServiceCall<NotUsed, NotUsed> publishWithoutKey(String msg) {
        return notUsed ->
                simpleTopicProducersRegistry.get(TestTopicService.TOPIC_WITHOUT_KEYS).publish(msg)
                    .thenApply( x -> NotUsed.getInstance() );
    }

    @Override
    public ServiceCall<NotUsed, NotUsed> publishWithKey(String msg) {
        return notUsed ->
                simpleTopicProducersRegistry.get(TestTopicService.TOPIC_WITH_KEYS).publish(msg)
                    .thenApply( x -> NotUsed.getInstance() );
    }
}