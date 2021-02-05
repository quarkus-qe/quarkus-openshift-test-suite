package io.quarkus.ts.openshift.security.keycloak;

import org.junit.jupiter.api.Test;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;

public abstract class AbstractSecurityKeycloakOpenShiftTest {

    protected abstract String getAuthServerUrl();

    @Test
    public void clientCredentials_securedResource() {
        given()
                .when()
                .auth().preemptive().oauth2(TokenProviderMethod.CLIENT_CREDENTIALS.getToken())
                .get("/secured")
                .then()
                .statusCode(200)
                .body(equalTo("Hello, user service-account-test-application-client"));
    }

    @Test
    public void jwtSecret_securedResource() {
        given()
                .when()
                .auth().preemptive().oauth2(TokenProviderMethod.JWT.getToken())
                .get("/secured")
                .then()
                .statusCode(200)
                .body(equalTo("Hello, user service-account-test-application-client-jwt"));
    }

    @Test
    public void normalUser_userResource() {
        given()
                .when()
                .auth().preemptive().oauth2(TokenProviderMethod.NORMAL_USER.getToken())
                .get("/user")
                .then()
                .statusCode(200)
                .body(equalTo("Hello, user test-normal-user"));
    }

    @Test
    public void normalUser_userResource_issuer() {
        given()
                .when()
                .auth().oauth2(TokenProviderMethod.NORMAL_USER.getToken())
                .get("/user/issuer")
                .then()
                .statusCode(200)
                .body(equalTo("user token issued by " + getAuthServerUrl()));
    }

    @Test
    public void normalUser_adminResource() {
        given()
                .when()
                .auth().preemptive().oauth2(TokenProviderMethod.NORMAL_USER.getToken())
                .get("/admin")
                .then()
                .statusCode(403);
    }

    @Test
    public void adminUser_userResource() {
        given()
                .when()
                .auth().preemptive().oauth2(TokenProviderMethod.ADMIN_USER.getToken())
                .get("/user")
                .then()
                .statusCode(200)
                .body(equalTo("Hello, user test-admin-user"));
    }

    @Test
    public void adminUser_adminResource() {
        given()
                .when()
                .auth().preemptive().oauth2(TokenProviderMethod.ADMIN_USER.getToken())
                .get("/admin")
                .then()
                .statusCode(200)
                .body(equalTo("Hello, admin test-admin-user"));
    }

    @Test
    public void noUser_securedResource() {
        given()
                .when()
                .get("/secured")
                .then()
                .statusCode(401);
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

    public enum TokenProviderMethod {
        CLIENT_CREDENTIALS("client-credentials"),
        JWT("jwt-secret"),
        NORMAL_USER("normal-user-password"),
        ADMIN_USER("admin-user-password");

        private final String path;

        TokenProviderMethod(String path) {
            this.path = path;
        }

        public String getToken() {
            return given().when().get("/generate-token/" + path).then().statusCode(200).extract().asString();
        }
    }
}
