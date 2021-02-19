package org.taymyr.lagom.javadsl.api.transport

import akka.japi.Pair
import com.lightbend.lagom.javadsl.api.transport.MessageProtocol
import com.lightbend.lagom.javadsl.api.transport.ResponseHeader
import com.lightbend.lagom.javadsl.api.transport.ResponseHeader.OK
import org.taymyr.lagom.javadsl.api.transport.MessageProtocols.JSON
import play.core.cookie.encoding.Cookie
import play.core.cookie.encoding.ServerCookieDecoder
import play.core.cookie.encoding.ServerCookieEncoder
import play.mvc.Http.HeaderNames.SET_COOKIE

/**
 * Utilities and constants for [ResponseHeader].
 */
object ResponseHeaders {

    @JvmField
    val OK_JSON = OK.withProtocol(JSON)!!

    /**
     *
     * Create successful service response.
     *
     * *Content-Type: text/plain* for [String] type.
     *
     * @param T Type of data
     * @param data Response data
     * @return Response with 'OK' header
     */
    @JvmStatic
    fun <T> ok(data: T): Pair<ResponseHeader, T> {
        return Pair.create(OK, data)
    }

    /**
     *
     * Create successful service response with MIME-TYPE.
     * @param T Type of data
     * @param protocol MIME-TYPE
     * @param data Response data
     * @return Response with 'OK' header
     */
    @JvmStatic
    fun <T> ok(protocol: MessageProtocol, data: T): Pair<ResponseHeader, T> {
        return Pair.create(OK.withProtocol(protocol), data)
    }

    /**
     * Create successful service response with '*Content-Type: application/json*'.
     * @param T Type of data
     * @param data Response data
     * @return Response with 'OK' header
     */
    @JvmStatic
    fun <T> okJson(data: T): Pair<ResponseHeader, T> {
        return Pair.create(OK_JSON, data)
    }

    /**
     * Get all cookies from response.
     * @return Empty set if cookies not found.
     */
    @JvmStatic
    fun ResponseHeader.getCookies(): Set<Cookie> =
        this.headers()[SET_COOKIE]
            .orEmpty()
            .flatMap(ServerCookieDecoder.STRICT::decode)
            .toSet()

    /**
     * Add cookie to response.
     * @param cookie Cookie
     */
    @JvmStatic
    fun ResponseHeader.withCookie(cookie: Cookie): ResponseHeader {
        return this.withHeader(SET_COOKIE, ServerCookieEncoder.STRICT.encode(cookie))
    }
}
