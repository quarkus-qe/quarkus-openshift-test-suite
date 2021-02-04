package io.quarkus.ts.openshift.http;

import io.quarkus.ts.openshift.common.injection.TestResource;
import io.smallrye.mutiny.Uni;
import io.vertx.core.http.HttpVersion;
import io.vertx.core.json.JsonObject;
import io.vertx.core.net.JksOptions;
import io.vertx.ext.web.client.WebClientOptions;
import io.vertx.mutiny.core.Vertx;
import io.vertx.mutiny.ext.web.client.HttpResponse;
import io.vertx.mutiny.ext.web.client.WebClient;
import io.vertx.mutiny.ext.web.client.predicate.ResponsePredicate;
import io.vertx.mutiny.ext.web.client.predicate.ResponsePredicateResult;
import org.apache.commons.lang3.StringUtils;
import org.apache.http.impl.client.HttpClients;
import org.eclipse.microprofile.rest.client.inject.RestClient;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.keycloak.authorization.client.AuthzClient;
import org.keycloak.authorization.client.Configuration;

import javax.inject.Inject;
import javax.ws.rs.core.Response;

import java.io.File;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Paths;
import java.time.Duration;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static io.quarkus.ts.openshift.http.HttpClientVersionResource.HTTP_VERSION;
import static io.restassured.RestAssured.given;
import static io.restassured.RestAssured.when;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public abstract class AbstractHttpTest {

    private static final int TIMEOUT_SEC = 3;
    private static final int RETRY = 3;

    @Inject
    @RestClient
    private HttpVersionClientService httpVersionClientService;

    @Inject
    @RestClient
    private HttpVersionClientServiceAsync httpVersionClientServiceAsync;

    @TestResource
    private URL appEndpoint;

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
    @DisplayName("Http/1.1 Server test")
    public void httpServer() {
        when()
                .get("/hello")
                .then()
                .statusLine("HTTP/1.1 200 OK")
                .statusCode(200)
                .body("content", is("Hello, World!"));
    }

    @Test
    @DisplayName("GRPC Server test")
    public void testGrpc() {
        given()
                .when().get("/grpc/trinity")
                .then().statusCode(200)
                .body(is("Hello trinity"));
    }

    @Test
    @DisplayName("Http/2 Server test")
    public void http2Server() throws InterruptedException, URISyntaxException {

        URL res = getClass().getClassLoader().getResource("META-INF/resources/server.truststore");
        File file = Paths.get(res.toURI()).toFile();
        String truststore = file.getAbsolutePath();

        CountDownLatch done = new CountDownLatch(1);
        WebClientOptions options = new WebClientOptions().setProtocolVersion(HttpVersion.HTTP_2).setSsl(true).setVerifyHost(false).setUseAlpn(true).setTrustStoreOptions(new JksOptions().setPassword("password").setPath(truststore));
        Uni<JsonObject> content = WebClient.create(Vertx.vertx(), options).getAbs(getAppEndpoint() + "/hello")
                .expect(ResponsePredicate.create(AbstractHttpTest::isHttp2x))
                .expect(ResponsePredicate.status(Response.Status.OK.getStatusCode()))
                .send().map(resp -> resp.bodyAsJsonObject())
                .ifNoItem().after(Duration.ofSeconds(TIMEOUT_SEC)).fail()
                .onFailure().retry().atMost(RETRY);

        content.subscribe().with(body -> {
            assertEquals(body.getString("content"),"Hello, World!");
            done.countDown();
        });

        done.await(TIMEOUT_SEC, TimeUnit.SECONDS);
        assertTrue(done.getCount() == 0);
    }

    @Test
    @DisplayName("Http/2 Client Sync test")
    @Disabled("blocked by: https://issues.redhat.com/browse/QUARKUS-658")
    public void http2ClientSync() {
        Response resp = httpVersionClientService.getClientHttpVersion();
        assertEquals(200, resp.getStatus());
        assertEquals(HttpVersion.HTTP_2.name(), resp.getHeaderString(HTTP_VERSION));
    }

    @Test
    @DisplayName("Http/2 Client Async test")
    @Disabled("blocked by: https://issues.redhat.com/browse/QUARKUS-658")
    public void http2ClientAsync() {
        Response resp = httpVersionClientServiceAsync
                .getClientHttpVersion()
                .await()
                .atMost(Duration.ofSeconds(10));

        assertEquals(200, resp.getStatus());
        assertEquals(HttpVersion.HTTP_2.name(), resp.getHeaderString(HTTP_VERSION));
    }

    @Test
    @DisplayName("Non-application endpoint move to /q/")
    public void nonAppRedirections() {
        //TODO QUARKUS-752: swagger-ui (quarkus.swagger-ui.always-include) is not supported by Native mode.
        List<String> endpoints = Arrays.asList(
                "/openapi", "/metrics/base", "/metrics/application",
                "/metrics/vendor", "/metrics", "/health/group", "/health/well", "/health/ready",
                "/health/live", "/health"
        );

        for(String endpoint: endpoints) {
            given().redirects().follow(false)
                    .expect().
                    statusCode(301).
                    header("Location", containsString( "/q" + endpoint)).
                    when().
                    get(endpoint);

            given().expect().statusCode(200).when().get(endpoint);
        }
    }

    protected String getAppEndpoint() {
        return appEndpoint.toString();
    }

    private static ResponsePredicateResult isHttp2x(HttpResponse<Void> resp) {
        return (resp.version().compareTo(HttpVersion.HTTP_2) == 0) ? ResponsePredicateResult.success() : ResponsePredicateResult.failure("Expected HTTP/2");
    }

}
