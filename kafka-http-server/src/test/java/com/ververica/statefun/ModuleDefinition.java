package com.ververica.statefun;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.StdSerializer;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.Singular;
import java.io.IOException;
import java.util.List;

/**
 * Version 3.0 metadata.
 */
@AllArgsConstructor
@NoArgsConstructor
@Data
@Builder
@JsonSerialize(using = ModuleDefinition.ModuleDefinitionSerializer.class)
public class ModuleDefinition {
    @Builder.Default
    String version = "3.0";
    Module module;

    public static class ModuleDefinitionSerializer extends StdSerializer<ModuleDefinition> {
        public ModuleDefinitionSerializer() {
            this(null);
        }

        public ModuleDefinitionSerializer(Class<ModuleDefinition> t) {
            super(t);
        }

        @Override
        public void serialize(ModuleDefinition def, JsonGenerator gen, SerializerProvider serializerProvider) throws IOException {
            gen.writeStartObject();

            gen.writeStringField("version", def.getVersion());

            // module: { meta, spec }
            gen.writeObjectFieldStart("module");

            gen.writeObjectField("meta", def.getModule().getMeta());

            // spec: { endpoints: [], ingresses: [], egresses: []}
            gen.writeObjectFieldStart("spec");
            var spec = def.getModule().getSpec();

            // endpoints: [{ endpoint: {}}, ...]
            gen.writeArrayFieldStart("endpoints");
            for (var endpoint : spec.getEndpoints()) {
                gen.writeStartObject();
                gen.writeObjectField("endpoint", endpoint);
                gen.writeEndObject();
            }
            gen.writeEndArray();

            gen.writeArrayFieldStart("ingresses");
            for (var ingress : spec.getIngresses()) {
                gen.writeStartObject();
                gen.writeObjectField("ingress", ingress);
                gen.writeEndObject();
            }
            gen.writeEndArray();

            gen.writeArrayFieldStart("egresses");
            for (var egress : spec.getEgresses()) {
                gen.writeStartObject();
                gen.writeObjectField("egress", egress);
                gen.writeEndObject();
            }
            gen.writeEndArray();

            gen.writeEndObject();
        }
    }


    @AllArgsConstructor
    @NoArgsConstructor
    @Data
    @Builder
    public static class Module {
        ModuleMeta meta;
        ModuleSpec spec;
    }

    @AllArgsConstructor
    @NoArgsConstructor
    @Data
    @Builder
    public static class ModuleMeta {
        String type;
    }

    @AllArgsConstructor
    @NoArgsConstructor
    @Data
    @Builder
    public static class ModuleSpec {
        @Singular
        List<Endpoint> endpoints;

        @Singular
        List<Ingress> ingresses;

        @Singular
        List<Egress> egresses;
    }

    public interface Endpoint {
        EndpointMeta getMeta();
    }

    @AllArgsConstructor
    @NoArgsConstructor
    @Data
    @Builder
    public static class HttpEndpoint implements Endpoint {
        EndpointMeta meta;
        HttpEndpointSpec spec;
    }

    @AllArgsConstructor
    @NoArgsConstructor
    @Data
    @Builder
    public static class EndpointMeta {
        String kind;
    }

    @AllArgsConstructor
    @NoArgsConstructor
    @Data
    @Builder
    public static class HttpEndpointSpec {
        @JsonInclude(JsonInclude.Include.NON_NULL)
        String functions;

        String urlPathTemplate;
        @JsonInclude(JsonInclude.Include.NON_NULL)
        Integer maxNumBatchRequests;
        @JsonInclude(JsonInclude.Include.NON_NULL)
        HttpEndpointTimeout timeout;
    }

    @AllArgsConstructor
    @NoArgsConstructor
    @Data
    @Builder
    public static class HttpEndpointTimeout {
        String call;
        String connect;
        String read;
        String write;
    }

    public interface Ingress {
        IngressMeta getMeta();
    }

    @AllArgsConstructor
    @NoArgsConstructor
    @Data
    @Builder
    public static class IngressMeta {
        String type;
        String id;
    }

    @AllArgsConstructor
    @NoArgsConstructor
    @Data
    @Builder
    public static class KafkaIngress implements Ingress {
        IngressMeta meta;
        KafkaIngressSpec spec;

        @AllArgsConstructor
        @NoArgsConstructor
        @Data
        @Builder
        public static class KafkaIngressSpec {
            String address;
            String consumerGroupId;
            KafkaStartupPosition startupPosition;
            @Singular
            List<KafkaTopic> topics;
            // TODO: add properties
        }

        @AllArgsConstructor
        @NoArgsConstructor
        @Data
        @Builder
        public static class KafkaStartupPosition {
            String type;
            // TODO: add all types
        }

        @AllArgsConstructor
        @NoArgsConstructor
        @Data
        @Builder
        public static class KafkaTopic {
            String topic;
            String valueType;
            @Singular
            List<String> targets;
        }
    }

    public interface Egress {
        EgressMeta getMeta();
    }

    @AllArgsConstructor
    @NoArgsConstructor
    @Data
    @Builder
    public static class EgressMeta {
        String type;
        String id;
    }

    @AllArgsConstructor
    @NoArgsConstructor
    @Data
    @Builder
    public static class KafkaEgress implements Egress {
        EgressMeta meta;

        KafkaEgressSpec spec;

        @AllArgsConstructor
        @NoArgsConstructor
        @Data
        @Builder
        public static class KafkaEgressSpec {
            String address;

            KafkaEgressDeliverySemantic deliverySemantic;

            // TODO: support properties
        }

        @AllArgsConstructor
        @NoArgsConstructor
        @Data
        @Builder
        public static class KafkaEgressDeliverySemantic {
            String type;

            @JsonInclude(JsonInclude.Include.NON_NULL)
            Long transactionTimeoutMillis;
        }
    }
}
