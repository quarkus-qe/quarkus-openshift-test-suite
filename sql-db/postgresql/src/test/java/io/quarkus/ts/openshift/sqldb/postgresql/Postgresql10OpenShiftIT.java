package io.quarkus.ts.openshift.sqldb.postgresql;

import io.quarkus.ts.openshift.common.AdditionalResources;
import io.quarkus.ts.openshift.common.OpenShiftTest;
import io.quarkus.ts.openshift.sqldb.AbstractSqlDatabaseTest;

@OpenShiftTest
@AdditionalResources("classpath:postgresql-10.yaml")
public class Postgresql10OpenShiftIT extends AbstractSqlDatabaseTest {
}
