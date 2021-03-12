package io.quarkus.ts.openshift.messaging.artemisjta;

import io.quarkus.test.common.QuarkusTestResource;
import io.quarkus.test.junit.QuarkusTest;

@QuarkusTest
@QuarkusTestResource(ArtemisTestResource.class)
public class ArtemisJtaTest extends AbstractArtemisTest {
}
