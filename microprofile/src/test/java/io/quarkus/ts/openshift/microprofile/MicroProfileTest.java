package io.quarkus.ts.openshift.microprofile;

import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.ts.openshift.common.DisabledOnQuarkus;

@QuarkusTest
@DisabledOnQuarkus(version = "1\\.3\\..*", reason = "https://github.com/quarkusio/quarkus/pull/7987")
public class MicroProfileTest extends AbstractMicroProfileTest {
}
