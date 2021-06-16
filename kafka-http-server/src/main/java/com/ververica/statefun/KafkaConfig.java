package com.ververica.statefun;

import com.ververica.statefun.reqreply.DefaultReplyingKafkaTemplatePool;
import com.ververica.statefun.reqreply.StatefunReplyingKafkaTemplatePool;
import com.ververica.statefun.reqreply.V1Alpha1PayloadCorrelationIdStrategy;
import com.ververica.statefun.reqreply.v1alpha1.V1Alpha1Invocation;
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
	public StatefunReplyingKafkaTemplatePool<String, byte[], byte[]> replyingKafkaTemplatePool(ProducerFactory<String, byte[]> pf,
																							   ConcurrentKafkaListenerContainerFactory<String, byte[]> factory) {
		return new DefaultReplyingKafkaTemplatePool<>(groupId, pf, factory);
	}

	@Bean
	public StatefunReplyingKafkaTemplatePool<String, V1Alpha1Invocation, V1Alpha1Invocation> v1Alpha1InvocationReplyingKafkaTemplatePool(ProducerFactory<String, V1Alpha1Invocation> pf, ConcurrentKafkaListenerContainerFactory<String, V1Alpha1Invocation> factory) {
		var correlationIdStrategy = new V1Alpha1PayloadCorrelationIdStrategy();
		return new DefaultReplyingKafkaTemplatePool<>(groupId, pf, factory)
			.withModifier(tmpl -> {
				tmpl.setCorrelationIdStrategy(correlationIdStrategy);
				return tmpl;
			});
	}
}
