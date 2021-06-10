package org.taymyr.lagom.javadsl.api

import akka.NotUsed
import com.lightbend.lagom.javadsl.api.Descriptor
import com.lightbend.lagom.javadsl.api.Service
import com.lightbend.lagom.javadsl.api.Service.named
import com.lightbend.lagom.javadsl.api.Service.restCall
import com.lightbend.lagom.javadsl.api.ServiceCall
import com.lightbend.lagom.javadsl.api.transport.Method
import kotlin.reflect.jvm.javaMethod

interface TestService : Service {

    fun testMethod(): ServiceCall<NotUsed, String>

    override fun descriptor(): Descriptor {
        return named("test-service")
            .withCalls(
                restCall<NotUsed, String>(Method.GET, "/test", TestService::testMethod.javaMethod)
            )
    }
}
