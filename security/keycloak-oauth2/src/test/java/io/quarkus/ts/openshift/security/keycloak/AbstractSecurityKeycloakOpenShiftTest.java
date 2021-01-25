package io.quarkus.ts.openshift.security.keycloak;

import org.apache.http.impl.client.HttpClients;
import org.gradle.internal.impldep.org.apache.commons.lang.StringUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.keycloak.authorization.client.AuthzClient;
import org.keycloak.authorization.client.Configuration;

import java.util.Collections;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;

public abstract class AbstractSecurityKeycloakOpenShiftTest {

    protected abstract String getOAuth2IntrospectionUrl();

    protected abstract String getAppUrl();

    private AuthzClient authzClient;

    @BeforeEach
    public void setup() {
        authzClient = AuthzClient.create(new Configuration(
                StringUtils.substringBefore(getOAuth2IntrospectionUrl(), "/realms"),
                "test-realm",
                "test-application-client",
                Collections.singletonMap("secret", "test-application-client-secret"),
                HttpClients.createDefault()
        ));
    }

    @Test
    public void normalUser_userResource() {
        given()
        .when()
                .auth().preemptive().oauth2(getToken("test-normal-user", "test-normal-user"))
                .get("/user")
        .then()
                .statusCode(200)
                .body(equalTo("Hello, user test-normal-user"));
    }

    @Test
    public void normalUser_adminResource() {
        given()
        .when()
                .auth().preemptive().oauth2(getToken("test-normal-user", "test-normal-user"))
                .get("/admin")
        .then()
                .statusCode(403);
    }

    @Test
    public void adminUser_userResource() {
        given()
        .when()
                .auth().preemptive().oauth2(getToken("test-admin-user", "test-admin-user"))
                .get("/user")
        .then()
                .statusCode(200)
                .body(equalTo("Hello, user test-admin-user"));
    }

    @Test
    public void adminUser_adminResource() {
        given()
        .when()
                .auth().preemptive().oauth2(getToken("test-admin-user", "test-admin-user"))
                .get("/admin")
        .then()
                .statusCode(200)
                .body(equalTo("Hello, admin test-admin-user"));
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

    private String getToken(String userName, String password) {
        return authzClient.obtainAccessToken(userName, password).getToken();
    }
}
