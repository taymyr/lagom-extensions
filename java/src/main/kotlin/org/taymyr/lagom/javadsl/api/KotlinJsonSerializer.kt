package org.taymyr.lagom.javadsl.api

import akka.util.ByteString
import akka.util.ByteStringBuilder
import com.lightbend.lagom.javadsl.api.deser.MessageSerializer.NegotiatedDeserializer
import com.lightbend.lagom.javadsl.api.deser.MessageSerializer.NegotiatedSerializer
import com.lightbend.lagom.javadsl.api.deser.SerializerFactory
import com.lightbend.lagom.javadsl.api.deser.StrictMessageSerializer
import com.lightbend.lagom.javadsl.api.transport.MessageProtocol
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.KSerializer
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.decodeFromStream
import kotlinx.serialization.json.encodeToStream
import kotlinx.serialization.serializer
import java.io.ByteArrayInputStream
import java.lang.reflect.Type
import java.util.Optional.empty
import java.util.Optional.of

@Suppress("unused")
class KotlinJsonSerializerFactory(private val json: Json) : SerializerFactory {

    /**
     * Get a message json-serializer for the given type.
     *
     * @param type serializable type, must be annotated [kotlinx.serialization.Serializable]
     * @return message json-serializer
     */
    override fun <Message> messageSerializerFor(type: Type): StrictMessageSerializer<Message> =
        KotlinJsonSerializer(json, type)
}

/**
 * Json-serializer that uses kotlinx-serialization.
 */
class KotlinJsonSerializer<Message>(json: Json, type: Type) : StrictMessageSerializer<Message> {
    private val kSerializer: KSerializer<Message> = getKSerializer(type)
    private val serializer = Serializer(json, kSerializer)
    private val deserializer = Deserializer(json, kSerializer)

    override fun serializerForRequest(): NegotiatedSerializer<Message, ByteString> = serializer

    override fun deserializer(protocol: MessageProtocol): NegotiatedDeserializer<Message, ByteString> = deserializer

    override fun serializerForResponse(acceptedMessageProtocols: List<MessageProtocol>): NegotiatedSerializer<Message, ByteString> = serializer
}

private class Serializer<MessageEntity>(
    private val json: Json,
    private val kSerializer: KSerializer<MessageEntity>
) : NegotiatedSerializer<MessageEntity, ByteString> {

    override fun protocol(): MessageProtocol = MessageProtocol(of("application/json"), of("utf-8"), empty())

    @OptIn(ExperimentalSerializationApi::class)
    override fun serialize(messageEntity: MessageEntity): ByteString {
        val builder = ByteStringBuilder()
        json.encodeToStream(kSerializer, messageEntity, builder.asOutputStream())
        return builder.result()
    }
}

private class Deserializer<MessageEntity>(
    private val json: Json,
    private val kSerializer: KSerializer<MessageEntity>
) : NegotiatedDeserializer<MessageEntity, ByteString> {

    @OptIn(ExperimentalSerializationApi::class)
    override fun deserialize(wire: ByteString): MessageEntity {
        val inputStream = ByteArrayInputStream(wire.toArray())
        return json.decodeFromStream(kSerializer, inputStream)
    }
}

@Suppress("UNCHECKED_CAST")
@OptIn(ExperimentalSerializationApi::class)
private fun <Message> getKSerializer(type: Type): KSerializer<Message> = serializer(type) as KSerializer<Message>
