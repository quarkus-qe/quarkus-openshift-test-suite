package io.quarkus.ts.openshift.http;

import io.quarkus.vertx.web.Route;
import io.vertx.core.http.HttpMethod;
import io.vertx.ext.web.RoutingContext;

import javax.enterprise.context.ApplicationScoped;
import javax.ws.rs.core.Response;

@ApplicationScoped
public class HttpClientVersionResource {

    protected static final String HTTP_VERSION = "x-http-version";

    @Route(methods = HttpMethod.GET, path = "/httpVersion")
    public void clientHttpVersion(RoutingContext rc) {
        String httpClientVersion = rc.request().version().name();
         rc.response().headers().add(HTTP_VERSION, httpClientVersion);
         rc.response().setStatusCode(Response.Status.OK.getStatusCode()).end();
    }
}
