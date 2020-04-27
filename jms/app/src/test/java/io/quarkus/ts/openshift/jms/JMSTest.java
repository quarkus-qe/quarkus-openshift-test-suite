package io.quarkus.ts.openshift.jms;

import io.quarkus.test.common.QuarkusTestResource;
import io.quarkus.test.junit.QuarkusTest;

@QuarkusTest
@QuarkusTestResource(ArtemisTestResource.class)
public class JMSTest extends AbstractJMSTest {
}
