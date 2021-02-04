package io.quarkus.ts.openshift.micrometer;

import io.quarkus.ts.openshift.common.AdditionalResources;
import io.quarkus.ts.openshift.common.OnlyIfConfigured;
import io.quarkus.ts.openshift.common.OpenShiftTest;

@OpenShiftTest
@AdditionalResources("classpath:deployments/kafka/amq-streams.yaml")
@AdditionalResources("classpath:service-monitor.yaml")
@OnlyIfConfigured("ts.authenticated-registry")
public class AmqStreamsAlertEventsOpenShiftIT extends AbstractAlertEventsOpenShiftIT {

}
