package io.quarkus.ts.openshift.sqldb;

import io.quarkus.ts.openshift.common.AdditionalResources;
import io.quarkus.ts.openshift.common.OpenShiftTest;

@OpenShiftTest
@AdditionalResources("classpath:postgresql.yaml")
public class PostgresqlOpenShiftIT extends AbstractSqlDatabaseTest {
}
