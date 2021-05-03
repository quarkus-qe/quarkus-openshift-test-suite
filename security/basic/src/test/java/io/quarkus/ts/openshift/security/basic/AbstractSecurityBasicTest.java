package io.quarkus.ts.openshift.security.basic;

import static io.restassured.RestAssured.given;
import static org.hamcrest.CoreMatchers.anything;
import static org.hamcrest.CoreMatchers.equalTo;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Stream;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import io.restassured.response.ValidatableResponse;
import io.restassured.specification.RequestSpecification;

public abstract class AbstractSecurityBasicTest {
    private static final String ADMIN_USERNAME = "albert";
    private static final String ADMIN_PASSWORD = "E!nst3iN";
    private static final String USER_USERNAME = "isaac";
    private static final String USER_PASSWORD = "N3wt0N";
    private static final String UNKNOWN_USERNAME = "unknown";
    private static final String UNKNOWN_PASSWORD = "unknown";
    private static final Set<String> USER_ROLE = new HashSet<>(Arrays.asList(ADMIN_USERNAME, USER_USERNAME));
    private static final Set<String> ADMIN_ROLE = Collections.singleton(ADMIN_USERNAME);

    @ParameterizedTest(name = "[{index}] user {0}, known: {2}")
    @MethodSource("usernamePasswordCombinations")
    public void permitAll(String username, String password, boolean known) {
        test("/permit-all", username, password)
                .statusCode(200)
                .body(equalTo("Hello!"));
    }

    @ParameterizedTest(name = "[{index}] user {0}, known: {2}")
    @MethodSource("usernamePasswordCombinations")
    public void denyAll(String username, String password, boolean known) {
        test("/deny-all", username, password)
                .statusCode(known ? 403 : 401);
    }

    @ParameterizedTest(name = "[{index}] user {0}, known: {2}")
    @MethodSource("usernamePasswordCombinations")
    public void everyone(String username, String password, boolean known) {
        test("/everyone", username, password)
                .statusCode(known ? 200 : 401)
                .body(known ? equalTo("Hello, " + username) : anything());
    }

    @ParameterizedTest(name = "[{index}] user {0}, known: {2}")
    @MethodSource("usernamePasswordCombinations")
    public void user(String username, String password, boolean known) {
        boolean ok = known && USER_ROLE.contains(username);
        test("/user", username, password)
                .statusCode(ok ? 200 : 401)
                .body(ok ? equalTo("Hello, user " + username) : anything());
    }

    @ParameterizedTest(name = "[{index}] user {0}, known: {2}")
    @MethodSource("usernamePasswordCombinations")
    public void admin(String username, String password, boolean known) {
        boolean ok = known && ADMIN_ROLE.contains(username);

        int expectedStatus = ok ? 200 : 401;
        if (known && !ADMIN_ROLE.contains(username)) {
            expectedStatus = 403;
        }

        test("/admin", username, password)
                .statusCode(expectedStatus)
                .body(ok ? equalTo("Hello, admin " + username) : anything());
    }

    private ValidatableResponse test(String url, String username, String password) {
        RequestSpecification test = given();
        if (username != null && password != null) {
            test = test.auth().basic(username, password);
        }

        return test.when().get(url).then();
    }

    static Stream<Arguments> usernamePasswordCombinations() {
        return Stream.of(
                Arguments.of(null, null, false),
                Arguments.of(USER_USERNAME, USER_PASSWORD, true),
                Arguments.of(ADMIN_USERNAME, ADMIN_PASSWORD, true),
                Arguments.of(USER_USERNAME, UNKNOWN_PASSWORD, false),
                Arguments.of(ADMIN_USERNAME, UNKNOWN_PASSWORD, false),
                Arguments.of(UNKNOWN_USERNAME, UNKNOWN_PASSWORD, false));
    }
}
