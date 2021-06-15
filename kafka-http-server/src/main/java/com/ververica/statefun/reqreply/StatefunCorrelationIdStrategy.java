package com.ververica.statefun.reqreply;

import com.ververica.statefun.StatefunAnnotations;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.header.Header;
import org.springframework.kafka.requestreply.CorrelationKey;
import java.nio.charset.StandardCharsets;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.StreamSupport;

public class StatefunCorrelationIdStrategy<K, V> implements Function<ProducerRecord<K, V>, CorrelationKey> {
    @Override
    public CorrelationKey apply(ProducerRecord<K, V> record) {
        Iterable<Header> headers = record.headers().headers(StatefunAnnotations.CORRELATION_ID);
        return StreamSupport.stream(headers.spliterator(), false)
            .findFirst()
            .map(Header::value)
            .or(() -> Optional.of(createCorrelationId().getBytes(StandardCharsets.UTF_8)))
            .map(CorrelationKey::new)
            .get();
    }

    public static String createCorrelationId() {
        return UUID.randomUUID().toString();
    }
}
