package com.ververica.statefun.reqreply;

import org.springframework.kafka.requestreply.ReplyingKafkaOperations;
import org.springframework.kafka.requestreply.ReplyingKafkaTemplate;

/**
 * Logically a cache of {@link ReplyingKafkaTemplate} for response topic listeners.
 */
public interface ReplyingKafkaTemplatePool<K, V, R> {
    ReplyingKafkaOperations<K, V, R> getTemplate(String replyTopic);
}
