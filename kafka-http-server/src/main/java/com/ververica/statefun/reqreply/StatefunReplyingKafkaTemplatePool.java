package com.ververica.statefun.reqreply;

import org.springframework.kafka.requestreply.ReplyingKafkaTemplate;

/**
 * Logically a cache of {@link ReplyingKafkaTemplate} for response topic listeners.
 */
public interface StatefunReplyingKafkaTemplatePool<K, V, R> {
    StatefunReplyingKafkaOperations<K, V, R> getTemplate(String replyTopic);
}
