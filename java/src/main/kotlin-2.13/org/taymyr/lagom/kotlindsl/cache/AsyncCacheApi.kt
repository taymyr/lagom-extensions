package org.taymyr.lagom.kotlindsl.cache

import akka.Done
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.asCoroutineDispatcher
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.future.await
import kotlinx.coroutines.future.future
import scala.concurrent.ExecutionContextExecutor
import kotlin.time.Duration
import kotlin.time.toJavaDuration
import java.time.Duration as JDuration
import play.cache.AsyncCacheApi as JAsyncCacheApi

/**
 * The Cache API using coroutines.
 */
interface AsyncCacheApi {

    /**
     * Retrieves an object by key.
     *
     * @param key The key to look up.
     * @return Cache value or null if cache empty.
     */
    suspend fun <T> get(key: String): T?

    /**
     * Retrieve a value from the cache, or set it from a default function.
     *
     * @param key Item key.
     * @param block Returning value to set if key does not exist.
     * @return Cache value.
     */
    suspend fun <T> getOrElseUpdate(key: String, block: suspend () -> T): T

    /**
     * Retrieve a value from the cache, or set it from a default function.
     *
     * @param key Item key.
     * @param ttl Expiration period.
     * @param block Returning value to set if key does not exist.
     * @return Cache value.
     */
    suspend fun <T> getOrElseUpdate(key: String, ttl: Duration, block: suspend () -> T): T =
        getOrElseUpdate(key, ttl.toJavaDuration(), block)

    /**
     * Retrieve a value from the cache, or set it from a default function.
     *
     * @param key Item key.
     * @param ttl Expiration period.
     * @param block Returning value to set if key does not exist.
     * @return Cache value.
     */
    suspend fun <T> getOrElseUpdate(key: String, ttl: JDuration, block: suspend () -> T): T

    /**
     * Sets a value without expiration.
     *
     * @param key Item key.
     * @param value The value to set.
     * @return [Done]
     */
    suspend fun <T> set(key: String, value: T): Done

    /**
     * Sets a value with expiration.
     *
     * @param key Item key.
     * @param ttl Expiration period.
     * @param value The value to set.
     * @return [Done]
     */
    suspend fun <T> set(key: String, ttl: Duration, value: T): Done =
        set(key, ttl.toJavaDuration(), value)

    /**
     * Sets a value with expiration.
     *
     * @param key Item key.
     * @param ttl Expiration period.
     * @param value The value to set.
     * @return [Done]
     */
    suspend fun <T> set(key: String, ttl: JDuration, value: T): Done

    /**
     * Removes a value from the cache.
     *
     * @param key The key to remove the value for.
     * @return [Done]
     */
    suspend fun remove(key: String): Done

    /**
     * Removes all values from the cache.
     *
     * @return [Done]
     */
    suspend fun removeAll(): Done
}

internal class AsyncCacheApiImpl(
    private val cacheApi: JAsyncCacheApi,
    private val dispatcher: CoroutineDispatcher
) : AsyncCacheApi {

    override suspend fun <T> get(key: String): T? = cacheApi.get<T>(key).await().orElse(null)

    override suspend fun <T> getOrElseUpdate(key: String, block: suspend () -> T): T = coroutineScope {
        cacheApi.getOrElseUpdate(key) {
            // Creates a link to the parent coroutine
            val combinedContext = coroutineContext + dispatcher
            CoroutineScope(combinedContext).future { block() }
        }.await()
    }

    override suspend fun <T> getOrElseUpdate(key: String, ttl: JDuration, block: suspend () -> T): T = coroutineScope {
        cacheApi.getOrElseUpdate(
            key,
            {
                // Creates a link to the parent coroutine
                val combinedContext = coroutineContext + dispatcher
                CoroutineScope(combinedContext).future { block() }
            },
            ttl.seconds.toInt()
        ).await()
    }

    override suspend fun <T> set(key: String, value: T): Done = cacheApi.set(key, value).await()

    override suspend fun <T> set(key: String, ttl: JDuration, value: T): Done = cacheApi.set(key, value, ttl.seconds.toInt()).await()

    override suspend fun remove(key: String): Done = cacheApi.remove(key).await()

    override suspend fun removeAll(): Done = cacheApi.removeAll().await()
}

@Suppress("UNCHECKED_CAST")
private fun <R> Any.getPrivateProperty(name: String): R = try {
    this.javaClass.getDeclaredField(name)
        .apply { isAccessible = true }
        .get(this) as? R ?: throw NoSuchElementException("Not found private property value $name in ${this::class.qualifiedName}")
} catch (e: NoSuchFieldException) {
    throw NoSuchElementException("Not found private property $name in ${this::class.qualifiedName}")
}

/**
 * Creates an [AsyncCacheApi] based on [play.cache.AsyncCacheApi].
 * Using reflection, retrieves the execution context from the [play.cache.AsyncCacheApi] implementation.
 * The execution context is used to create the [CoroutineDispatcher].
 * Supported implementations: <a href="https://github.com/playframework/playframework/tree/master/cache/play-caffeine-cache">play-caffeine</a>,
 * <a href="https://github.com/KarelCemus/play-redis">play-redis</a>.
 *
 * @throws NoSuchElementException required property not found
 * @throws UnsupportedOperationException the current implementation will not support the cache implementation
 * @return [AsyncCacheApi]
 */
fun JAsyncCacheApi.suspend(): AsyncCacheApi {
    val context: ExecutionContextExecutor = when (this::class.qualifiedName) {
        "play.cache.DefaultAsyncCacheApi" -> {
            val asyncCacheApi = this.getPrivateProperty<Any>("asyncCacheApi")
            when (asyncCacheApi::class.qualifiedName) {
                "play.api.cache.caffeine.CaffeineCacheApi" -> asyncCacheApi.getPrivateProperty<ExecutionContextExecutor>("context")
                else -> throw UnsupportedOperationException("Implementation not supported for ${asyncCacheApi::class.qualifiedName}")
            }
        }
        "play.api.cache.redis.impl.AsyncJavaRedis" -> this.getPrivateProperty<Any>("runtime")
            .getPrivateProperty("context")
        else -> throw UnsupportedOperationException("Implementation not supported for ${this::class.qualifiedName}")
    }
    return AsyncCacheApiImpl(cacheApi = this, dispatcher = context.asCoroutineDispatcher())
}
