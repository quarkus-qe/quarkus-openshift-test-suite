package io.quarkus.ts.openshift.jms.qpidamqp;

import io.quarkus.ts.openshift.common.AdditionalResources;
import io.quarkus.ts.openshift.common.OpenShiftTest;
import io.quarkus.ts.openshift.jms.AbstractJMSTest;

@OpenShiftTest
@AdditionalResources("classpath:amq.yaml")
public class ArtemisOpenShiftIT extends AbstractJMSTest {
}
