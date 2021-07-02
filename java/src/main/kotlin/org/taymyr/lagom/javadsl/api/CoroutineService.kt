package org.taymyr.lagom.javadsl.api

import com.lightbend.lagom.javadsl.api.ServiceCall
import com.lightbend.lagom.javadsl.api.transport.RequestHeader
import com.lightbend.lagom.javadsl.api.transport.ResponseHeader
import com.lightbend.lagom.javadsl.server.HeaderServiceCall
import com.lightbend.lagom.javadsl.server.ServerServiceCall
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.future.future

/**
 * Starts new coroutine on threads from [CoroutineDispatcher] and returns its result as an implementation of [ServiceCall].
 *
 * @property dispatcher determines what thread or threads the corresponding coroutine uses for its execution.
 */
interface CoroutineService {

    val dispatcher: CoroutineDispatcher

    /**
     * Starts new coroutine and returns its result as an implementation of [ServiceCall].
     *
     * @param start coroutine start option. The default value is [CoroutineStart.DEFAULT].
     * @param block the coroutine code.
     */
    fun <Request, Response> serviceCall(
        start: CoroutineStart = CoroutineStart.DEFAULT,
        block: suspend CoroutineScope.(request: Request) -> Response
    ) = ServiceCall<Request, Response> {
        CoroutineScope(dispatcher).future(start = start) {
            block(it)
        }
    }

    /**
     * Starts new coroutine and returns its result as an implementation of [ServerServiceCall].
     *
     * @param start coroutine start option. The default value is [CoroutineStart.DEFAULT].
     * @param block the coroutine code.
     */
    fun <Request, Response> serverServiceCall(
        start: CoroutineStart = CoroutineStart.DEFAULT,
        block: suspend CoroutineScope.(request: Request) -> Response
    ) = ServerServiceCall<Request, Response> {
        CoroutineScope(dispatcher).future(start = start) {
            block(it)
        }
    }

    /**
     * Starts new coroutine and returns its result as an implementation of [HeaderServiceCall].
     *
     * @param start coroutine start option. The default value is [CoroutineStart.DEFAULT].
     * @param block the coroutine code.
     */
    fun <Request, Response> headerServiceCall(
        start: CoroutineStart = CoroutineStart.DEFAULT,
        block: suspend CoroutineScope.(requestHeader: RequestHeader, request: Request) -> akka.japi.Pair<ResponseHeader, Response>
    ) = HeaderServiceCall<Request, Response> { requestHeader, request ->
        CoroutineScope(dispatcher).future(start = start) {
            block(requestHeader, request)
        }
    }
}
