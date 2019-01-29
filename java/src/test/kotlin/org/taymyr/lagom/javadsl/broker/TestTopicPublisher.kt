package org.taymyr.lagom.javadsl.broker

import akka.NotUsed
import com.lightbend.lagom.javadsl.api.Descriptor
import com.lightbend.lagom.javadsl.api.Service
import com.lightbend.lagom.javadsl.api.Service.named
import com.lightbend.lagom.javadsl.api.Service.pathCall
import com.lightbend.lagom.javadsl.api.ServiceCall
import kotlin.reflect.jvm.javaMethod

interface TestTopicPublisher : Service {
    fun publishWithoutKey(msg: String): ServiceCall<NotUsed, NotUsed>

    fun publishWithKey(msg: String): ServiceCall<NotUsed, NotUsed>

    @JvmDefault
    override fun descriptor(): Descriptor {
        return named("test-topic-publisher").withCalls(
            pathCall<NotUsed, NotUsed>("/test-topic-publisher/without-key/:msg", TestTopicPublisher::publishWithoutKey.javaMethod),
            pathCall<NotUsed, NotUsed>("/test-topic-publisher/with-key/:key", TestTopicPublisher::publishWithKey.javaMethod)
        ).withAutoAcl(true)
    }
}