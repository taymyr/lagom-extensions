package org.taymyr.lagom.javadsl.api

import akka.util.ByteString
import assertk.assertThat
import assertk.assertions.isEqualTo
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.taymyr.lagom.javadsl.api.KotlinJsonSerializer.Companion.serializer
import org.taymyr.lagom.javadsl.api.transport.MessageProtocols.JSON
import kotlin.random.Random

class KotlinJsonSerializerTest {

    @Serializable
    data class TestMessage(
        val age: Int,
        val name: String,
        val id: Long,
        val wired: Boolean,
        val simpleList: List<Int>,
        val nullableType: Double?
    )

    @Serializable
    data class GenericTest<T>(val value: T, val temp: Int = Random.nextInt())

    data class WithoutSerializable(val temp: Int = Random.nextInt())

    private fun testMessage(nullableType: Double? = Random.nextDouble()) = TestMessage(
        age = Random.nextInt(),
        name = "Alex Mihailov",
        id = Random.nextLong(),
        wired = Random.nextBoolean(),
        simpleList = List(3) { Random.nextInt() },
        nullableType = nullableType
    )

    @Test
    fun testSerialize() {
        val serializer = serializer<TestMessage>(Json)

        var message = testMessage()
        var json = serializer.serializerForRequest().serialize(message).decodeString(Charsets.UTF_8)
        assertThat(json).isEqualTo(Json.encodeToString(message))

        message = testMessage(null)
        json = serializer.serializerForResponse(emptyList()).serialize(message).decodeString(Charsets.UTF_8)
        assertThat(json).isEqualTo(Json.encodeToString(message))
    }

    @Test
    fun testGenericSerialize() {
        val serializer = serializer<GenericTest<TestMessage>>(Json)

        val message = GenericTest(testMessage())
        var json = serializer.serializerForRequest().serialize(message).decodeString(Charsets.UTF_8)
        assertThat(json).isEqualTo(Json.encodeToString(message))

        json = serializer.serializerForResponse(emptyList()).serialize(message).decodeString(Charsets.UTF_8)
        assertThat(json).isEqualTo(Json.encodeToString(message))
    }

    @Test
    fun testDeserialize() {
        val serializer = serializer<TestMessage>(Json)

        var expected = testMessage()
        var json = Json.encodeToString(expected)
        var actual = serializer.deserializer(JSON).deserialize(ByteString.fromString(json))
        assertThat(actual).isEqualTo(expected)

        expected = testMessage(null)
        json = Json.encodeToString(expected)
        actual = serializer.deserializer(JSON).deserialize(ByteString.fromString(json))
        assertThat(actual).isEqualTo(expected)
    }

    @Test
    fun testGenericDeserialize() {
        val serializer = serializer<GenericTest<TestMessage>>(Json)

        val expected = GenericTest(testMessage())
        val json = Json.encodeToString(expected)
        val actual = serializer.deserializer(JSON).deserialize(ByteString.fromString(json))
        assertThat(actual).isEqualTo(expected)
    }

    @Test
    fun testSerializeWithoutSerializable() {
        assertThrows<IllegalArgumentException > {
            val serializer = serializer<WithoutSerializable>(Json)
            serializer.serializerForRequest().serialize(WithoutSerializable())
        }
        assertThrows<IllegalArgumentException > {
            val serializer = serializer<GenericTest<WithoutSerializable>>(Json)
            serializer.serializerForResponse(emptyList()).serialize(GenericTest(WithoutSerializable()))
        }
        assertThrows<IllegalArgumentException > {
            val serializer = serializer<WithoutSerializable>(Json)
            val json = """{ "temp": 10 }"""
            serializer.deserializer(JSON).deserialize(ByteString.fromString(json))
        }
    }
}
