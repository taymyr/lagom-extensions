package org.taymyr.lagom.javadsl.api

import com.lightbend.lagom.javadsl.api.transport.RequestHeader
import com.lightbend.lagom.javadsl.api.transport.ResponseHeader
import com.lightbend.lagom.javadsl.server.HeaderServiceCall
import com.lightbend.lagom.javadsl.server.ServerServiceCall
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CoroutineStart
import org.pac4j.core.authorization.authorizer.IsAuthenticatedAuthorizer
import org.pac4j.core.profile.AnonymousProfile
import org.pac4j.core.profile.CommonProfile
import org.pac4j.lagom.javadsl.SecuredService

interface CoroutineSecuredService : SecuredService, CoroutineService {

    /**
     * Starts new coroutine with authorized and returns its result as an implementation of [ServerServiceCall].
     *
     * @param start coroutine start option. The default value is [CoroutineStart.DEFAULT].
     * @param block he coroutine code.
     */
    fun <Request, Response> authorizedServiceCall(
        start: CoroutineStart = CoroutineStart.DEFAULT,
        block: suspend CoroutineScope.(request: Request, profile: CommonProfile) -> Response
    ): ServerServiceCall<Request, Response> = this.authorize(IsAuthenticatedAuthorizer.isAuthenticated()) { profile ->
        serverServiceCall<Request, Response>(start) { request -> block(request, profile) }
    }

    /**
     * Starts new coroutine with authorized and returns its result as an implementation of [ServerServiceCall].
     *
     * @param start coroutine start option. The default value is [CoroutineStart.DEFAULT].
     * @param block he coroutine code.
     */
    fun <Request, Response> authorizedHeaderServiceCall(
        start: CoroutineStart = CoroutineStart.DEFAULT,
        block: suspend CoroutineScope.(requestHeader: RequestHeader, request: Request, profile: CommonProfile) -> akka.japi.Pair<ResponseHeader, Response>
    ): ServerServiceCall<Request, Response> = this.authorize(IsAuthenticatedAuthorizer.isAuthenticated()) { profile ->
        HeaderServiceCall.of(
            headerServiceCall<Request, Response>(start) { requestHeader, request -> block(requestHeader, request, profile) }
        )
    }

    /**
     * Starts new coroutine with authentication and returns its result as an implementation of [ServerServiceCall].
     * If authentication is failed, the profile will be an instance of [AnonymousProfile].
     *
     * @param start coroutine start option. The default value is [CoroutineStart.DEFAULT].
     * @param block he coroutine code.
     */
    fun <Request, Response> authenticatedServiceCall(
        start: CoroutineStart = CoroutineStart.DEFAULT,
        block: suspend CoroutineScope.(request: Request, profile: CommonProfile) -> Response
    ): ServerServiceCall<Request, Response> = this.authenticate { profile ->
        serverServiceCall<Request, Response>(start) { request -> block(request, profile) }
    }

    /**
     * Starts new coroutine with authentication and returns its result as an implementation of [ServerServiceCall].
     * If authentication is failed, the profile will be an instance of [AnonymousProfile].
     *
     * @param start coroutine start option. The default value is [CoroutineStart.DEFAULT].
     * @param block he coroutine code.
     */
    fun <Request, Response> authenticatedHeaderServiceCall(
        start: CoroutineStart = CoroutineStart.DEFAULT,
        block: suspend CoroutineScope.(requestHeader: RequestHeader, request: Request, profile: CommonProfile) -> akka.japi.Pair<ResponseHeader, Response>
    ): ServerServiceCall<Request, Response> = this.authenticate { profile ->
        HeaderServiceCall.of(
            headerServiceCall<Request, Response>(start) { requestHeader, request -> block(requestHeader, request, profile) }
        )
    }
}
