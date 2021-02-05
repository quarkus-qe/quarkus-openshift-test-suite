package io.quarkus.ts.openshift.security.keycloak.authz;

import io.quarkus.ts.openshift.common.AdditionalResources;
import io.quarkus.ts.openshift.common.InjectRouteUrlIntoApp;
import io.quarkus.ts.openshift.common.OpenShiftTest;

@OpenShiftTest
@AdditionalResources("classpath:deployments/keycloak/version-73.yaml")
@AdditionalResources("classpath:keycloak-realm.yaml")
@AdditionalResources("classpath:deployments/keycloak/deployment.yaml")
@InjectRouteUrlIntoApp(route = "keycloak-plain", envVar = "KEYCLOAK_HTTP_URL")
// Run this test always as for RH SSO 7.4 is not working. Related issue: https://github.com/quarkusio/quarkus/issues/14318
// @OnlyIfNotConfigured("ts.authenticated-registry")
public class SecurityKeycloak73AuthzOpenShiftIT extends AbstractSecurityKeycloakAuthzOpenShiftIT {
}
