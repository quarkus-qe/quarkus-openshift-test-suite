package io.quarkus.ts.openshift.sqldb.mysql;

import io.quarkus.ts.openshift.common.AdditionalResources;
import io.quarkus.ts.openshift.common.OpenShiftTest;
import io.quarkus.ts.openshift.sqldb.AbstractSqlDatabaseTest;

@OpenShiftTest
@AdditionalResources("classpath:mysql-8.yaml")
public class Mysql8OpenShiftIT extends AbstractSqlDatabaseTest {
}
