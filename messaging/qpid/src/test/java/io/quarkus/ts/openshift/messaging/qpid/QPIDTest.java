package io.quarkus.ts.openshift.messaging.qpid;

import io.quarkus.test.common.QuarkusTestResource;
import io.quarkus.test.junit.QuarkusTest;

@QuarkusTest
@QuarkusTestResource(ArtemisTestResource.class)
public class QPIDTest extends AbstractQPIDTest {
}
