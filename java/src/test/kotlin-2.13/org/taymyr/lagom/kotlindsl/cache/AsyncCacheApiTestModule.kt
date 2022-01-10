package org.taymyr.lagom.kotlindsl.cache

import com.google.inject.AbstractModule
import com.lightbend.lagom.javadsl.api.Descriptor
import com.lightbend.lagom.javadsl.api.Service
import com.lightbend.lagom.javadsl.server.ServiceGuiceSupport

class AsyncCacheApiTestModule : AbstractModule(), ServiceGuiceSupport {

    override fun configure() {
        bindService(TestMockService::class.java, TestMockServiceImpl::class.java)
    }
}

interface TestMockService : Service {

    override fun descriptor(): Descriptor {
        return Service.named("cache-test-service")
    }
}

class TestMockServiceImpl : TestMockService
