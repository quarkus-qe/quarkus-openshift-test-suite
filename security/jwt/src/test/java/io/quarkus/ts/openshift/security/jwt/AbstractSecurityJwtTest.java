package io.quarkus.ts.openshift.security.jwt;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.restassured.http.ContentType;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.security.GeneralSecurityException;
import java.security.KeyFactory;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.PrivateKey;
import java.security.spec.PKCS8EncodedKeySpec;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;

public abstract class AbstractSecurityJwtTest {
    private static PrivateKey loadPrivateKey() throws IOException, GeneralSecurityException {
        byte[] bytes = Files.readAllBytes(Paths.get("target/test-classes/private-key.der"));
        return KeyFactory.getInstance("RSA").generatePrivate(new PKCS8EncodedKeySpec(bytes));
    }

    private enum Invalidity {
        WRONG_ISSUER,
        WRONG_DATE,
        WRONG_KEY
    }

    private static String createToken(String... groups) throws IOException, GeneralSecurityException {
        return createToken(Date::new, null, groups);
    }

    private static String createToken(Invalidity invalidity, String... groups) throws IOException, GeneralSecurityException {
        return createToken(Date::new, invalidity, groups);
    }

    private static String createToken(Supplier<Date> clock, Invalidity invalidity, String... groups) throws IOException, GeneralSecurityException {
        String issuer = "https://my.auth.server/";
        if (invalidity == Invalidity.WRONG_ISSUER) {
            issuer = "https://wrong/";
        }

        Date now = clock.get();
        Date expiration = new Date(TimeUnit.SECONDS.toMillis(10) + now.getTime());
        if (invalidity == Invalidity.WRONG_DATE) {
            now = new Date(now.getTime() - TimeUnit.DAYS.toMillis(10));
            expiration = new Date(now.getTime() - TimeUnit.DAYS.toMillis(10));
        }

        PrivateKey privateKey = loadPrivateKey();
        if (invalidity == Invalidity.WRONG_KEY) {
            KeyPairGenerator keyPairGenerator = KeyPairGenerator.getInstance("RSA");
            KeyPair keyPair = keyPairGenerator.generateKeyPair();
            privateKey = keyPair.getPrivate();
        }

        return Jwts.builder()
                .setIssuer(issuer) // iss
                .setId(UUID.randomUUID().toString()) // jti
                .setExpiration(expiration) // exp
                .setIssuedAt(now) // iat
                .setSubject("test_subject_at_example_com") // sub
                .claim("upn", "test-subject@example.com")
                .claim("groups", Arrays.asList(groups))
                .claim("roleMappings", Collections.singletonMap("admin", "superuser"))
                .signWith(privateKey, SignatureAlgorithm.RS256)
                .compact();
    }

    @Test
    public void secured_everyone_noGroup() throws IOException, GeneralSecurityException {
        given()
        .when()
                .auth().oauth2(createToken())
                .get("/secured/everyone")
        .then()
                .statusCode(200)
                .body(equalTo("Hello, test-subject@example.com, your token was issued by https://my.auth.server/ and you are in groups []"));
    }

    @Test
    public void secured_everyone_viewGroup() throws IOException, GeneralSecurityException {
        given()
        .when()
                .auth().oauth2(createToken("view"))
                .get("/secured/everyone")
        .then()
                .statusCode(200)
                .body(equalTo("Hello, test-subject@example.com, your token was issued by https://my.auth.server/ and you are in groups [view]"));
    }

    @Test
    public void secured_everyone_adminGroup() throws IOException, GeneralSecurityException {
        given()
        .when()
                .auth().oauth2(createToken("admin"))
                .get("/secured/everyone")
        .then()
                .statusCode(200)
                .body(equalTo("Hello, test-subject@example.com, your token was issued by https://my.auth.server/ and you are in groups [admin, superuser]"));
    }

    @Test
    public void secured_everyone_wrongIssuer() throws IOException, GeneralSecurityException {
        given()
        .when()
                .auth().oauth2(createToken(Invalidity.WRONG_ISSUER))
                .get("/secured/everyone")
        .then()
                .statusCode(401);
    }

    @Test
    public void secured_everyone_wrongDate() throws IOException, GeneralSecurityException {
        given()
        .when()
                .auth().oauth2(createToken(Invalidity.WRONG_DATE))
                .get("/secured/everyone")
        .then()
                .statusCode(401);
    }

    @Test
    public void secured_everyone_wrongKey() throws IOException, GeneralSecurityException {
        given()
        .when()
                .auth().oauth2(createToken(Invalidity.WRONG_KEY))
                .get("/secured/everyone")
        .then()
                .statusCode(401);
    }

    @Test
    public void secured_admin_noGroup() throws IOException, GeneralSecurityException {
        given()
        .when()
                .auth().oauth2(createToken())
                .get("/secured/admin")
        .then()
                .statusCode(403);
    }

    @Test
    public void secured_admin_viewGroup() throws IOException, GeneralSecurityException {
        given()
        .when()
                .auth().oauth2(createToken("view"))
                .get("/secured/admin")
        .then()
                .statusCode(403);
    }

    @Test
    public void secured_admin_adminGroup() throws IOException, GeneralSecurityException {
        given()
        .when()
                .auth().oauth2(createToken("admin"))
                .get("/secured/admin")
        .then()
                .statusCode(200)
                .body(equalTo("Restricted area! Admin access granted!"));
    }

    @Test
    public void secured_noone_noGroup() throws IOException, GeneralSecurityException {
        given()
        .when()
                .auth().oauth2(createToken())
                .get("/secured/noone")
        .then()
                .statusCode(403);
    }

