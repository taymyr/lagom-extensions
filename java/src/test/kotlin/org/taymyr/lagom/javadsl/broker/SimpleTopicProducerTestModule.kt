package org.taymyr.lagom.javadsl.broker

import com.google.inject.AbstractModule
import com.lightbend.lagom.javadsl.server.ServiceGuiceSupport

class SimpleTopicProducerTestModule : AbstractModule(), ServiceGuiceSupport {
    override fun configure() {
        bindService(TestTopicPublisher::class.java, TestTopicPublisherImpl::class.java)
    }
}