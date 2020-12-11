package io.quarkus.ts.openshift.messaging.kafka;

import io.quarkus.ts.openshift.common.AdditionalResources;
import io.quarkus.ts.openshift.common.OnlyIfConfigured;
import io.quarkus.ts.openshift.common.OpenShiftTest;

@OpenShiftTest
@AdditionalResources("classpath:amq-streams.yaml")
@OnlyIfConfigured("ts.authenticated-registry")
public class AmqStreamsOpenShiftIT extends AbstractKafkaTest{
}
