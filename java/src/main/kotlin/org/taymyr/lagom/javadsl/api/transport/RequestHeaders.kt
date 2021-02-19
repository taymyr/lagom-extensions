package org.taymyr.lagom.javadsl.api.transport

import com.lightbend.lagom.javadsl.api.transport.RequestHeader
import play.core.cookie.encoding.Cookie
import play.core.cookie.encoding.ServerCookieDecoder
import play.core.cookie.encoding.ServerCookieEncoder
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

    /**
     * Add cookie to request.
     * @param cookies Set Cookies
     */
    @JvmStatic
    fun RequestHeader.withCookies(cookies: Collection<Cookie>): RequestHeader {
        return this.withHeader(COOKIE, ServerCookieEncoder.STRICT.encode(cookies).joinToString("; "))
    }

    /**
     * Add cookie to request.
     * @param cookies Cookies
     */
    @JvmStatic
    fun RequestHeader.withCookies(vararg cookies: Cookie): RequestHeader {
        return withCookies(cookies.asList())
    }
}
