package org.taymyr.lagom.javadsl.api

import com.lightbend.lagom.javadsl.api.ServiceCall
import kotlinx.coroutines.future.await

/**
 * Helper extension method for [ServiceCall.invoke] call chain.
 */
suspend fun <Request, Response> ServiceCall<Request, Response>.await(): Response = this.invoke().await()
