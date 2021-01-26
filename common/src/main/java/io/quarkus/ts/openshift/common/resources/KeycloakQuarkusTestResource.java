package io.quarkus.ts.openshift.common.resources;

import io.quarkus.test.common.QuarkusTestResourceLifecycleManager;
import org.testcontainers.containers.BindMode;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.wait.strategy.Wait;

import java.time.Duration;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

public class KeycloakQuarkusTestResource implements QuarkusTestResourceLifecycleManager {

    private static final String OIDC_AUTH_URL_PROPERTY = "quarkus.oidc.auth-server-url";
    private static final String OIDC_TOKEN_ISSUER_PROPERTY = "quarkus.oidc.token.issuer";
    private static final String OAUTH2_INTROSPECTION_URL_PROPERTY = "quarkus.oauth2.introspection-url";

    private static final String USER = "admin";
    private static final String PASSWORD = "admin";
    private static final String REALM = "test-realm";
    private static final int PORT = 8080;

    private static final String REALM_FILE = "/tmp/realm.json";
    private static final String KEYCLOAK_IMAGE = "quay.io/keycloak/keycloak:11.0.3";

    private GenericContainer<?> container;

    @SuppressWarnings("resource")
    @Override
    public Map<String, String> start() {

        container = new GenericContainer<>(KEYCLOAK_IMAGE)
                .withEnv("KEYCLOAK_USER", USER)
                .withEnv("KEYCLOAK_PASSWORD", PASSWORD)
                .withEnv("KEYCLOAK_IMPORT", REALM_FILE)
                .withClasspathResourceMapping("keycloak-realm.json", REALM_FILE, BindMode.READ_ONLY)
                .waitingFor(Wait.forHttp("/auth").withStartupTimeout(Duration.ofMinutes(5)));
        container.addExposedPort(PORT);
        container.start();

        return providesQuarkusConfiguration();
    }

    @Override
    public void stop() {
        Optional.ofNullable(container).ifPresent(GenericContainer::stop);
    }

    protected Map<String, String> providesQuarkusConfiguration() {
        return Collections.emptyMap();
    }

    protected String realmAuthUrl() {
        return String.format("http://localhost:%s/auth/realms/%s", container.getMappedPort(PORT), REALM);
    }

    protected String tokenInstrospectUrl() {
        return String.format("%s/protocol/openid-connect/token/introspect", realmAuthUrl());
    }

    public static class WithOidcConfig extends KeycloakQuarkusTestResource {
        @Override
        protected Map<String, String> providesQuarkusConfiguration() {
            return Collections.singletonMap(OIDC_AUTH_URL_PROPERTY, realmAuthUrl());
        }
    }

    public static class WithOidcAndTokenIssuerConfig extends WithOidcConfig {
        @Override
        protected Map<String, String> providesQuarkusConfiguration() {
            Map<String, String> properties = new HashMap<>(super.providesQuarkusConfiguration());
            properties.put(OIDC_TOKEN_ISSUER_PROPERTY, realmAuthUrl());
            return properties;
        }
    }

    public static class WithOAuth2Config extends KeycloakQuarkusTestResource {
        @Override
        protected Map<String, String> providesQuarkusConfiguration() {
            return Collections.singletonMap(OAUTH2_INTROSPECTION_URL_PROPERTY, tokenInstrospectUrl());
        }
    }

}
