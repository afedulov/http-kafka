package com.ververica.statefun.reqreply;

import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.header.Header;
import org.springframework.kafka.requestreply.CorrelationKey;
import org.springframework.kafka.support.KafkaHeaders;
import java.nio.ByteBuffer;
import java.util.Optional;
import java.util.UUID;

public interface StatefunCorrelationIdStrategy<K, V, R> {
    CorrelationKey apply(ProducerRecord<K, V> record);

    Optional<CorrelationKey> apply(ConsumerRecord<K, R> record);

    class DefaultStatefunCorrelationIdStrategy<K, V, R>  implements StatefunCorrelationIdStrategy<K, V, R> {
        @Override
        public CorrelationKey apply(ProducerRecord<K, V> record) {
            UUID uuid = UUID.randomUUID();
            byte[] bytes = new byte[16]; // NOSONAR magic #
            ByteBuffer bb = ByteBuffer.wrap(bytes);
            bb.putLong(uuid.getMostSignificantBits());
            bb.putLong(uuid.getLeastSignificantBits());
            return new CorrelationKey(bytes);
        }

        @Override
        public Optional<CorrelationKey> apply(ConsumerRecord<K, R> record) {
            Header correlationHeader = record.headers().lastHeader(KafkaHeaders.CORRELATION_ID);
            return Optional
                .ofNullable(correlationHeader)
                .map(Header::value)
                .map(CorrelationKey::new);
        }
    }
}
