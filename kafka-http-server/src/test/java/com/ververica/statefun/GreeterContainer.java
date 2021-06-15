package com.ververica.statefun;

import com.github.dockerjava.api.command.InspectContainerResponse;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.utility.DockerImageName;
import java.net.URI;

public class GreeterContainer extends GenericContainer<GreeterContainer> {
    public static final DockerImageName DEFAULT_IMAGE = DockerImageName
        .parse("austince/statefun-greeter")
        .withTag("0.0.1");

    private static final int SERVER_PORT = 1108;

    private static final int PORT_NOT_ASSIGNED = -1;

    private int port = PORT_NOT_ASSIGNED;

    public GreeterContainer(DockerImageName image) {
        super(image);
        withEnv("PORT", Integer.toString(SERVER_PORT));
        withExposedPorts(SERVER_PORT);

        withType("greeter.fns/greetings");
        withEgressType("greeter.io/user-greetings");
        withEgressTopic("greetings");
    }

    GreeterContainer withEgressType(String type) {
        return withEnv("EGRESS_TYPE", type);
    }

    GreeterContainer withEgressTopic(String topic) {
        return withEnv("EGRESS_TOPIC", topic);
    }

    GreeterContainer withType(String type) {
        return withEnv("TYPE", type);
    }

    @Override
    protected void containerIsStarting(InspectContainerResponse containerInfo) {
        super.containerIsStarting(containerInfo);
        port = getMappedPort(SERVER_PORT);
    }

    public URI getAddress() {
        if (port == PORT_NOT_ASSIGNED) {
            throw new IllegalStateException("You should start Kafka container first");
        }
        return URI.create(String.format("http://%s:%s", getHost(), port));
    }
}
