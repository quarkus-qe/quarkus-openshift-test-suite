package io.quarkus.ts.openshift.messaging.kafka;

import java.util.HashMap;
import java.util.Map;

import org.jboss.logging.Logger;

import io.quarkus.test.common.QuarkusTestResourceLifecycleManager;
import io.strimzi.StrimziKafkaContainer;

public class StrimziKafkaResource implements QuarkusTestResourceLifecycleManager {

    private static final Logger LOG = Logger.getLogger(StrimziKafkaResource.class);
    private StrimziKafkaContainer kafkaContainer;

    @Override
    public Map<String, String> start() {
        kafkaContainer = new StrimziKafkaContainer("latest-kafka-2.7.0");
        kafkaContainer.start();

        String kafkaUrl = kafkaContainer.getBootstrapServers();
        LOG.info(String.format("TestContainers Kafka URL -> %s", kafkaUrl));

        Map<String, String> config = new HashMap<>();
        config.put("kafka.bootstrap.servers", kafkaUrl);
        config.put("quarkus.kafka-streams.bootstrap-servers", kafkaUrl);

        return config;
    }

    @Override
    public void stop() {
        kafkaContainer.close();
    }
}
