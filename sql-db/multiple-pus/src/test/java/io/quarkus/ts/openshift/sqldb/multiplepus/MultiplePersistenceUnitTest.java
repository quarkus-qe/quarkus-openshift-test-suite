package io.quarkus.ts.openshift.sqldb.multiplepus;

import org.junit.jupiter.api.Disabled;

import io.quarkus.test.junit.QuarkusTest;

@QuarkusTest
@Disabled("https://github.com/quarkusio/quarkus/issues/15836")
public class MultiplePersistenceUnitTest extends AbstractMultiplePersistenceUnitTest {
}
