package com.ververica.statefun.reqreply;

import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.core.ProducerFactory;
import org.springframework.kafka.requestreply.ReplyingKafkaOperations;
import org.springframework.kafka.requestreply.ReplyingKafkaTemplate;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Logically a cache of {@link ReplyingKafkaTemplate} for response topic listeners.
 */
public class DefaultReplyingKafkaTemplatePool<K, V, R> implements ReplyingKafkaTemplatePool<K, V, R> {
    private final String groupId;

    private final Map<String, ReplyingKafkaTemplate<K, V, R>> templateMap = new ConcurrentHashMap<>();

    private final ProducerFactory<K, V> producerFactory;

    private final ConcurrentKafkaListenerContainerFactory<K, R> listenerContainerFactory;

    public DefaultReplyingKafkaTemplatePool(String groupId, ProducerFactory<K, V> pf, ConcurrentKafkaListenerContainerFactory<K, R> listenerContainerFactory) {
        this.groupId = groupId;
        this.producerFactory = pf;
        this.listenerContainerFactory = listenerContainerFactory;
    }

    public ReplyingKafkaOperations<K, V, R> getTemplate(String replyTopic) {
        // TODO: add cleanup logic when there are no longer listeners to fix memory leak
        synchronized (this) {
            if (!templateMap.containsKey(replyTopic)) {
                var template = buildTemplate(replyTopic);
                template.start();
                templateMap.put(replyTopic, template);
            }
        }

        return templateMap.get(replyTopic);
    }

    private ReplyingKafkaTemplate<K, V, R> buildTemplate(String replyTopic) {
        var replyContainer = listenerContainerFactory.createContainer(replyTopic);
        replyContainer.getContainerProperties().setMissingTopicsFatal(false);
        replyContainer.getContainerProperties().setGroupId(groupId);

        return new ReplyingKafkaTemplate<>(producerFactory, replyContainer);
    }
}
