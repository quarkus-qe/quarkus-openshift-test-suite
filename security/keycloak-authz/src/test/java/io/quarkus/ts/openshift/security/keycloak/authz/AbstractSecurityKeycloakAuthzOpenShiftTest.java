package io.quarkus.ts.openshift.security.keycloak.authz;

import org.apache.commons.lang3.StringUtils;
import org.apache.http.impl.client.HttpClients;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.keycloak.authorization.client.AuthzClient;
import org.keycloak.authorization.client.Configuration;

import java.util.Collections;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;

public abstract class AbstractSecurityKeycloakAuthzOpenShiftTest {

    private AuthzClient authzClient;

    protected abstract String getAuthServerUrl();

    @BeforeEach
    public void setup() {
        authzClient = AuthzClient.create(new Configuration(
                StringUtils.substringBefore(getAuthServerUrl(), "/realms"),
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
                .body(equalTo("user token issued by " + getAuthServerUrl()));
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
                .body(equalTo("admin token issued by " + getAuthServerUrl()));
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
