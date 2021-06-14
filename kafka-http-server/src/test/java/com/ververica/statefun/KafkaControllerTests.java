package com.ververica.statefun;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.containers.KafkaContainer;

@Import(value = {KafkaTestConfig.class, KafkaConfig.class})
@DirtiesContext
@WebMvcTest(controllers = KafkaController.class)
class KafkaControllerTests {
	@Autowired
	private ObjectMapper mapper;

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private ContainerBean<KafkaContainer> kafkaContainer;

	public StatefulFunctionsAppContainers myApp =
		StatefulFunctionsAppContainers.builder("app-name", 3)
			.withNetwork(kafkaContainer.getContainer().getNetwork())
			.dependsOn(kafkaContainer.getContainer())
			.build();

	@Test
	void invocationSucceeds() throws Exception {
		var student = Student.of("001", "austin", "9");

		var result = mockMvc.perform(post("/invoke")
			.contentType(MediaType.APPLICATION_JSON)
			.accept(MediaType.APPLICATION_JSON)
			.content(mapper.writeValueAsBytes(student))
		).andReturn();

		var response = result.getResponse();

		assertThat(response.getStatus()).isEqualTo(HttpStatus.OK.value());
	}
}
