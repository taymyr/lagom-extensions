package org.taymyr.lagom.javadsl.api

import com.lightbend.lagom.javadsl.api.ServiceCall
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.future.future
import kotlin.coroutines.CoroutineContext
import kotlin.coroutines.EmptyCoroutineContext

/**
 * Starts new coroutine and returns its result as an implementation of [ServiceCall].
 *
 * @param context additional to [CoroutineScope.coroutineContext] context of the coroutine.
 * @param start coroutine start option. The default value is [CoroutineStart.DEFAULT].
 * @param block the coroutine code.
 */
fun <Request, Response> serviceCall(
    context: CoroutineContext = EmptyCoroutineContext,
    start: CoroutineStart = CoroutineStart.DEFAULT,
    block: suspend CoroutineScope.(request: Request) -> Response
) = ServiceCall<Request, Response> {
    CoroutineScope(Dispatchers.Unconfined).future(context, start) {
        block(it)
    }
}