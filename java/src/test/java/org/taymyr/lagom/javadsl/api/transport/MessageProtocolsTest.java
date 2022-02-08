package org.taymyr.lagom.javadsl.api.transport;

import com.lightbend.lagom.javadsl.api.transport.MessageProtocol;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.taymyr.lagom.javadsl.api.transport.MessageProtocols.FORM_URLENCODED;
import static org.taymyr.lagom.javadsl.api.transport.MessageProtocols.JSON;
import static org.taymyr.lagom.javadsl.api.transport.MessageProtocols.JSON_UTF_8;
import static org.taymyr.lagom.javadsl.api.transport.MessageProtocols.YAML;

import static java.util.Optional.of;

class MessageProtocolsTest {

    @Test
    @DisplayName("Constants should be correct")
    void constantsShouldBeCorrect() {
        assertThat(JSON.contentType()).isEqualTo(of("application/json"));
        assertThat(JSON.charset()).isNotPresent();
        assertThat(JSON.version()).isNotPresent();

        assertThat(JSON_UTF_8.contentType()).isEqualTo(of("application/json"));
        assertThat(JSON_UTF_8.charset()).isEqualTo(of("utf-8"));
        assertThat(JSON_UTF_8.version()).isNotPresent();

        assertThat(YAML.contentType()).isEqualTo(of("application/x-yaml"));
        assertThat(YAML.charset()).isNotPresent();
        assertThat(YAML.version()).isNotPresent();

        assertThat(FORM_URLENCODED.contentType()).isEqualTo(of("application/x-www-form-urlencoded"));
        assertThat(FORM_URLENCODED.charset()).isNotPresent();
        assertThat(FORM_URLENCODED.version()).isNotPresent();
    }

    @Test
    @DisplayName("MessageProtocol for file with extention 'json' should be correct")
    void testFromFileJson() {
        MessageProtocol json = MessageProtocols.fromFile("filename.JsOn");
        assertThat(json.contentType()).isEqualTo(of("application/json"));
        assertThat(json.charset()).isNotPresent();
        assertThat(json.version()).isNotPresent();
    }

    @Test
    @DisplayName("MessageProtocol for file with extention 'yaml/yml' should be correct")
    void testFromFileYaml() {
        MessageProtocol yaml = MessageProtocols.fromFile("filename.YaMl");
        assertThat(yaml.contentType()).isEqualTo(of("application/x-yaml"));
        assertThat(yaml.charset()).isNotPresent();
        assertThat(yaml.version()).isNotPresent();

        MessageProtocol yml = MessageProtocols.fromFile("filename.YmL");
        assertThat(yml.contentType()).isEqualTo(of("application/x-yaml"));
        assertThat(yml.charset()).isNotPresent();
        assertThat(yml.version()).isNotPresent();
    }

    @Test
    @DisplayName("MessageProtocol for file with unknown extention should be correct")
    void testFromFileWithDefaultValue() {
        MessageProtocol withoutExt = MessageProtocols.fromFile("filename");
        assertThat(withoutExt).isNull();

        MessageProtocol nill = MessageProtocols.fromFile("filename.unknown");
        assertThat(nill).isNull();

        MessageProtocol json = MessageProtocols.fromFile("filename.unknown", JSON);
        assertThat(json).isEqualTo(JSON);
    }
}