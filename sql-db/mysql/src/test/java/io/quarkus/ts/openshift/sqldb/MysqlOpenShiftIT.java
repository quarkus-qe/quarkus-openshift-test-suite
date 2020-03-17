package io.quarkus.ts.openshift.sqldb;

import io.quarkus.ts.openshift.common.AdditionalResources;
import io.quarkus.ts.openshift.common.OpenShiftTest;

@OpenShiftTest
@AdditionalResources("classpath:mysql.yaml")
public class MysqlOpenShiftIT extends AbstractSqlDatabaseTest {
}
