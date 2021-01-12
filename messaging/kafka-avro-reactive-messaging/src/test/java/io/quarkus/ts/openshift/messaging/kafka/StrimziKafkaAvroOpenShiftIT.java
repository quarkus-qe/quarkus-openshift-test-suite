package io.quarkus.ts.openshift.messaging.kafka;

import io.quarkus.ts.openshift.common.AdditionalResources;
import io.quarkus.ts.openshift.common.OpenShiftTest;

@OpenShiftTest
@AdditionalResources("classpath:strimzi-kafka.yaml")
public class StrimziKafkaAvroOpenShiftIT extends AbstractKafkaTest{
}
