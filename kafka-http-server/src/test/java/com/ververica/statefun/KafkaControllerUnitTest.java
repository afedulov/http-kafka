package com.ververica.statefun;

import static org.assertj.core.api.Assertions.assertThat;

import com.ververica.statefun.reqreply.ReplyingKafkaTemplatePool;
import com.ververica.statefun.reqreply.v1alpha1.V1Alpha1Invocation;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.kafka.requestreply.ReplyingKafkaOperations;
import org.springframework.kafka.requestreply.RequestReplyFuture;
import org.springframework.kafka.support.SendResult;
import org.springframework.util.concurrent.CompletableToListenableFutureAdapter;
import java.time.Duration;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

class KafkaControllerUnitTest {
	@Test
	void invocationSucceeds() throws ExecutionException, InterruptedException {
		var pool = new MockTemplatePool(Map.of(
			"invoke-results",
			new MirroringReplyingKafkaOperations()
		));
		var controller = new KafkaController(pool);

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

	private static class MockTemplatePool implements ReplyingKafkaTemplatePool<String, byte[], byte[]> {
		Map<String, ReplyingKafkaOperations<String, byte[], byte[]>> topicToTemplateMap;

		MockTemplatePool(Map<String, ReplyingKafkaOperations<String, byte[], byte[]>> topicToTemplateMap) {
			this.topicToTemplateMap = topicToTemplateMap;
		}

		@Override
		public ReplyingKafkaOperations<String, byte[], byte[]> getTemplate(String replyTopic) {
			return topicToTemplateMap.get(replyTopic);
		}
	}

	private static class MirroringReplyingKafkaOperations implements ReplyingKafkaOperations<String, byte[], byte[]> {
		@Override
		public RequestReplyFuture<String, byte[], byte[]> sendAndReceive(ProducerRecord<String, byte[]> record) {
			var fut = CompletableFuture.completedFuture(new SendResult<>(record, null));
			return new RequestReplyFuture<>() {{
				setSendFuture(new CompletableToListenableFutureAdapter<>(fut));
			}};
		}

		@Override
		public RequestReplyFuture<String, byte[], byte[]> sendAndReceive(ProducerRecord<String, byte[]> record, Duration replyTimeout) {
			return sendAndReceive(record);
		}
	}
}
