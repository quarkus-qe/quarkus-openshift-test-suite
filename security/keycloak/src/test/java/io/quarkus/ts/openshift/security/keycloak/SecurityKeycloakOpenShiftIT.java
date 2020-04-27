package io.quarkus.ts.openshift.security.keycloak;

import io.fabric8.kubernetes.api.model.EnvVar;
import io.fabric8.kubernetes.api.model.HasMetadata;
import io.fabric8.kubernetes.api.model.KubernetesList;
import io.fabric8.kubernetes.client.utils.Serialization;
import io.fabric8.openshift.api.model.DeploymentConfig;
import io.fabric8.openshift.client.OpenShiftClient;
import io.quarkus.ts.openshift.app.metadata.AppMetadata;
import io.quarkus.ts.openshift.common.AdditionalResources;
import io.quarkus.ts.openshift.common.OpenShiftTest;
import io.quarkus.ts.openshift.common.BeforeApplicationDeployment;
import io.quarkus.ts.openshift.common.injection.TestResource;
import org.apache.http.impl.client.HttpClients;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.keycloak.authorization.client.AuthzClient;
import org.keycloak.authorization.client.Configuration;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.List;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;

@OpenShiftTest
@AdditionalResources("classpath:keycloak-realm.yaml")
@AdditionalResources("classpath:keycloak.yaml")
public class SecurityKeycloakOpenShiftIT {
    private static String getKeycloakUrl(OpenShiftClient oc) {
        // @BeforeApplicationDeployment method is invoked too early for @TestResource @WithName("keycloak-plain") approach
        return "http://" + oc.routes().withName("keycloak-plain").get().getSpec().getHost() + "/auth";
    }

    private static String getKeycloakRealmUrl(OpenShiftClient oc) {
        return getKeycloakUrl(oc) + "/realms/test-realm";
    }

    // TODO this is pretty ugly, but I'm tired and can't think of a better way at the moment
    @BeforeApplicationDeployment
    public static void configureKeycloakUrl(OpenShiftClient oc, AppMetadata appMetadata) throws IOException {
        String authServerUrl = getKeycloakRealmUrl(oc);

        List<HasMetadata> objs = oc.load(Files.newInputStream(Paths.get("target/kubernetes/openshift.yml"))).get();
        objs.stream()
                .filter(it -> it instanceof DeploymentConfig)
                .filter(it -> it.getMetadata().getName().equals(appMetadata.appName))
                .map(DeploymentConfig.class::cast)
                .forEach(dc -> {
                    dc.getSpec().getTemplate().getSpec().getContainers().forEach(container -> {
                        container.getEnv().add(
                                new EnvVar("QUARKUS_OIDC_AUTH_SERVER_URL", authServerUrl, null)
                        );
                    });
                });

        KubernetesList list = new KubernetesList();
        list.setItems(objs);
        Serialization.yamlMapper().writeValue(Files.newOutputStream(Paths.get("target/kubernetes/openshift.yml")), list);
    }

    @TestResource
    private OpenShiftClient oc;

    private AuthzClient authzClient;

    @BeforeEach
    public void setup() {
        authzClient = AuthzClient.create(new Configuration(
                getKeycloakUrl(oc),
                "test-realm",
                "test-application-client",
                new HashMap<String, Object>() {{
                    put("secret", "test-application-client-secret");
                }},
                HttpClients.createDefault()
        ));
    }

    private String getToken(String userName, String password) {
        return authzClient.obtainAccessToken(userName, password).getToken();
    }

    @Test
    public void normalUser_userResource() {
        given()
        .when()
                .auth().oauth2(getToken("test-normal-user", "test-normal-user"))
                .get("/user")
        .then()
                .statusCode(200)
                .body(equalTo("Hello, user test-normal-user"));
    }

    @Test
    public void normalUser_userResource_issuer() {
        given()
        .when()
                .auth().oauth2(getToken("test-normal-user", "test-normal-user"))
                .get("/user/issuer")
        .then()
                .statusCode(200)
                .body(equalTo("user token issued by " + getKeycloakRealmUrl(oc)));
    }

    @Test
    public void normalUser_adminResource() {
        given()
        .when()
                .auth().oauth2(getToken("test-normal-user", "test-normal-user"))
                .get("/admin")
        .then()
                .statusCode(403);
    }

    @Test
    public void adminUser_userResource() {
        given()
        .when()
                .auth().oauth2(getToken("test-admin-user", "test-admin-user"))
                .get("/user")
        .then()
                .statusCode(200)
                .body(equalTo("Hello, user test-admin-user"));
    }

    @Test
    public void adminUser_adminResource() {
        given()
        .when()
                .auth().oauth2(getToken("test-admin-user", "test-admin-user"))
                .get("/admin")
        .then()
                .statusCode(200)
                .body(equalTo("Hello, admin test-admin-user"));
    }

    @Test
    public void adminUser_adminResource_issuer() {
        given()
        .when()
                .auth().oauth2(getToken("test-admin-user", "test-admin-user"))
                .get("/admin/issuer")
        .then()
                .statusCode(200)
                .body(equalTo("admin token issued by " + getKeycloakRealmUrl(oc)));
    }

    @Test
    public void noUser_userResource() {
        given()
        .when()
                .get("/user")
        .then()
                .statusCode(401);
    }

    @Test
    public void noUser_adminResource() {
        given()
        .when()
                .get("/admin")
        .then()
                .statusCode(401);
    }
}
