package org.taymyr.lagom.javadsl.api.transport

import com.lightbend.lagom.javadsl.api.transport.RequestHeader
import play.core.cookie.encoding.Cookie
import play.core.cookie.encoding.ServerCookieDecoder
import play.mvc.Http.HeaderNames.COOKIE

/**
 * Utilities and constants for [RequestHeader].
 */
object RequestHeaders {

    /**
     * Find cookie by name from request.
     * @param name Cookie name
     * @return `null` if cookie not found.
     */
    @JvmStatic
    fun RequestHeader.getCookie(name: String): Cookie? =
        this.getHeader(COOKIE)
            .map(ServerCookieDecoder.STRICT::decode)
            .orElse(emptySet())
            .find { it.name() == name }

    /**
     * Get all cookies from request.
     * @return Empty set if cookies not found.
     */
    @JvmStatic
    fun RequestHeader.getCookies(): Set<Cookie> =
        this.getHeader(COOKIE)
            .map(ServerCookieDecoder.STRICT::decode)
            .orElse(emptySet())
}
