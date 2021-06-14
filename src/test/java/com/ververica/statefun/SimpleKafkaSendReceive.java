package com.ververica.statefun;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

import java.util.Collections;
import java.util.Properties;

import com.ververica.statefun.generated.InvokeOuterClass.Invoke;
import com.ververica.statefun.generated.InvokeOuterClass.InvokeResult;
import org.apache.kafka.clients.consumer.Consumer;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.Producer;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.apache.kafka.common.serialization.StringSerializer;
import org.junit.Rule;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testcontainers.containers.KafkaContainer;
import org.testcontainers.utility.DockerImageName;


public class SimpleKafkaSendReceive {

  private static final Logger LOG = LoggerFactory.getLogger(SimpleKafkaSendReceive.class);
  private static final DockerImageName KAFKA_IMAGE = DockerImageName.parse("confluentinc/cp-kafka:5.0.3");

  private static final String KAFKA_HOST = "kafka-broker";

  private static final String INVOKE_TOPIC = "invoke";
  private static final String INVOKE_RESULTS_TOPIC = "invoke-results";

  @Rule
  public KafkaContainer kafka =
          new KafkaContainer(KAFKA_IMAGE)
                  .withNetworkAliases(KAFKA_HOST)
                  .withEnv("KAFKA_TRANSACTION_STATE_LOG_REPLICATION_FACTOR", "1")
                  .withEnv("KAFKA_TRANSACTION_STATE_LOG_MIN_ISR", "1");

  @Test(timeout = 1000 * 60 * 10)
  public void run() {
    final String kafkaAddress = kafka.getBootstrapServers();

    final Producer<String, Invoke> invokeProducer = kafkaKeyedInvokesProducer(kafkaAddress);
    final Consumer<String, InvokeResult> invokeResultConsumer =
            kafkaInvokeResultsConsumer(kafkaAddress);

    final KafkaIOVerifier<String, Invoke, String, InvokeResult> verifier =
            new KafkaIOVerifier<>(invokeProducer, invokeResultConsumer);

    assertThat(
            verifier.sending(invoke("foo"), invoke("foo"), invoke("bar")),
            verifier.resultsInAnyOrder(
                    is(invokeResult("foo", 1)), is(invokeResult("foo", 2)), is(invokeResult("bar", 1))));

    assertThat(
            verifier.sending(invoke("foo"), invoke("foo"), invoke("bar")),
            verifier.resultsInAnyOrder(
                    is(invokeResult("foo", 3)), is(invokeResult("foo", 4)), is(invokeResult("bar", 2))));
  }

  private static Producer<String, Invoke> kafkaKeyedInvokesProducer(String bootstrapServers) {
    Properties props = new Properties();
    props.put("bootstrap.servers", bootstrapServers);

    return new KafkaProducer<>(
            props, new StringSerializer(), new KafkaProtobufSerializer<>(Invoke.parser()));
  }

  private Consumer<String, InvokeResult> kafkaInvokeResultsConsumer(String bootstrapServers) {
    Properties consumerProps = new Properties();
    consumerProps.setProperty("bootstrap.servers", bootstrapServers);
    consumerProps.setProperty("group.id", "remote-module-e2e");
    consumerProps.setProperty("auto.offset.reset", "earliest");
    consumerProps.setProperty("isolation.level", "read_committed");

    KafkaConsumer<String, InvokeResult> consumer =
            new KafkaConsumer<>(
                    consumerProps,
                    new StringDeserializer(),
                    new KafkaProtobufSerializer<>(InvokeResult.parser()));
    consumer.subscribe(Collections.singletonList(INVOKE_RESULTS_TOPIC));

    return consumer;
  }

  private static ProducerRecord<String, Invoke> invoke(String target) {
    return new ProducerRecord<>(INVOKE_TOPIC, target, Invoke.getDefaultInstance());
  }

  private static InvokeResult invokeResult(String id, int invokeCount) {
    return InvokeResult.newBuilder().setId(id).setInvokeCount(invokeCount).build();
  }

}
