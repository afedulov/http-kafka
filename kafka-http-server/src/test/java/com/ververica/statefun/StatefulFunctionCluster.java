/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.ververica.statefun;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import lombok.SneakyThrows;
import org.apache.flink.configuration.ConfigOption;
import org.apache.flink.configuration.Configuration;
import org.apache.flink.configuration.GlobalConfiguration;
import org.apache.flink.statefun.flink.core.StatefulFunctionsConfig;
import org.junit.rules.ExternalResource;
import org.testcontainers.containers.BindMode;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.Network;
import org.testcontainers.lifecycle.Startable;
import org.testcontainers.utility.DockerImageName;
import org.testcontainers.utility.MountableFile;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

/**
 * Similar to {@link StatefulFunctionsAppContainers} but does not assume anything about the code structure
 * and has a fluent API similar to other testcontainer extensions.
 */
public class StatefulFunctionCluster extends ExternalResource {
  public static final DockerImageName DEFAULT_IMAGE = DockerImageName.parse("apache/flink-statefun");
  public static final DockerImageName DEFAULT_IMAGE_WITH_VERSION = DEFAULT_IMAGE.withTag("3.0.0");


  private static final String MANAGER_HOST = "statefun-app-manager";
  private static final String WORKER_HOST_PREFIX = "statefun-app-worker";

  private final StatefunManagerContainer manager;
  private final List<StatefunWorkerContainer> workers;

  private File checkpointDir;

  public StatefulFunctionCluster(DockerImageName image, int numWorkers) {
    this(
        new StatefunManagerContainer(image),
        workers(image, numWorkers)
    );
  }

  public StatefulFunctionCluster(StatefunManagerContainer manager, List<StatefunWorkerContainer> workers) {
    this.manager = manager.withWorkers(workers.size()).withNetworkAliases(MANAGER_HOST);
    this.workers = workers;

    for (int i = 0; i < workers.size(); i++) {
      var worker = workers.get(i);
      worker.withNetworkAliases(workerHostOf(i));
    }
  }

  public StatefulFunctionCluster dependsOn(Startable... startables) {
    allContainersStream().forEach(c -> Arrays.stream(startables).forEach(c::dependsOn));

    return this;
  }

  public StatefulFunctionCluster withOnStartConsumer(Consumer<GenericStatefunContainer<?>> onStart) {
    allContainersStream().forEach(c -> c.withOnStartConsumer(onStart::accept));

    return this;
  }

  public StatefulFunctionCluster withNetwork(Network network) {
    allContainersStream()
        .forEach((c) -> c.withNetwork(network));

    return this;
  }

  public StatefunManagerContainer getManager() {
    return manager;
  }

  public StatefulFunctionCluster withModule(ModuleDefinition moduleDefinition) {
    allContainersStream()
        .forEach((c) -> c.withModule(moduleDefinition));

    return this;
  }

  public StatefulFunctionCluster withCheckpointDir(String bindPath) {
    allContainersStream()
        .forEach((c) -> c.withFileSystemBind(bindPath, "/checkpoint-dir", BindMode.READ_WRITE));

    return this;
  }

  /** @return the exposed port on master for calling REST APIs. */
  public int getManagerRestPort() {
    return manager.getRestPort();
  }

  private Stream<GenericStatefunContainer<?>> allContainersStream() {
    return Stream.concat(
        Stream.of(manager),
        workers.stream()
    );
  }

  private static List<StatefunWorkerContainer> workers(DockerImageName image, int numWorkers) {
    return IntStream.range(0, numWorkers)
        .mapToObj(i -> new StatefunWorkerContainer(image))
        .collect(Collectors.toList());
  }

  private static String workerHostOf(int workerIndex) {
    return WORKER_HOST_PREFIX + "-" + workerIndex;
  }

  @Override
  protected void before() {
    allContainersStream().forEach(GenericContainer::start);
  }

  @Override
  protected void after() {
    allContainersStream().forEach(GenericContainer::stop);
  }

  public static class StatefunManagerContainer extends GenericStatefunContainer<StatefunManagerContainer> {
    private static final int REST_PORT = 8081;

    public StatefunManagerContainer(DockerImageName imageName) {
      super(imageName);
      asManager();
      withExposedPorts(REST_PORT);
    }

    public StatefunManagerContainer withWorkers(int numWorkers) {
      if (numWorkers <= 0) {
        return self();
      }

      return withCommand("-p " + numWorkers);
    }

    public int getRestPort() {
      return getMappedPort(REST_PORT);
    }
  }

  public static class StatefunWorkerContainer extends GenericStatefunContainer<StatefunWorkerContainer> {
    public StatefunWorkerContainer(DockerImageName imageName) {
      super(imageName);
      asWorker();
    }
  }

