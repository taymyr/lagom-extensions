package org.taymyr.lagom.javadsl.api.transport

import com.google.common.net.MediaType
import com.lightbend.lagom.javadsl.api.transport.MessageProtocol

import java.util.Optional.of

/**
 * Utilities and constants for [MessageProtocol].
 * @author Sergey Morgunov
 */
object MessageProtocols {

    /**
     * [MessageProtocol] for "application/json"
     */
    @JvmField
    val JSON = fromMediaType(MediaType.create("application", "json"))

    /**
     * [MessageProtocol] for "application/json; charset=utf-8"
     */
    @JvmField
    val JSON_UTF_8 = fromMediaType(MediaType.JSON_UTF_8)

    /**
     * [MessageProtocol] for "application/x-yaml"
     */
    @JvmField
    val YAML = fromMediaType(MediaType.create("application", "x-yaml"))

    /**
     * Create [MessageProtocol] from [MediaType].
     * @param mediaType Media Type
     * @return Message Protocol
     */
    @JvmStatic
    fun fromMediaType(mediaType: MediaType): MessageProtocol {
        return MessageProtocol.fromContentTypeHeader(of(mediaType.toString()))
    }

    /**
     * Find [MessageProtocol] by file name.
     * @param file File name
     * @return Message Protocol. `null` if not found
     */
    @JvmStatic
    fun fromFile(file: String): MessageProtocol? {
        return fromFile(file, null)
    }

    /**
     * Find [MessageProtocol] by file name.
     * @param file File name
     * @param def Default protocol
     * @return Message Protocol. `def` if not found
     */
    @JvmStatic
    fun fromFile(file: String, def: MessageProtocol?): MessageProtocol? {
        return when (file.substringAfterLast('.')) {
            "json" -> JSON
            "yaml", "yml" -> YAML
            else -> def
        }
    }
}