package com.ververica.statefun;

import org.junit.Rule;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.output.Slf4jLogConsumer;
import org.testcontainers.images.builder.ImageFromDockerfile;

import java.nio.file.Path;
import java.nio.file.Paths;

public class RemoteImage {

  private static final Logger LOG = LoggerFactory.getLogger(RemoteImage.class);

  private static final String REMOTE_FUNCTION_HOST = "remote-function";

  @Rule
  public GenericContainer<?> remoteFunction =
      new GenericContainer<>(remoteFunctionImage())
          .withNetworkAliases(REMOTE_FUNCTION_HOST)
          .withLogConsumer(new Slf4jLogConsumer(LOG));

  @Test(timeout = 1000 * 60 * 10)
  public void noop() throws Exception {
    System.out.println("BLA");
  }

  private static ImageFromDockerfile remoteFunctionImage() {
    final Path pythonSourcePath = remoteFunctionPythonSourcePath();
    LOG.info("Building remote function image with Python source at: {}", pythonSourcePath);

    return new ImageFromDockerfile("remote-function", false)
        .withFileFromClasspath("Dockerfile", "Dockerfile.remote-function")
        .withFileFromPath("source/", pythonSourcePath)        .withFileFromClasspath("requirements.txt", "requirements.txt");
  }

  private static Path remoteFunctionPythonSourcePath() {
    return Paths.get(System.getProperty("user.dir") + "/src/main/python");
  }

}
