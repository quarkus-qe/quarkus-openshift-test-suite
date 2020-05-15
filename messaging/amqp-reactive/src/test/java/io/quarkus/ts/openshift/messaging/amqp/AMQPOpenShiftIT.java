package io.quarkus.ts.openshift.messaging.amqp;

import io.quarkus.ts.openshift.common.AdditionalResources;
import io.quarkus.ts.openshift.common.OpenShiftTest;

@OpenShiftTest
@AdditionalResources("classpath:amq.yaml")
public class AMQPOpenShiftIT extends AbstractAMQPTest {
}
