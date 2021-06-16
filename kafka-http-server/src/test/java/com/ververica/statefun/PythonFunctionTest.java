package com.ververica.statefun;

import com.ververica.statefun.generated.InvokeOuterClass.Invoke;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.runner.RunWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.junit4.SpringRunner;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.KafkaContainer;
import org.testcontainers.containers.Network;
import org.testcontainers.containers.output.Slf4jLogConsumer;
import org.testcontainers.images.builder.ImageFromDockerfile;
import org.testcontainers.utility.DockerImageName;

import java.nio.file.Path;
import java.nio.file.Paths;

@SpringBootTest(classes = KafkaHttpApplication.class, webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@RunWith(SpringRunner.class)
class PythonFunctionTest {
	private static final Logger LOG = LoggerFactory.getLogger(PythonFunctionTest.class);

	private static final DockerImageName KAFKA_IMAGE = DockerImageName.parse("confluentinc/cp-kafka:5.0.3");

	private static final String KAFKA_HOST = "kafka-broker";
	private static final String REMOTE_FUNCTION_HOST = "remote-function";

	private static final String INVOKE_TOPIC = "invoke";
	private static final String INVOKE_RESULTS_TOPIC = "invoke-results";

	private static final int NUM_WORKERS = 2;

	public static final Network CONTAINER_NETWORK = Network.newNetwork();

	@Autowired
	TestRestTemplate restTemplate;

//	public static GreeterContainer remoteGreeter = new GreeterContainer(GreeterContainer.DEFAULT_IMAGE) {{
//		withNetwork(CONTAINER_NETWORK);
//		withType("org.apache.flink.statefun.e2e.remote/greeter");
//		withEgressTopic("invoke-results");
//		withEgressType("org.apache.flink.statefun.e2e.remote/invoke-results");
//		withLogConsumer(new Slf4jLogConsumer(LOG));
//	}};

	public static GenericContainer<?> remoteFunction =
			new GenericContainer<>(remoteFunctionImage())
//					.withNetwork(CONTAINER_NETWORK)
					.withNetworkAliases(REMOTE_FUNCTION_HOST)
					.withLogConsumer(new Slf4jLogConsumer(LOG));

	// start kafka with the spring context
	public static final KafkaContainer kafka = new KafkaContainer(KAFKA_IMAGE) {{
//		withNetwork(CONTAINER_NETWORK);
		withNetworkAliases(KAFKA_HOST);
		withEnv("KAFKA_TRANSACTION_STATE_LOG_REPLICATION_FACTOR", "1");
		withEnv("KAFKA_TRANSACTION_STATE_LOG_MIN_ISR", "1");
		withLogConsumer(new Slf4jLogConsumer(LOG));
	}};

	public static StatefulFunctionsAppContainers statefunCluster =
			StatefulFunctionsAppContainers.builder("remote-module-verification", NUM_WORKERS)
//					.withNetwork(CONTAINER_NETWORK)
					.dependsOn(kafka)
					.dependsOn(remoteFunction)
					.exposeMasterLogs(LOG)
					.withBuildContextFileFromClasspath("remote-module", "/remote-module/")
					.build();

	private static ImageFromDockerfile remoteFunctionImage() {
		final Path pythonSourcePath = remoteFunctionPythonSourcePath();
		LOG.info("Building remote function image with Python source at: {}", pythonSourcePath);

		return new ImageFromDockerfile("remote-function", false)
				.withFileFromClasspath("Dockerfile", "Dockerfile.remote-function")
				.withFileFromPath("source/", pythonSourcePath)
				.withFileFromClasspath("requirements.txt", "requirements.txt");
	}

	private static Path remoteFunctionPythonSourcePath() {
		return Paths.get(System.getProperty("user.dir") + "/src/main/python");
	}

	@BeforeEach
	public void before() throws Throwable {
		remoteFunction.start();
		statefunCluster.before();
	}

	@AfterEach
	public void after() {
		statefunCluster.after();
		remoteFunction.stop();
	}

	@Test
	void invocationSucceeds() throws Exception {
//		var student = new Student("001", "austin", "9");
//		var response = restTemplate.postForEntity("/invoke", student, Result.class);
//		assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
//		new ProducerRecord<>(INVOKE_TOPIC, "foo", Invoke.getDefaultInstance());
	}

	@DynamicPropertySource
	static void kafkaProperties(DynamicPropertyRegistry registry) {
		kafka.start();
		registry.add( "spring.kafka.bootstrap-servers", kafka::getBootstrapServers);
	}
}
