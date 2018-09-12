package org.taymyr.lagom.javadsl.api.transport

import com.lightbend.lagom.javadsl.api.transport.ResponseHeader.OK
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.taymyr.lagom.javadsl.api.transport.MessageProtocols.JSON
import org.taymyr.lagom.javadsl.api.transport.MessageProtocols.YAML
import org.taymyr.lagom.javadsl.api.transport.ResponseHeaders.OK_JSON
import org.taymyr.lagom.javadsl.api.transport.ResponseHeaders.ok
import org.taymyr.lagom.javadsl.api.transport.ResponseHeaders.okJson

class ResponseHeadersTest {

    @Test
    fun testConstants() {
        assertThat(OK_JSON.status()).isEqualTo(200)
        assertThat(OK_JSON.protocol()).isEqualTo(JSON)
    }

    @Test
    fun testOkWithData() {
        val data = "data"
        val ok = ok(data)
        assertThat(ok.first()).isEqualTo(OK)
        assertThat(ok.second()).isEqualTo(data)
    }

    @Test
    fun testOkWithProtocol() {
        val data = "data"
        val ok = ok(YAML, data)
        assertThat(ok.first().status()).isEqualTo(200)
        assertThat(ok.first().protocol()).isEqualTo(YAML)
        assertThat(ok.second()).isEqualTo(data)
    }

    @Test
    fun testOkJson() {
        val data = "data"
        val ok = okJson(data)
        assertThat(ok.first()).isEqualTo(OK_JSON)
        assertThat(ok.second()).isEqualTo(data)
    }
}