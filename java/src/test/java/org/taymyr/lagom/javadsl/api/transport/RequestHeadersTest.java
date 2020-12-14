package org.taymyr.lagom.javadsl.api.transport;

import com.lightbend.lagom.javadsl.api.transport.RequestHeader;
import org.junit.jupiter.api.Test;
import play.core.cookie.encoding.Cookie;
import play.core.cookie.encoding.DefaultCookie;

import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.taymyr.lagom.javadsl.api.transport.RequestHeaders.getCookie;

class RequestHeadersTest {

    @Test
    void testCookies() {
        RequestHeader requestHeader = RequestHeader.DEFAULT.withHeader("cookie", "name=value; name2=value2; name3=value3");
        Set<Cookie> cookies = RequestHeaders.getCookies(requestHeader);
        assertThat(cookies).containsExactly(
            new DefaultCookie("name", "value"),
            new DefaultCookie("name2", "value2"),
            new DefaultCookie("name3", "value3")
        );
    }

    @Test
    void testCookieByName() {
        RequestHeader requestHeader = RequestHeader.DEFAULT.withHeader("cookie", "name=value; name2=value2; name3=value3");
        assertThat(getCookie(requestHeader, "name2").value()).isEqualTo("value2");
        assertThat(getCookie(requestHeader, "name4")).isNull();
    }

}
