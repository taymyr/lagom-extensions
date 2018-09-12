package org.taymyr.lagom.javadsl.api.transport

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.taymyr.lagom.javadsl.api.transport.MessageProtocols.JSON
import org.taymyr.lagom.javadsl.api.transport.MessageProtocols.JSON_UTF_8
import org.taymyr.lagom.javadsl.api.transport.MessageProtocols.YAML
import java.util.Optional.of

class MessageProtocolsTest {

    @Test
    fun testConstants() {
        assertThat(JSON.contentType()).isEqualTo(of("application/json"))
        assertThat(JSON.charset()).isNotPresent
        assertThat(JSON.version()).isNotPresent

        assertThat(JSON_UTF_8.contentType()).isEqualTo(of("application/json"))
        assertThat(JSON_UTF_8.charset()).isEqualTo(of("utf-8"))
        assertThat(JSON_UTF_8.version()).isNotPresent

        assertThat(YAML.contentType()).isEqualTo(of("application/x-yaml"))
        assertThat(YAML.charset()).isNotPresent
        assertThat(YAML.version()).isNotPresent
    }

    @Test
    fun testFromFileJson() {
        val json = MessageProtocols.fromFile("filename.JsOn")
        assertThat(json?.contentType()).isEqualTo(of("application/json"))
        assertThat(json?.charset()).isNotPresent
        assertThat(json?.version()).isNotPresent
    }

    @Test
    fun testFromFileYaml() {
        val yaml = MessageProtocols.fromFile("filename.YaMl")
        assertThat(yaml?.contentType()).isEqualTo(of("application/x-yaml"))
        assertThat(yaml?.charset()).isNotPresent
        assertThat(yaml?.version()).isNotPresent

        val yml = MessageProtocols.fromFile("filename.YmL")
        assertThat(yml?.contentType()).isEqualTo(of("application/x-yaml"))
        assertThat(yml?.charset()).isNotPresent
        assertThat(yml?.version()).isNotPresent
    }

    @Test
    fun testFromFileWithDefaultValue() {
        val withoutExt = MessageProtocols.fromFile("filename")
        assertThat(withoutExt).isNull()

        val nill = MessageProtocols.fromFile("filename.unknown")
        assertThat(nill).isNull()

        val json = MessageProtocols.fromFile("filename.unknown", JSON)
        assertThat(json).isEqualTo(JSON)
    }
}