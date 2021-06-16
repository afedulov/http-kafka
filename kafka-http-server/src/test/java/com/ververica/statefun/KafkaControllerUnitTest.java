package com.ververica.statefun;

import static org.assertj.core.api.Assertions.assertThat;

import com.ververica.statefun.reqreply.StatefunReplyingKafkaOperations;
import com.ververica.statefun.reqreply.StatefunReplyingKafkaTemplatePool;
import com.ververica.statefun.reqreply.StatefunRequestReplyFuture;
import com.ververica.statefun.reqreply.v1alpha1.V1Alpha1Invocation;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.kafka.support.SendResult;
import org.springframework.util.concurrent.CompletableToListenableFutureAdapter;
import java.time.Duration;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

class KafkaControllerUnitTest {
	@Test
	void invocationInPayloadSucceeds() throws ExecutionException, InterruptedException {
		var payloadPool = new MockTemplatePool<V1Alpha1Invocation, V1Alpha1Invocation>(Map.of(
			"invoke-results",
			new MirroringReplyingKafkaOperations<>()
		));
		var controller = new KafkaController(new MockTemplatePool<>(Map.of()), payloadPool);

		var invocationReq = V1Alpha1Invocation.builder()
			.metadata(V1Alpha1Invocation.V1Alpha1InvocationMetadata.builder()
				.correlationId("abc")
				.build())
			.request(V1Alpha1Invocation.V1Alpha1InvocationRequest.builder()
				.key("test-message")
				.ingressTopic("invoke")
				.egressTopic("invoke-results")
				.value("some value".getBytes())
				.build())
			.build();

		var response = controller.invokeViaPayload(invocationReq, null);

		assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);

		assertThat(response.getBody()).isInstanceOf(V1Alpha1Invocation.class);

		var resInvocation = (V1Alpha1Invocation) response.getBody();
		assertThat(resInvocation.getMetadata().getCorrelationId()).isEqualTo("abc");
		assertThat(resInvocation.getResponse().getValue()).isEqualTo("some value".getBytes());
	}

	@Test
	void invocationSucceeds() throws ExecutionException, InterruptedException {
		var pool = new MockTemplatePool<byte[], byte[]>(Map.of(
			"invoke-results",
			new MirroringReplyingKafkaOperations<>()
		));
		var controller = new KafkaController(pool, new MockTemplatePool<>(Map.of()));

		var invocationReq = V1Alpha1Invocation.builder()
			.metadata(V1Alpha1Invocation.V1Alpha1InvocationMetadata.builder()
				.correlationId("abc")
				.build())
			.request(V1Alpha1Invocation.V1Alpha1InvocationRequest.builder()
				.key("test-message")
				.ingressTopic("invoke")
				.egressTopic("invoke-results")
				.value("some value".getBytes())
				.build())
			.build();

		var response = controller.invoke(invocationReq, null);

		assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);

		assertThat(response.getBody()).isInstanceOf(V1Alpha1Invocation.class);

		var resInvocation = (V1Alpha1Invocation) response.getBody();
		assertThat(resInvocation.getMetadata().getCorrelationId()).isEqualTo("abc");
		assertThat(resInvocation.getResponse().getValue()).isEqualTo("some value".getBytes());
	}

	private static class MockTemplatePool<V, R> implements StatefunReplyingKafkaTemplatePool<String, V, R> {
		Map<String, StatefunReplyingKafkaOperations<String, V, R>> topicToTemplateMap;

		MockTemplatePool(Map<String, StatefunReplyingKafkaOperations<String, V, R>> topicToTemplateMap) {
			this.topicToTemplateMap = topicToTemplateMap;
		}

		@Override
		public StatefunReplyingKafkaOperations<String, V, R> getTemplate(String replyTopic) {
			return topicToTemplateMap.get(replyTopic);
		}
	}

	private static class MirroringReplyingKafkaOperations<V, R> implements StatefunReplyingKafkaOperations<String, V, R> {
		@Override
		public StatefunRequestReplyFuture<String, V, R> sendAndReceive(ProducerRecord<String, V> record) {
			var fut = CompletableFuture.completedFuture(new SendResult<>(record, null));
			return new StatefunRequestReplyFuture<>() {{
				setSendFuture(new CompletableToListenableFutureAdapter<>(fut));
			}};
		}

		@Override
		public StatefunRequestReplyFuture<String, V, R> sendAndReceive(ProducerRecord<String, V> record, Duration replyTimeout) {
			return sendAndReceive(record);
		}
	}
}
