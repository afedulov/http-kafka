package com.ververica.statefun.reqreply.v1alpha1;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

public class V1Alpha1InvocationTest {
    @Test
    @DisplayName("should serialize to JSON")
    void shouldSerialize() throws Exception {
        var mapper = new ObjectMapper();
        var invocation = V1Alpha1Invocation.builder()
            .request(V1Alpha1Invocation.V1Alpha1InvocationRequest.builder()
                .key("test-message")
                .ingressTopic("invoke")
                .egressTopic("invoke-results")
                .value("some value".getBytes())
                .build())
            .build();

        var serialized = mapper.writeValueAsString(invocation);
        assertThat(serialized).isNotEmpty();
    }

    @Test
    @DisplayName("should deserialize from JSON")
    void shouldDeserialize() throws Exception {
        var mapper = new ObjectMapper();
        var serialized = "{" +
            "\"kind\": \"Invocation\"," +
            "\"apiVersion\": \"reqreply.statefun.ververica.com/v1alpha1\"," +
            "\"request\": {}," +
            "\"response\": {}" +
            "}";

        var deserialized = mapper.readValue(serialized, V1Alpha1Invocation.class);

        assertThat(deserialized.getKind()).isEqualTo("Invocation");
    }
}
