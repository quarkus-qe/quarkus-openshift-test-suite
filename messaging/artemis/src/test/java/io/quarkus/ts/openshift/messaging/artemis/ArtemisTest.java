package io.quarkus.ts.openshift.messaging.artemis;

import io.quarkus.test.common.QuarkusTestResource;
import io.quarkus.test.junit.QuarkusTest;

@QuarkusTest
@QuarkusTestResource(ArtemisTestResource.class)
public class ArtemisTest extends AbstractArtemisTest {
}
