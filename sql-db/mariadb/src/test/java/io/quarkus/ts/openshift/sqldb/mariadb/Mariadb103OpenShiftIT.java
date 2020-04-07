package io.quarkus.ts.openshift.sqldb.mariadb;

import io.quarkus.ts.openshift.common.AdditionalResources;
import io.quarkus.ts.openshift.common.OnlyIfConfigured;
import io.quarkus.ts.openshift.common.OpenShiftTest;
import io.quarkus.ts.openshift.sqldb.AbstractSqlDatabaseTest;

@OpenShiftTest
@AdditionalResources("classpath:mariadb-10.3.yaml")
@OnlyIfConfigured("ts.authenticated-registry")
public class Mariadb103OpenShiftIT extends AbstractSqlDatabaseTest {
}
