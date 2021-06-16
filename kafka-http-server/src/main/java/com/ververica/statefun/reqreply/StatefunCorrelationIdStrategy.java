package com.ververica.statefun.reqreply;

import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.springframework.kafka.requestreply.CorrelationKey;
import java.util.Optional;

public interface StatefunCorrelationIdStrategy<K, V, R> {
    CorrelationKey apply(ProducerRecord<K, V> record);

    Optional<CorrelationKey> apply(ConsumerRecord<K, R> record);
}
