package org.taymyr.lagom.javadsl.api

import akka.util.ByteString
import akka.util.ByteStringBuilder
import com.lightbend.lagom.javadsl.api.Descriptor
import com.lightbend.lagom.javadsl.api.deser.MessageSerializer.NegotiatedDeserializer
import com.lightbend.lagom.javadsl.api.deser.MessageSerializer.NegotiatedSerializer
import com.lightbend.lagom.javadsl.api.deser.StrictMessageSerializer
import com.lightbend.lagom.javadsl.api.transport.MessageProtocol
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.KSerializer
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.decodeFromStream
import kotlinx.serialization.json.encodeToStream
import kotlinx.serialization.serializer
import java.io.ByteArrayInputStream
import java.util.Optional.empty
import java.util.Optional.of

/**
 * Json-serializer that uses kotlinx-serialization.
 */
class KotlinJsonSerializer<Message> @PublishedApi internal constructor(json: Json, kSerializer: KSerializer<Message>) : StrictMessageSerializer<Message> {
    private val serializer = Serializer(json, kSerializer)
    private val deserializer = Deserializer(json, kSerializer)

    override fun serializerForRequest(): NegotiatedSerializer<Message, ByteString> = serializer

    override fun deserializer(protocol: MessageProtocol): NegotiatedDeserializer<Message, ByteString> = deserializer

    override fun serializerForResponse(acceptedMessageProtocols: List<MessageProtocol>): NegotiatedSerializer<Message, ByteString> = serializer

    companion object {

        /**
         * Creates a [KotlinJsonSerializer] for the [Message] type.
         *
         * @param json [Json]
         * @return [KotlinJsonSerializer]
         */
        inline fun <reified Message> serializer(json: Json): KotlinJsonSerializer<Message> =
            KotlinJsonSerializer(json, json.serializersModule.serializer())
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
}

/**
 * Setting [KotlinJsonSerializer] to serialize [Request].
 *
 * @receiver [Descriptor.Call]
 * @param json [Json]
 * @return [Descriptor.Call]
 */
inline fun <reified Request, Response> Descriptor.Call<Request, Response>.withRequestKotlinJsonSerializer(json: Json): Descriptor.Call<Request, Response> =
    withRequestSerializer(KotlinJsonSerializer.serializer(json))

/**
 * Setting [KotlinJsonSerializer] to serialize [Response].
 *
 * @receiver [Descriptor.Call]
 * @param json [Json]
 * @return [Descriptor.Call]
 */
inline fun <reified Response, Request> Descriptor.Call<Request, Response>.withResponseKotlinJsonSerializer(json: Json): Descriptor.Call<Request, Response> =
    withResponseSerializer(KotlinJsonSerializer.serializer(json))

/**
 * Setting [KotlinJsonSerializer] to serialize [Message].
 * When using a parameterized class, one serializer will be used for all variants.
 * Therefore, this method will throw an [UnsupportedOperationException]
 *
 * @receiver [Descriptor]
 * @param json [Json]
 * @return [Descriptor]
 */
inline fun <reified Message> Descriptor.withKotlinJsonSerializer(json: Json): Descriptor = let {
    if (Message::class.typeParameters.isNotEmpty()) {
        throw UnsupportedOperationException(
            "Parameterized types are not supported. Use withRequestKotlinJsonSerializer," +
                " withResponseKotlinJsonSerializer instead of withKotlinJsonSerializer."
        )
    }
    withMessageSerializer(Message::class.java, KotlinJsonSerializer.serializer(json))
}
