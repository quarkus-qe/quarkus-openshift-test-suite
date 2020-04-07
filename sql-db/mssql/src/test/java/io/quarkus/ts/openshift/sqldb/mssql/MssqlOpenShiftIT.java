package io.quarkus.ts.openshift.sqldb.mssql;

import io.quarkus.ts.openshift.common.AdditionalResources;
import io.quarkus.ts.openshift.common.OpenShiftTest;
import io.quarkus.ts.openshift.sqldb.AbstractSqlDatabaseTest;

@OpenShiftTest
@AdditionalResources("classpath:mssql.yaml")
public class MssqlOpenShiftIT extends AbstractSqlDatabaseTest {
}
