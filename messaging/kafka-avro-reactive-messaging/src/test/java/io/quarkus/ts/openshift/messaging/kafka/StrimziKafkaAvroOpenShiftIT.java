package io.quarkus.ts.openshift.messaging.kafka;

import io.quarkus.ts.openshift.common.AdditionalResources;
import io.quarkus.ts.openshift.common.OpenShiftTest;
import io.quarkus.ts.openshift.common.ParallelAdditionalResourcesEnabled;

@OpenShiftTest
@AdditionalResources("classpath:deployments/kafka/strimzi.yaml")
@AdditionalResources("classpath:deployments/kafka/apicurio.yaml")
@ParallelAdditionalResourcesEnabled
public class StrimziKafkaAvroOpenShiftIT extends AbstractKafkaTest {
}
