package io.quarkus.ts.openshift.microprofile;

import static io.restassured.RestAssured.when;
import static org.awaitility.Awaitility.await;
import static org.hamcrest.Matchers.hasItems;
import static org.hamcrest.Matchers.hasSize;

import java.net.URL;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;

import io.quarkus.ts.openshift.common.AdditionalResources;
import io.quarkus.ts.openshift.common.DisabledOnQuarkus;
import io.quarkus.ts.openshift.common.OpenShiftTest;
import io.quarkus.ts.openshift.common.injection.TestResource;
import io.quarkus.ts.openshift.common.injection.WithName;

@OpenShiftTest
@AdditionalResources("classpath:jaeger-all-in-one-template.yml")
@AdditionalResources("classpath:jaeger-route.yaml")
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@DisabledOnQuarkus(version = "1\\.3\\..*", reason = "https://github.com/quarkusio/quarkus/pull/7987")
public class MicroProfileOpenShiftIT extends AbstractMicroProfileTest {
    @TestResource
    @WithName("jaeger-query")
    private URL jaegerUrl;

    @TestResource
    private URL applicationUrl;

    @Test
    @Order(5) // see superclass
    public void verifyTracesInJaeger() {

        // the tracer inside the application doesn't send traces to the Jaeger server immediately,
        // they are batched, so we need to wait a bit
        await().atMost(30, TimeUnit.SECONDS).untilAsserted(() -> {
            when()
                    .get(jaegerUrl + "/api/traces?service=test-traced-service")
                    .then()
                    .statusCode(200)
                    .log().body()
                    .log().status()
                    .body("data", hasSize(1))
                    .body("data[0].spans", hasSize(3))
                    .body("data[0].spans.operationName", hasItems(
                            "GET:io.quarkus.ts.openshift.microprofile.ClientResource.get",
                            "GET",
                            "GET:io.quarkus.ts.openshift.microprofile.HelloResource.get"))
                    .body("data[0].spans.logs.fields.value.flatten()", hasItems(
                            "ClientResource called",
                            "HelloResource called",
                            "HelloService called",
                            "HelloService async processing"))
                    .body("data[0].spans.find { it.operationName == 'GET:io.quarkus.ts.openshift.microprofile.ClientResource.get' }.tags.collect { \"${it.key}=${it.value}\".toString() }",
                            hasItems(
                                    "span.kind=server",
                                    "component=jaxrs",
                                    "http.url=" + applicationUrl + "/client",
                                    "http.method=GET",
                                    "http.status_code=200"))
                    .body("data[0].spans.find { it.operationName == 'GET' }.tags.collect { \"${it.key}=${it.value}\".toString() }",
                            hasItems(
                                    "span.kind=client",
                                    "component=jaxrs",
                                    "http.url=http://microprofile-test:8080/hello",
                                    "http.method=GET",
                                    "http.status_code=200"))
                    .body("data[0].spans.find { it.operationName == 'GET:io.quarkus.ts.openshift.microprofile.HelloResource.get' }.tags.collect { \"${it.key}=${it.value}\".toString() }",
                            hasItems(
                                    "span.kind=server",
                                    "component=jaxrs",
                                    "http.url=http://microprofile-test:8080/hello",
                                    "http.method=GET",
                                    "http.status_code=200"));
        });
    }
}
