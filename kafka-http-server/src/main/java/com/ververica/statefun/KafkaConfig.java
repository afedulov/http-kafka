package com.ververica.statefun;

import com.ververica.statefun.reqreply.DefaultReplyingKafkaTemplatePool;
import com.ververica.statefun.reqreply.ReplyingKafkaTemplatePool;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.core.ProducerFactory;

@Configuration
public class KafkaConfig {
	@Value("${kafka.group.id}")
	private String groupId;

	@Bean
	public ReplyingKafkaTemplatePool<String, byte[], byte[]> replyingKafkaTemplatePool(ProducerFactory<String, byte[]> pf,
																					   ConcurrentKafkaListenerContainerFactory<String, byte[]> factory) {
		return new DefaultReplyingKafkaTemplatePool<>(groupId, pf, factory);
	}
}
