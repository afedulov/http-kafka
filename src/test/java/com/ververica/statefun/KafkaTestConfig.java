package com.ververica.statefun;

import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.apache.kafka.common.serialization.StringSerializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.core.ConsumerFactory;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.kafka.core.DefaultKafkaProducerFactory;
import org.springframework.kafka.core.ProducerFactory;
import org.springframework.kafka.support.serializer.JsonDeserializer;
import org.springframework.kafka.support.serializer.JsonSerializer;
import org.testcontainers.containers.KafkaContainer;
import org.testcontainers.containers.output.Slf4jLogConsumer;
import org.testcontainers.utility.DockerImageName;
import java.util.HashMap;
import java.util.Map;

@TestConfiguration
public class KafkaTestConfig {
    private static final Logger LOG = LoggerFactory.getLogger(KafkaTestConfig.class);

    private static final DockerImageName KAFKA_IMAGE = DockerImageName.parse("confluentinc/cp-kafka:5.0.3");

    private static final String KAFKA_HOST = "kafka-broker";

    @Bean
    ContainerBean<KafkaContainer> kafkaContainer() {
        return new ContainerBean<>(new KafkaContainer(KAFKA_IMAGE)
            .withNetworkAliases(KAFKA_HOST)
            .withEnv("KAFKA_TRANSACTION_STATE_LOG_REPLICATION_FACTOR", "1")
            .withEnv("KAFKA_TRANSACTION_STATE_LOG_MIN_ISR", "1")
            .withLogConsumer(new Slf4jLogConsumer(LOG)));
    }

    @Bean
    ConcurrentKafkaListenerContainerFactory<String, Result> kafkaListenerContainerFactory(ContainerBean<KafkaContainer> kafka) {
        var factory = new ConcurrentKafkaListenerContainerFactory<String, Result>();
        factory.setConsumerFactory(consumerFactory(kafka));
        return factory;
    }

    @Bean
    public ConsumerFactory<String, Result> consumerFactory(ContainerBean<KafkaContainer> kafka) {
        return new DefaultKafkaConsumerFactory<>(consumerConfigs(kafka));
    }

    @Bean
    public Map<String, Object> consumerConfigs(ContainerBean<KafkaContainer> kafka) {
        Map<String, Object> props = new HashMap<>();
        props.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, kafka.getContainer().getBootstrapServers());
        props.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
        props.put(ConsumerConfig.GROUP_ID_CONFIG, "test-group");
        props.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        props.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, JsonDeserializer.class);
        return props;
    }

    @Bean
    public ProducerFactory<String, Result> resultProducerFactory(ContainerBean<KafkaContainer> kafka) {
        Map<String, Object> configProps = new HashMap<>();
        configProps.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, kafka.getContainer().getBootstrapServers());
        configProps.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
        configProps.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, JsonSerializer.class);
        return new DefaultKafkaProducerFactory<>(configProps);
    }

    @Bean
    public ProducerFactory<String, Student> producerFactory(ContainerBean<KafkaContainer> kafka) {
        Map<String, Object> configProps = new HashMap<>();
        configProps.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, kafka.getContainer().getBootstrapServers());
        configProps.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
        configProps.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, JsonSerializer.class);
        return new DefaultKafkaProducerFactory<>(configProps);
    }
}
