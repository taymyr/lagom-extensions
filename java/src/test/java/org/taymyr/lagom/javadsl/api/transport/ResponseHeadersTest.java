package org.taymyr.lagom.javadsl.api.transport;

import akka.japi.Pair;
import com.lightbend.lagom.javadsl.api.transport.ResponseHeader;
import org.junit.jupiter.api.Test;
import play.core.cookie.encoding.DefaultCookie;

import static com.lightbend.lagom.javadsl.api.transport.ResponseHeader.OK;
import static org.assertj.core.api.Assertions.assertThat;
import static org.taymyr.lagom.javadsl.api.transport.MessageProtocols.JSON;
import static org.taymyr.lagom.javadsl.api.transport.MessageProtocols.YAML;
import static org.taymyr.lagom.javadsl.api.transport.ResponseHeaders.OK_JSON;
import static org.taymyr.lagom.javadsl.api.transport.ResponseHeaders.ok;
import static org.taymyr.lagom.javadsl.api.transport.ResponseHeaders.okJson;
import static org.taymyr.lagom.javadsl.api.transport.ResponseHeaders.withCookie;

class ResponseHeadersTest {

    @Test
    void testConstants() {
        assertThat(OK_JSON.status()).isEqualTo(200);
        assertThat(OK_JSON.protocol()).isEqualTo(JSON);
    }

    @Test
    void testOkWithData() {
        String data = "data";
        Pair<ResponseHeader, String> ok = ok(data);
        assertThat(ok.first()).isEqualTo(OK);
        assertThat(ok.second()).isEqualTo(data);
    }

    @Test
    void testOkWithProtocol() {
        String data = "data";
        Pair<ResponseHeader, String> ok = ok(YAML, data);
        assertThat(ok.first().status()).isEqualTo(200);
        assertThat(ok.first().protocol()).isEqualTo(YAML);
        assertThat(ok.second()).isEqualTo(data);
    }

    @Test
    void testOkJson() {
        String data = "data";
        Pair<ResponseHeader, String> ok = okJson(data);
        assertThat(ok.first()).isEqualTo(OK_JSON);
        assertThat(ok.second()).isEqualTo(data);
    }

    @Test
    void testCookie() {
        ResponseHeader header = withCookie(OK, new DefaultCookie("cookie", "value"));
        assertThat(header.getHeader("set-cookie").get()).isEqualTo("cookie=value");
    }
}
