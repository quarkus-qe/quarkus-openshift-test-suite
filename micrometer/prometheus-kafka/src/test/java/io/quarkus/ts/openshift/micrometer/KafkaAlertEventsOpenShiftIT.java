package io.quarkus.ts.openshift.micrometer;

import io.quarkus.ts.openshift.common.AdditionalResources;
import io.quarkus.ts.openshift.common.OpenShiftTest;

@OpenShiftTest
@AdditionalResources("classpath:deployments/kafka/strimzi.yaml")
@AdditionalResources("classpath:service-monitor.yaml")
public class KafkaAlertEventsOpenShiftIT extends AbstractAlertEventsOpenShiftIT {

}
