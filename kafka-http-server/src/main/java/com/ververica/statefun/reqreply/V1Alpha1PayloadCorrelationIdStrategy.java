package com.ververica.statefun.reqreply;

import com.ververica.statefun.reqreply.v1alpha1.V1Alpha1Invocation;
import com.ververica.statefun.reqreply.v1alpha1.V1Alpha1Invocation.V1Alpha1InvocationMetadata;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.springframework.kafka.requestreply.CorrelationKey;
import java.util.Optional;

public class V1Alpha1PayloadCorrelationIdStrategy extends StatefunReplyingKafkaTemplate.DefaultStatefunCorrelationIdStrategy<String, V1Alpha1Invocation, V1Alpha1Invocation> {
    @Override
    public CorrelationKey apply(ProducerRecord<String, V1Alpha1Invocation> record) {
        var key = fromInvocation(record.value())
            .orElseGet(() -> super.apply(record));

        // Probably not a good idea, but not the ideal solution anyway
        var updatedMeta = Optional.ofNullable(record.value().getMetadata())
            .orElseGet(() -> V1Alpha1InvocationMetadata.builder().build());
        updatedMeta.setCorrelationId(new String(key.getCorrelationId()));

        record.value().setMetadata(updatedMeta);

        return key;
    }

    @Override
    public Optional<CorrelationKey> apply(ConsumerRecord<String, V1Alpha1Invocation> record) {
        return fromInvocation(record.value())
            .or(() -> super.apply(record));
    }

    private static Optional<CorrelationKey> fromInvocation(V1Alpha1Invocation invocation) {
        return Optional.ofNullable(invocation.getMetadata())
            .map(V1Alpha1InvocationMetadata::getCorrelationId)
            .map(String::getBytes)
            .map(CorrelationKey::new);
    }
}
