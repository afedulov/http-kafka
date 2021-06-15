package com.ververica.statefun;

import static org.assertj.core.api.Assertions.assertThat;

import com.ververica.statefun.ModuleDefinition.KafkaEgress;
import com.ververica.statefun.ModuleDefinition.KafkaEgress.KafkaEgressDeliverySemantic;
import com.ververica.statefun.ModuleDefinition.KafkaEgress.KafkaEgressSpec;
import com.ververica.statefun.ModuleDefinition.KafkaIngress;
import com.ververica.statefun.ModuleDefinition.ModuleMeta;
import com.ververica.statefun.ModuleDefinition.ModuleSpec;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.testcontainers.containers.KafkaContainer;
import org.testcontainers.containers.Network;
import org.testcontainers.containers.output.Slf4jLogConsumer;
import org.testcontainers.utility.DockerImageName;

@SpringBootTest(classes = SpringKafkaSynchronousExampleApplication.class, webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ExtendWith(SpringExtension.class)
class KafkaControllerTests {
	private static final Logger LOG = LoggerFactory.getLogger(KafkaControllerTests.class);

	private static final DockerImageName KAFKA_IMAGE = DockerImageName.parse("confluentinc/cp-kafka:5.0.3");

	private static final String KAFKA_HOST = "kafka-broker";

	public static final Network CONTAINER_NETWORK = Network.newNetwork();

	@Autowired
	TestRestTemplate restTemplate;

	public static GreeterContainer remoteGreeter = new GreeterContainer(GreeterContainer.DEFAULT_IMAGE) {{
		withNetwork(CONTAINER_NETWORK);
		withType("org.apache.flink.statefun.e2e.remote/greeter");
		withEgressTopic("invoke-results");
		withEgressType("org.apache.flink.statefun.e2e.remote/invoke-results");
		withLogConsumer(new Slf4jLogConsumer(LOG));
	}};

	// start kafka with the spring context
	public static final KafkaContainer kafka = new KafkaContainer(KAFKA_IMAGE) {{
		withNetwork(CONTAINER_NETWORK);
		withNetworkAliases(KAFKA_HOST);
		withEnv("KAFKA_TRANSACTION_STATE_LOG_REPLICATION_FACTOR", "1");
		withEnv("KAFKA_TRANSACTION_STATE_LOG_MIN_ISR", "1");
		withLogConsumer(new Slf4jLogConsumer(LOG));
	}};

	public static StatefulFunctionCluster statefunCluster = new StatefulFunctionCluster(StatefulFunctionCluster.DEFAULT_IMAGE_WITH_VERSION, 0) {{
		withNetwork(CONTAINER_NETWORK);

		var manager = getManager();
		manager.withLogConsumer(new Slf4jLogConsumer(LOG));

		dependsOn(kafka, remoteGreeter);

		withOnStartConsumer((container) -> {
			var kafkaAddress = kafka.getBootstrapServers();
			var remoteFnAddress = remoteGreeter.getAddress().toString();

			// yikes...
			var moduleDef = ModuleDefinition.builder()
				.module(
					ModuleDefinition.Module.builder()
						.meta(ModuleMeta.builder()
							.type("remote")
							.build())
						.spec(ModuleSpec.builder()
							// endpoints
							.endpoint(ModuleDefinition.HttpEndpoint.builder()
								.meta(ModuleDefinition.EndpointMeta.builder()
									.kind("http")
									.build())
								.spec(ModuleDefinition.HttpEndpointSpec.builder()
									.functions("org.apache.flink.statefun.e2e.remote/*")
									.urlPathTemplate(remoteFnAddress)
									.maxNumBatchRequests(10_000)
									.build())
								.build())
							// ingress
							.ingress(KafkaIngress.builder()
								.meta(ModuleDefinition.IngressMeta.builder()
									.type("io.statefun.kafka/ingress")
									.id("org.apache.flink.statefun.e2e.remote/invoke")
									.build())
								.spec(KafkaIngress.KafkaIngressSpec.builder()
									.address(kafkaAddress)
									.consumerGroupId("remote-module-e2e")
									.startupPosition(KafkaIngress.KafkaStartupPosition.builder()
										.type("earliest")
										.build())
									.topic(KafkaIngress.KafkaTopic.builder()
										.topic("invoke")
										.valueType("greeter.types/com.ververica.statefun.greeter.types.generated.UserProfile")
										.target("org.apache.flink.statefun.e2e.remote/greeter")
										.build())
									.build())
								.build())
							// egresss
							.egress(
								KafkaEgress.builder()
									.meta(ModuleDefinition.EgressMeta.builder()
										.type("io.statefun.kafka/egress")
										.id("org.apache.flink.statefun.e2e.remote/invoke-results")
										.build())
									.spec(KafkaEgressSpec.builder()
										.address(kafkaAddress)
										.deliverySemantic(KafkaEgressDeliverySemantic.builder()
											.type("exactly-once")
											.transactionTimeoutMillis(900000L)
											.build())
										.build())
									.build()
							)
							.build())
						.build()
				)
				.build();

			container.withModule(moduleDef);
		});
	}};

	@DynamicPropertySource
	static void kafkaProperties(DynamicPropertyRegistry registry) {
		kafka.start();
		registry.add( "spring.kafka.bootstrap-servers", kafka::getBootstrapServers);
	}

	@BeforeEach
	public void before() throws Throwable {
		remoteGreeter.start();
		statefunCluster.before();
	}

	@AfterEach
	public void after() {
		statefunCluster.after();
		remoteGreeter.stop();
	}

	@Test
	void invocationSucceeds() throws Exception {
		var student = new Student("001", "austin", "9");

		var response = restTemplate.postForEntity("/invoke", student, Result.class);

		assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
	}
}
