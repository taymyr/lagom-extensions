package org.taymyr.lagom.javadsl.api

import com.lightbend.lagom.javadsl.api.ServiceCall
import com.lightbend.lagom.javadsl.api.transport.RequestHeader
import com.lightbend.lagom.javadsl.api.transport.ResponseHeader
import com.lightbend.lagom.javadsl.server.HeaderServiceCall
import com.lightbend.lagom.javadsl.server.ServerServiceCall
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.future.await
import kotlinx.coroutines.future.future
import org.pac4j.core.authorization.authorizer.IsAuthenticatedAuthorizer.isAuthenticated
import org.pac4j.core.profile.CommonProfile
import org.pac4j.lagom.javadsl.SecuredService
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

/**
 * Starts new coroutine and returns its result as an implementation of [ServerServiceCall].
 *
 * @param context additional to [CoroutineScope.coroutineContext] context of the coroutine.
 * @param start coroutine start option. The default value is [CoroutineStart.DEFAULT].
 * @param block the coroutine code.
 */
fun <Request, Response> serverServiceCall(
    context: CoroutineContext = EmptyCoroutineContext,
    start: CoroutineStart = CoroutineStart.DEFAULT,
    block: suspend CoroutineScope.(request: Request) -> Response
) = ServerServiceCall<Request, Response> {
    CoroutineScope(Dispatchers.Unconfined).future(context, start) {
        block(it)
    }
}

/**
 * Starts new coroutine and returns its result as an implementation of [HeaderServiceCall].
 *
 * @param context additional to [CoroutineScope.coroutineContext] context of the coroutine.
 * @param start coroutine start option. The default value is [CoroutineStart.DEFAULT].
 * @param block the coroutine code.
 */
fun <Request, Response> headerServiceCall(
    context: CoroutineContext = EmptyCoroutineContext,
    start: CoroutineStart = CoroutineStart.DEFAULT,
    block: suspend CoroutineScope.(requestHeader: RequestHeader, request: Request) -> akka.japi.Pair<ResponseHeader, Response>
) = HeaderServiceCall<Request, Response> { requestHeader, request ->
    CoroutineScope(Dispatchers.Unconfined).future(context, start) {
        block(requestHeader, request)
    }
}

/**
 * Starts new coroutine with authorized and returns its result as an implementation of [ServerServiceCall].
 *
 * @receiver SecuredService
 * @param context additional to [CoroutineScope.coroutineContext] context of the coroutine.
 * @param start coroutine start option. The default value is [CoroutineStart.DEFAULT].
 * @param block he coroutine code.
 */
fun <Request, Response> SecuredService.authorizedServiceCall(
    context: CoroutineContext = EmptyCoroutineContext,
    start: CoroutineStart = CoroutineStart.DEFAULT,
    block: suspend CoroutineScope.(request: Request, profile: CommonProfile) -> Response
): ServerServiceCall<Request, Response> = this.authorize(
    isAuthenticated(),
    { profile ->
        serverServiceCall<Request, Response>(context, start) { request -> block(request, profile) }
    }
)

/**
 * Starts new coroutine with authorized and returns its result as an implementation of [ServerServiceCall].
 *
 * @receiver SecuredService
 * @param context additional to [CoroutineScope.coroutineContext] context of the coroutine.
 * @param start coroutine start option. The default value is [CoroutineStart.DEFAULT].
 * @param block he coroutine code.
 */
fun <Request, Response> SecuredService.authorizedHeaderServiceCall(
    context: CoroutineContext = EmptyCoroutineContext,
    start: CoroutineStart = CoroutineStart.DEFAULT,
    block: suspend CoroutineScope.(requestHeader: RequestHeader, request: Request, profile: CommonProfile) -> akka.japi.Pair<ResponseHeader, Response>
): ServerServiceCall<Request, Response> = this.authorize(
    isAuthenticated(),
    { profile ->
        HeaderServiceCall.of(
            headerServiceCall<Request, Response>(context, start) { requestHeader, request -> block(requestHeader, request, profile) }
        )
    }
)

/**
 * Helper extension method for [ServiceCall.invoke().await()] call chain.
 */
suspend fun <Request, Response> ServiceCall<Request, Response>.await(): Response = this.invoke().await()
