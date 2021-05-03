package io.quarkus.ts.openshift.messaging.kafka;

import io.quarkus.ts.openshift.common.AdditionalResources;
import io.quarkus.ts.openshift.common.OnlyIfConfigured;
import io.quarkus.ts.openshift.common.OpenShiftTest;
import io.quarkus.ts.openshift.common.ParallelAdditionalResourcesEnabled;

@OpenShiftTest
@AdditionalResources("classpath:deployments/kafka/amq-streams.yaml")
@AdditionalResources("classpath:deployments/kafka/apicurio.yaml")
@OnlyIfConfigured("ts.authenticated-registry")
@ParallelAdditionalResourcesEnabled
public class AmqStreamsOpenShiftIT extends AbstractKafkaTest {
}
