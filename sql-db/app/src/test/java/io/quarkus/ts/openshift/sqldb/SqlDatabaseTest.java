package io.quarkus.ts.openshift.sqldb;

import io.quarkus.test.junit.QuarkusTest;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;

import static io.restassured.RestAssured.when;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.Matchers.hasSize;

@QuarkusTest
public class SqlDatabaseTest extends AbstractSqlDatabaseTest {
}