    @Test
    public void secured_noone_viewGroup() throws IOException, GeneralSecurityException {
        given()
        .when()
                .auth().oauth2(createToken("view"))
                .get("/secured/noone")
        .then()
                .statusCode(403);
    }

    @Test
    public void secured_noone_adminGroup() throws IOException, GeneralSecurityException {
        given()
        .when()
                .auth().oauth2(createToken("admin"))
                .get("/secured/noone")
        .then()
                .statusCode(403);
    }

    @Test
    public void permitted_correctToken() throws IOException, GeneralSecurityException {
        given()
        .when()
                .auth().oauth2(createToken())
                .get("/permitted")
        .then()
                .statusCode(200)
                .body(equalTo("Hello there!"));
    }

    @Test
    public void permitted_wrongIssuer() throws IOException, GeneralSecurityException {
        given()
        .when()
                .auth().oauth2(createToken(Invalidity.WRONG_ISSUER))
                .get("/permitted")
        .then()
                .statusCode(401); // in Thorntail, this is 200, but both approaches are likely valid
    }

    @Test
    public void permitted_wrongDate() throws IOException, GeneralSecurityException {
        given()
        .when()
                .auth().oauth2(createToken(Invalidity.WRONG_DATE))
                .get("/permitted")
        .then()
                .statusCode(401); // in Thorntail, this is 200, but both approaches are likely valid
    }

    @Test
    public void permitted_wrongKey() throws IOException, GeneralSecurityException {
        given()
        .when()
                .auth().oauth2(createToken(Invalidity.WRONG_KEY))
                .get("/permitted")
        .then()
                .statusCode(401); // in Thorntail, this is 200, but both approaches are likely valid
    }

    @Test
    public void denied_correctToken() throws IOException, GeneralSecurityException {
        given()
        .when()
                .auth().oauth2(createToken())
                .get("/denied")
        .then()
                .statusCode(403);
    }

    @Test
    public void mixed_constrained() throws IOException, GeneralSecurityException {
        given()
        .when()
                .auth().oauth2(createToken())
                .get("/mixed/constrained")
        .then()
                .statusCode(200)
                .body(equalTo("Constrained method"));
    }

    @Test
    public void mixed_unconstrained() throws IOException, GeneralSecurityException {
        given()
        .when()
                .auth().oauth2(createToken())
                .get("/mixed/unconstrained")
        .then()
                .statusCode(403); // quarkus.security.deny-unannotated-members=true
    }

    @Test
    public void contentTypes_plain_plainGroup() throws IOException, GeneralSecurityException {
        given()
        .when()
                .auth().oauth2(createToken("plain"))
                .accept(ContentType.TEXT)
                .get("/content-types")
        .then()
                .statusCode(200)
                .body(equalTo("Hello, world!"));
    }

    @Test
    public void contentTypes_plain_webGroup() throws IOException, GeneralSecurityException {
        given()
        .when()
                .auth().oauth2(createToken("web"))
                .accept(ContentType.TEXT)
                .get("/content-types")
        .then()
                .statusCode(403);
    }

    @Test
    public void contentTypes_web_webGroup() throws IOException, GeneralSecurityException {
        given()
        .when()
                .auth().oauth2(createToken("web"))
                .accept(ContentType.HTML)
                .get("/content-types")
        .then()
                .statusCode(200)
                .body(equalTo("<html>Hello, world!</html>"));
    }

    @Test
    public void contentTypes_web_plainGroup() throws IOException, GeneralSecurityException {
        given()
        .when()
                .auth().oauth2(createToken("plain"))
                .accept(ContentType.HTML)
                .get("/content-types")
        .then()
                .statusCode(403);
    }

    @Test
    public void parameterizedPaths_admin_adminGroup() throws IOException, GeneralSecurityException {
        given()
        .when()
                .auth().oauth2(createToken("admin"))
                .get("/parameterized-paths/my/foo/admin")
        .then()
                .statusCode(200)
                .body(equalTo("Admin accessed foo"));
    }

    @Test
    public void parameterizedPaths_admin_viewGroup() throws IOException, GeneralSecurityException {
        given()
        .when()
                .auth().oauth2(createToken("view"))
                .get("/parameterized-paths/my/foo/admin")
        .then()
                .statusCode(403);
    }

    @Test
    public void parameterizedPaths_view_viewGroup() throws IOException, GeneralSecurityException {
        given()
        .when()
                .auth().oauth2(createToken("view"))
                .get("/parameterized-paths/my/foo/view")
        .then()
                .statusCode(200)
                .body(equalTo("View accessed foo"));
    }

    @Test
    public void parameterizedPaths_view_adminGroup() throws IOException, GeneralSecurityException {
        given()
        .when()
                .auth().oauth2(createToken("admin"))
                .get("/parameterized-paths/my/foo/view")
        .then()
                .statusCode(403);
    }

    @Test
    @Disabled("until we properly fix clock skew in our infrastructure")
    public void tokenExpirationGracePeriod() throws IOException, GeneralSecurityException {
        Supplier<Date> clock = () -> {
            Date now = new Date();
            now = new Date(now.getTime() - TimeUnit.SECONDS.toMillis(90));
            return now;
        };

        given()
        .when()
                .auth().oauth2(createToken(clock, null,"admin"))
                .get("/secured/admin")
        .then()
                .statusCode(200)
                .body(equalTo("Restricted area! Admin access granted!"));
    }
}
