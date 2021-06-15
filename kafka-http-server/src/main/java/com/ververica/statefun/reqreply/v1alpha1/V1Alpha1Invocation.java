package com.ververica.statefun.reqreply.v1alpha1;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import javax.annotation.Nullable;


/**
 * Modeled after <a href="https://kubernetes.io/docs/reference/access-authn-authz/extensible-admission-controllers/#request">k8s Admission API</a>
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
@JsonPropertyOrder({"kind", "apiVersion", "metadata", "response", "request"})
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public class V1Alpha1Invocation {
    @Builder.Default
    String kind = "Invocation";

    @Builder.Default
    String apiVersion = "reqreply.statefun.ververica.com/v1alpha1";

    @Nullable
    V1Alpha1InvocationMetadata metadata;

    @Nullable
    V1Alpha1InvocationRequest request;

    @Nullable
    V1Alpha1InvocationResponse response;

    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    @Builder
    public static class V1Alpha1InvocationMetadata {
        @Nullable
        String correlationId;
    }

    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    @Builder
    public static class V1Alpha1InvocationResponse {
        String key;

        byte[] value;
    }

    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    @Builder
    public static class V1Alpha1InvocationRequest {
        String key;

        String ingressTopic;

        String egressTopic;

        byte[] value;
    }
}
