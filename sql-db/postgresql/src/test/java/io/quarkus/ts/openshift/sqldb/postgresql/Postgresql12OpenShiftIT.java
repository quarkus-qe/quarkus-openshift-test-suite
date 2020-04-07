package io.quarkus.ts.openshift.sqldb.postgresql;

import io.quarkus.ts.openshift.common.AdditionalResources;
import io.quarkus.ts.openshift.common.OnlyIfConfigured;
import io.quarkus.ts.openshift.common.OpenShiftTest;
import io.quarkus.ts.openshift.sqldb.AbstractSqlDatabaseTest;

@OpenShiftTest
@AdditionalResources("classpath:postgresql-12.yaml")
@OnlyIfConfigured("ts.authenticated-registry")
public class Postgresql12OpenShiftIT extends AbstractSqlDatabaseTest {
}