  public static class GenericStatefunContainer<SELF extends GenericStatefunContainer<SELF>> extends GenericContainer<SELF> {
    private static final ObjectMapper YAML_MAPPER = new ObjectMapper(new YAMLFactory());

    private static final String MODULE_YAML_MOUNT_PATH = "/opt/statefun/modules/application-module/module.yaml";

    private final Configuration dynamicProperties = new Configuration();

    private Consumer<SELF> onStart;

    public GenericStatefunContainer(DockerImageName imageName) {
      super(imageName);
    }

    public SELF withManagerHost(String host) {
      return withEnv("MASTER_HOST", host);
    }

    public SELF withRole(String role) {
      return withEnv("ROLE", role);
    }

    public SELF asManager() {
      return withRole("master");
    }

    public SELF asWorker() {
      return withRole("worker");
    }

    public SELF withOnStartConsumer(Consumer<SELF> onStart) {
      this.onStart = onStart;
      return self();
    }

    public <T> SELF withFlinkConfiguration(
        ConfigOption<T> config, T value) {
      this.dynamicProperties.set(config, value);
      return self();
    }

    public SELF withModuleGlobalConfiguration(
        String key, String value) {
      this.dynamicProperties.setString(StatefulFunctionsConfig.MODULE_CONFIG_PREFIX + key, value);
      return self();
    }

    public SELF withModule(ModuleDefinition moduleDefinition) {
      try {
        // TODO: write as YAML, current error: com.fasterxml.jackson.dataformat.yaml.snakeyaml.emitter.EmitterException: expected NodeEvent, but got <com.fasterxml.jackson.dataformat.yaml.snakeyaml.events.DocumentEndEvent()>
        // json should be valid YAML though?
        var json = new ObjectMapper().writeValueAsString(moduleDefinition);
        return withFileFromString(json, MODULE_YAML_MOUNT_PATH);
      } catch (IOException ex) {
        throw new RuntimeException(ex);
      }
    }

    @SneakyThrows
    @Override
    protected void doStart() {
      if (onStart != null) {
        onStart.accept(self());
      }

      Configuration flinkConf = resolveFlinkConf(dynamicProperties);
      String flinkConfString = flinkConfigAsString(flinkConf);

      var flinkHome = getEnvMap().get("FLINK_HOME");
      var destPath = String.join("/", flinkHome, "config", "flink-conf.yaml");

      withFileFromString(flinkConfString, destPath);

      super.doStart();
    }

    private SELF withFileFromString(String contents, String containerPath) {
      var file = copyToTempFile(new ByteArrayInputStream(contents.getBytes()));
      return withCopyFileToContainer(MountableFile.forHostPath(file.getAbsolutePath()), containerPath);
    }

    /**
     * Merges set dynamic properties with configuration in the base flink-conf.yaml located in
     * resources.
     */
    private static Configuration resolveFlinkConf(Configuration dynamicProperties) {
      final InputStream baseFlinkConfResourceInputStream =
          GenericStatefunContainer.class.getResourceAsStream("/flink-conf.yaml");
      if (baseFlinkConfResourceInputStream == null) {
        throw new RuntimeException("Base flink-conf.yaml cannot be found.");
      }

      final File tempBaseFlinkConfFile = copyToTempFile(baseFlinkConfResourceInputStream, "flink-conf.yaml");
      return GlobalConfiguration.loadConfiguration(
          tempBaseFlinkConfFile.getParentFile().getAbsolutePath(), dynamicProperties);
    }

    private static String flinkConfigAsString(Configuration configuration) {
      StringBuilder yaml = new StringBuilder();
      for (Map.Entry<String, String> entry : configuration.toMap().entrySet()) {
        yaml.append(entry.getKey()).append(": ").append(entry.getValue()).append("\n");
      }

      return yaml.toString();
    }

    private static File copyToTempFile(InputStream inputStream) {
      try {
        var tempFilePath = Files.createTempFile("statefun-app", ".tmp");
        return quietlyReplaceFile(inputStream, tempFilePath);
      } catch (Exception e) {
        throw new RuntimeException(e);
      }
    }

    private static File copyToTempFile(InputStream inputStream, String filename) {
      try {
        var tempFilePath = new File(
            Files.createTempDirectory("statefun-app").toString(), "flink-conf.yaml").toPath();
        return quietlyReplaceFile(inputStream, tempFilePath);
      } catch (Exception e) {
        throw new RuntimeException(e);
      }
    }

    private static File quietlyReplaceFile(InputStream inputStream, Path path) {
      try {
        Files.copy(inputStream, path, StandardCopyOption.REPLACE_EXISTING);
        return path.toFile();
      } catch (Exception e) {
        throw new RuntimeException(e);
      }
    }
  }
}
