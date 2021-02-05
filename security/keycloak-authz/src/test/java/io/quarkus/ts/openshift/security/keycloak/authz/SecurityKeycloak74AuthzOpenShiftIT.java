package io.quarkus.ts.openshift.security.keycloak.authz;

import io.quarkus.ts.openshift.common.AdditionalResources;
import io.quarkus.ts.openshift.common.InjectRouteUrlIntoApp;
import io.quarkus.ts.openshift.common.OnlyIfConfigured;
import io.quarkus.ts.openshift.common.OpenShiftTest;
import org.junit.jupiter.api.Disabled;

@Disabled("Caused by https://github.com/quarkusio/quarkus/issues/14318")
@OpenShiftTest
@AdditionalResources("classpath:deployments/keycloak/version-74.yaml")
@AdditionalResources("classpath:keycloak-realm.yaml")
@AdditionalResources("classpath:deployments/keycloak/deployment.yaml")
@InjectRouteUrlIntoApp(route = "keycloak-plain", envVar = "KEYCLOAK_HTTP_URL")
@OnlyIfConfigured("ts.authenticated-registry")
public class SecurityKeycloak74AuthzOpenShiftIT extends AbstractSecurityKeycloakAuthzOpenShiftIT {
}
