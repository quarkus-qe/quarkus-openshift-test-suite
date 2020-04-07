package io.quarkus.ts.openshift.sqldb.mariadb;

import io.quarkus.ts.openshift.common.AdditionalResources;
import io.quarkus.ts.openshift.common.OpenShiftTest;
import io.quarkus.ts.openshift.sqldb.AbstractSqlDatabaseTest;

@OpenShiftTest
@AdditionalResources("classpath:mariadb-10.2.yaml")
public class Mariadb102OpenShiftIT extends AbstractSqlDatabaseTest {
}
