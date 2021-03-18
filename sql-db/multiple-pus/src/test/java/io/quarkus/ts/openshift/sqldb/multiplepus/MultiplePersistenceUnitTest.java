package io.quarkus.ts.openshift.sqldb.multiplepus;

import io.quarkus.test.junit.QuarkusTest;
import org.junit.jupiter.api.Disabled;

@QuarkusTest
@Disabled("https://github.com/quarkusio/quarkus/issues/15836")
public class MultiplePersistenceUnitTest extends AbstractMultiplePersistenceUnitTest {
}
