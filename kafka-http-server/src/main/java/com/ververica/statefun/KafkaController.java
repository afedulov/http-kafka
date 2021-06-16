package com.ververica.statefun;

import com.ververica.statefun.reqreply.StatefunReplyingKafkaTemplatePool;
import com.ververica.statefun.reqreply.StatefunProducerCorrelationIdStrategy;
import com.ververica.statefun.reqreply.StatefunRequestReplyFuture;
import com.ververica.statefun.reqreply.v1alpha1.V1Alpha1Invocation;
import com.ververica.statefun.reqreply.v1alpha1.V1Alpha1Invocation.V1Alpha1InvocationMetadata;
import com.ververica.statefun.reqreply.v1alpha1.V1Alpha1Invocation.V1Alpha1InvocationResponse;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.kafka.requestreply.KafkaReplyTimeoutException;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import java.time.Duration;
import java.util.Optional;
import java.util.concurrent.CompletionException;
import java.util.concurrent.ExecutionException;

@RestController
public class KafkaController {
	StatefunReplyingKafkaTemplatePool<String, byte[], byte[]> pool;
	StatefunReplyingKafkaTemplatePool<String, V1Alpha1Invocation, V1Alpha1Invocation> payloadPool;

	@Autowired
	public KafkaController(StatefunReplyingKafkaTemplatePool<String, byte[], byte[]> pool, StatefunReplyingKafkaTemplatePool<String, V1Alpha1Invocation, V1Alpha1Invocation> payloadPool) {
		this.pool = pool;
		this.payloadPool = payloadPool;
	}

	@PostMapping("/v1alpha1/invocation:in-payload")
	public ResponseEntity<?> invokeViaPayload(@RequestBody V1Alpha1Invocation invocation,  @RequestParam(value = "timeout", defaultValue = "-1") final Long timeout)
		throws InterruptedException, ExecutionException {
		var req = invocation.getRequest();
		if (req == null) {
			return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Missing request");
		}

		var record = new ProducerRecord<>(req.getIngressTopic(), null, req.getKey(), invocation);

		var template = payloadPool.getTemplate(req.getEgressTopic());

		StatefunRequestReplyFuture<String, V1Alpha1Invocation, V1Alpha1Invocation> future;
		if (timeout > 0) {
			future = template.sendAndReceive(record, Duration.ofMillis(timeout));
		} else {
			future = template.sendAndReceive(record);
		}

		var sentRecord = future.getSendFuture().completable().get().getProducerRecord();

		return future
			.completable()
			.thenApply((responseRecord) -> new ResponseEntity<>(responseRecord.value(), HttpStatus.OK))
			.exceptionally((err) -> {
				var cause = err.getCause();
				if (!(cause instanceof KafkaReplyTimeoutException)) {
					throw new CompletionException(cause);
				}

				// If we timeout, just send back a 201 response with the sent record so it can be polled later by correlation ID
				return ResponseEntity.status(HttpStatus.ACCEPTED).body(sentRecord.value());
			})
			.get();
	}

	@PostMapping("/v1alpha1/invocation")
	public ResponseEntity<?> invoke(@RequestBody V1Alpha1Invocation invocation,  @RequestParam(value = "timeout", defaultValue = "-1") final Long timeout)
			throws InterruptedException, ExecutionException {
		var req = invocation.getRequest();
		if (req == null) {
			return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Missing request");
		}

		var correlationId = Optional.ofNullable(invocation.getMetadata())
			.map(V1Alpha1InvocationMetadata::getCorrelationId)
			.orElseGet(StatefunProducerCorrelationIdStrategy::createCorrelationId);

		var record = new ProducerRecord<>(req.getIngressTopic(), null, req.getKey(), req.getValue());
		record.headers().add(StatefunAnnotations.CORRELATION_ID, correlationId.getBytes());

		var template = pool.getTemplate(req.getEgressTopic());

		StatefunRequestReplyFuture<String, byte[], byte[]> future;
		if (timeout > 0) {
			future = template.sendAndReceive(record, Duration.ofMillis(timeout));
		} else {
			future = template.sendAndReceive(record);
		}
		return future
			.completable()
			.thenApply((responseRecord) -> {
				var response = V1Alpha1Invocation.builder()
					.metadata(V1Alpha1InvocationMetadata.builder()
						.correlationId(correlationId)
						.build())
					.response(
						V1Alpha1InvocationResponse.builder()
							.key(responseRecord.key())
							.value(responseRecord.value())
							.build()
					)
					.build();

				return new ResponseEntity<>(response, HttpStatus.OK);
			})
			.exceptionally((err) -> {
				var cause = err.getCause();
				if (!(cause instanceof KafkaReplyTimeoutException)) {
					throw new CompletionException(cause);
				}

				// If we timeout, just send back a 201 response with the correlation ID so it can be polled later
				var response = V1Alpha1Invocation.builder()
					.metadata(V1Alpha1InvocationMetadata.builder()
						.correlationId(correlationId)
						.build())
					.build();

				return ResponseEntity.status(HttpStatus.ACCEPTED).body(response);
			})
			.get();
	}
}
