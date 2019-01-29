package org.taymyr.lagom.javadsl.api.transport

import akka.japi.Pair
import com.lightbend.lagom.javadsl.api.transport.MessageProtocol
import com.lightbend.lagom.javadsl.api.transport.ResponseHeader
import com.lightbend.lagom.javadsl.api.transport.ResponseHeader.OK
import org.taymyr.lagom.javadsl.api.transport.MessageProtocols.JSON

/**
 * Utilities and constants for [ResponseHeader].
 * @author Sergey Morgunov
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
}