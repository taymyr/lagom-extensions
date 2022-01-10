package org.taymyr.lagom.kotlindsl.cache

import akka.Done
import com.lightbend.lagom.javadsl.testkit.ServiceTest
import com.typesafe.config.ConfigFactory
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import java.time.Duration
import play.cache.AsyncCacheApi as JAsyncCacheApi

class AsyncCacheApiTest {
    companion object {
        private lateinit var testServer: ServiceTest.TestServer
        private lateinit var cacheApi: AsyncCacheApi

        @BeforeAll
        @JvmStatic
        fun beforeAll() {
            val setup = ServiceTest.defaultSetup().configureBuilder {
                it.loadConfig(ConfigFactory.load("cache.conf"))
            }
            testServer = ServiceTest.startServer(setup)
            cacheApi = testServer.injector().instanceOf(JAsyncCacheApi::class.java).suspend()
        }

        @AfterAll
        @JvmStatic
        fun afterAll() {
            testServer.stop()
        }
    }

    @Test
    @DisplayName("successful get empty value from cache")
    fun testEmptyCache(): Unit = runBlocking {
        assertThat(cacheApi.get<String>("empty value")).isNull()
    }

    @Test
    @DisplayName("successfully put the value into the cache and get it back")
    fun testSetAndGetFromCache(): Unit = runBlocking {
        assertThat(cacheApi.set("key", "not empty value")).isEqualTo(Done.done())
        assertThat(cacheApi.get<String>("key")).isEqualTo("not empty value")
    }

    @Test
    @DisplayName("successfully put the value into the cache and get it back using the function")
    fun testGetOrElseUpdate(): Unit = runBlocking {
        assertThat(cacheApi.getOrElseUpdate("key2") { "not empty value for key2" }).isEqualTo("not empty value for key2")
        assertThat(cacheApi.get<String>("key2")).isEqualTo("not empty value for key2")
    }

    @Test
    @DisplayName("successfully put the value into the cache (with ttl) and get it back")
    fun testSetAndGetFromCacheWithTtl(): Unit = runBlocking {
        assertThat(cacheApi.set("key3", Duration.ofSeconds(1), "not empty value for key3")).isEqualTo(Done.done())
        assertThat(cacheApi.get<String>("key3")).isEqualTo("not empty value for key3")
        delay(1000)
        assertThat(cacheApi.get<String>("key3")).isNull()
    }

    @Test
    @DisplayName("successfully put the value into the cache (with ttl) and get it back using the function")
    fun testGetOrElseUpdateWithTtl(): Unit = runBlocking {
        assertThat(cacheApi.getOrElseUpdate("key4", Duration.ofSeconds(1)) { "not empty value for key4" })
            .isEqualTo("not empty value for key4")
        assertThat(cacheApi.get<String>("key4")).isEqualTo("not empty value for key4")
        delay(1500)
        assertThat(cacheApi.get<String>("key4")).isNull()
    }

    @Test
    @DisplayName("successfully delete keys from cache")
    fun testRemoveFromCache(): Unit = runBlocking {
        assertThat(cacheApi.set("key5", "not empty value fro key5")).isEqualTo(Done.done())
        assertThat(cacheApi.set("key6", "not empty value fro key6")).isEqualTo(Done.done())
        assertThat(cacheApi.remove("key5")).isEqualTo(Done.done())
        assertThat(cacheApi.get<String>("key5")).isNull()
        assertThat(cacheApi.get<String>("key6")).isEqualTo("not empty value fro key6")
        assertThat(cacheApi.removeAll()).isEqualTo(Done.done())
        assertThat(cacheApi.get<String>("key")).isNull()
        assertThat(cacheApi.get<String>("key1")).isNull()
        assertThat(cacheApi.get<String>("key2")).isNull()
        assertThat(cacheApi.get<String>("key3")).isNull()
        assertThat(cacheApi.get<String>("key4")).isNull()
        assertThat(cacheApi.get<String>("key5")).isNull()
        assertThat(cacheApi.get<String>("key6")).isNull()
    }
}
