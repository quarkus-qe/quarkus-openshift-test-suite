package io.quarkus.ts.openshift.sqldb.multiplepus;

import io.quarkus.ts.openshift.common.AdditionalResources;
import io.quarkus.ts.openshift.common.OpenShiftTest;

@OpenShiftTest
@AdditionalResources("classpath:mariadb-10.2.yaml")
@AdditionalResources("classpath:postgresql-10.yaml")
public class MultiplePersistenceUnitIT extends AbstractMultiplePersistenceUnitTest {
}
