package io.quarkus.ts.openshift.http;

import io.quarkus.vertx.web.Route;
import io.vertx.ext.web.RoutingContext;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.ws.rs.core.Response;

@ApplicationScoped
public class HttpClientVersionResource {

    protected static final String HTTP_VERSION = "x-http-version";

    @Route(methods = Route.HttpMethod.GET, path = "/httpVersion")
    public void clientHttpVersion(RoutingContext rc) {
        String httpClientVersion = rc.request().version().name();
        rc.response().headers().add(HTTP_VERSION, httpClientVersion);
        rc.response().setStatusCode(Response.Status.OK.getStatusCode()).end();
    }
}
