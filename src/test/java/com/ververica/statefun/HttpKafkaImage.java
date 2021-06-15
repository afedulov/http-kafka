package com.ververica.statefun;

import org.junit.Rule;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.output.Slf4jLogConsumer;
import org.testcontainers.images.builder.ImageFromDockerfile;

import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;

public class HttpKafkaImage {

  private static final Logger LOG = LoggerFactory.getLogger(HttpKafkaImage.class);

  private static final String REMOTE_FUNCTION_HOST = "http-kafka";

  @Rule
  public GenericContainer<?> httpKafka =
      new GenericContainer<>(httpKafkaImage())
          .withNetworkAliases(REMOTE_FUNCTION_HOST)
          .withLogConsumer(new Slf4jLogConsumer(LOG));

  @Test(timeout = 1000 * 60 * 10)
  public void noop() throws Exception {

    System.out.println("BLA");
  }

  private static ImageFromDockerfile httpKafkaImage() {
    return new ImageFromDockerfile("http-kafka-test", false)
        .withFileFromPath("Dockerfile", httpKafkaDockerfile())
        .withFileFromPath("target/", targetPath());
  }

  private static Path targetPath() {
    return Paths.get(System.getProperty("user.dir") + File.separator + "target");
  }

  private static Path httpKafkaDockerfile(){
    return Paths.get(System.getProperty("user.dir") + File.separator + "Dockerfile.test");
  }

}
