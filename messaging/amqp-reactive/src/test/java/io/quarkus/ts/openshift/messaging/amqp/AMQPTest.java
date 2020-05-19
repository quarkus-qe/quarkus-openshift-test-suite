package io.quarkus.ts.openshift.messaging.amqp;

import io.quarkus.test.common.QuarkusTestResource;
import io.quarkus.test.junit.QuarkusTest;

@QuarkusTest
@QuarkusTestResource(ArtemisTestResource.class)
public class AMQPTest extends AbstractAMQPTest {
}
