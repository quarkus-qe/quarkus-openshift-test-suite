package io.quarkus.ts.openshift.http;

import io.quarkus.ts.openshift.common.injection.TestResource;
import io.quarkus.ts.openshift.http.clients.HealthClientService;
import io.quarkus.ts.openshift.http.clients.HttpVersionClientService;
import io.quarkus.ts.openshift.http.clients.HttpVersionClientServiceAsync;
import io.quarkus.ts.openshift.http.clients.RestClientServiceBuilder;
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
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import javax.ws.rs.core.Response;

import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Paths;
import java.time.Duration;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static io.quarkus.ts.openshift.http.HttpClientVersionResource.HTTP_VERSION;
import static io.restassured.RestAssured.given;
import static io.restassured.RestAssured.when;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.in;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertEquals;

public abstract class AbstractHttpTest {

    private static final int TIMEOUT_SEC = 3;
    private static final int RETRY = 3;
    private static final String PASSWORD = "password";
    private static final String KEY_STORE_PATH = "META-INF/resources/server.keystore";

    @TestResource
    private URL appEndpoint;

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
        CountDownLatch done = new CountDownLatch(1);
        Uni<JsonObject> content = WebClient.create(Vertx.vertx(), defaultVertxHttpClientOptions()).getAbs(getAppEndpoint() + "hello")
                .expect(ResponsePredicate.create(AbstractHttpTest::isHttp2x))
                .expect(ResponsePredicate.status(Response.Status.OK.getStatusCode()))
                .send().map(HttpResponse::bodyAsJsonObject)
                .ifNoItem().after(Duration.ofSeconds(TIMEOUT_SEC)).fail()
                .onFailure().retry().atMost(RETRY);

        content.subscribe().with(body -> {
            assertEquals(body.getString("content"), "Hello, World!");
            done.countDown();
        });

        done.await(TIMEOUT_SEC, TimeUnit.SECONDS);
        assertThat(done.getCount(), equalTo(0L));
    }

    @Test
    @DisplayName("Http/2 Client Sync test")
    @Disabled("blocked by: https://issues.redhat.com/browse/QUARKUS-658")
    public void http2ClientSync() throws Exception {
        HttpVersionClientService versionHttpClient = new RestClientServiceBuilder<HttpVersionClientService>(getAppEndpoint())
                .withHostVerified(true)
                .withPassword(PASSWORD)
                .withKeyStorePath(KEY_STORE_PATH)
                .build(HttpVersionClientService.class);

        Response resp = versionHttpClient.getClientHttpVersion();
        assertEquals(200, resp.getStatus());
        assertEquals(HttpVersion.HTTP_2.name(), resp.getHeaderString(HTTP_VERSION));
    }

    @Test
    @DisplayName("Http/2 Client Async test")
    @Disabled("blocked by: https://issues.redhat.com/browse/QUARKUS-658")
    public void http2ClientAsync() throws Exception {
        HttpVersionClientServiceAsync clientServiceAsync = new RestClientServiceBuilder<HttpVersionClientServiceAsync>(getAppEndpoint())
                .withHostVerified(true)
                .withPassword(PASSWORD)
                .withKeyStorePath(KEY_STORE_PATH)
                .build(HttpVersionClientServiceAsync.class);

        Response resp = clientServiceAsync
                .getClientHttpVersion()
                .await()
                .atMost(Duration.ofSeconds(10));

        assertEquals(200, resp.getStatus());
        assertEquals(HttpVersion.HTTP_2.name(), resp.getHeaderString(HTTP_VERSION));
    }

    @Test
    @DisplayName("Non-application endpoint move to /q/")
    public void nonAppRedirections() {
        List<String> endpoints = Arrays.asList(
                "/openapi", "/swagger-ui", "/metrics/base", "/metrics/application",
                "/metrics/vendor", "/metrics", "/health/group", "/health/well", "/health/ready",
                "/health/live", "/health"
        );

        for (String endpoint : endpoints) {
            given().redirects().follow(false)
                    .log().uri()
                    .expect().
                    statusCode(301).
                    header("Location", containsString("/q" + endpoint)).
                    when().
                    get(endpoint);

            given().expect().statusCode(in(Arrays.asList(200, 204))).when().get(endpoint);
        }
    }

    @Test
    @Disabled("blocked by: https://issues.redhat.com/browse/QUARKUS-781")
    public void microprofileHttpClientRedirection() throws Exception {
        HealthClientService healthHttpClient = new RestClientServiceBuilder<HealthClientService>(getAppEndpoint())
                .withHostVerified(true)
                .withPassword(PASSWORD)
                .withKeyStorePath(KEY_STORE_PATH)
                .build(HealthClientService.class);

        assertThat(200, equalTo(healthHttpClient.health().getStatus()));
    }

    @Test
    public void vertxHttpClientRedirection() throws InterruptedException, URISyntaxException {
        CountDownLatch done = new CountDownLatch(1);
        Uni<Integer> statusCode = WebClient.create(Vertx.vertx(), defaultVertxHttpClientOptions()).getAbs(getAppEndpoint() + "health")
                .send().map(HttpResponse::statusCode)
                .ifNoItem().after(Duration.ofSeconds(TIMEOUT_SEC)).fail()
                .onFailure().retry().atMost(RETRY);

        statusCode.subscribe().with(httpStatusCode -> {
            assertEquals(200, httpStatusCode);
            done.countDown();
        });

        done.await(TIMEOUT_SEC, TimeUnit.SECONDS);
        assertThat(done.getCount(), equalTo(0L));
    }

    protected String getAppEndpoint() {
        return appEndpoint.toString();
    }

    private static ResponsePredicateResult isHttp2x(HttpResponse<Void> resp) {
        return (resp.version().compareTo(HttpVersion.HTTP_2) == 0) ? ResponsePredicateResult.success() : ResponsePredicateResult.failure("Expected HTTP/2");
    }

    private WebClientOptions defaultVertxHttpClientOptions() throws URISyntaxException {
        return new WebClientOptions().setProtocolVersion(HttpVersion.HTTP_2)
                .setSsl(true)
                .setVerifyHost(false)
                .setUseAlpn(true)
                .setTrustStoreOptions(new JksOptions().setPassword(PASSWORD).setPath(defaultTruststore()));
    }

    private String defaultTruststore() throws URISyntaxException {
        URL res = getClass().getClassLoader().getResource(KEY_STORE_PATH);
        return Paths.get(res.toURI()).toFile().getAbsolutePath();
    }
}
