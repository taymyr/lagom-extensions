package org.taymyr.lagom.javadsl.broker;

import com.google.inject.AbstractModule;
import com.lightbend.lagom.javadsl.server.ServiceGuiceSupport;

public class SimpleTopicProducerTestModule extends AbstractModule implements ServiceGuiceSupport {

    @Override
    protected void configure() {
        bindService(TestTopicPublisher.class, TestTopicPublisherImpl.class);
    }
}