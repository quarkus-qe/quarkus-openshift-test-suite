package io.quarkus.ts.openshift.security.https.oneway;

import io.quarkus.test.common.http.TestHTTPResource;
import io.quarkus.test.junit.QuarkusTest;
import org.apache.http.HttpResponse;
import org.apache.http.client.fluent.Executor;
import org.apache.http.client.fluent.Request;
import org.apache.http.conn.ssl.DefaultHostnameVerifier;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.ssl.SSLContexts;
import org.junit.jupiter.api.Test;

import javax.net.ssl.SSLContext;

import java.io.File;
import java.io.IOException;
import java.security.GeneralSecurityException;

import static io.quarkus.ts.openshift.common.util.HttpsAssertions.assertTlsHandshakeError;
import static org.junit.jupiter.api.Assertions.assertEquals;

@QuarkusTest
public class SecurityHttps1wayTest {
    // not using RestAssured because we want 100% control over certificate & hostname verification

    private static final char[] CLIENT_PASSWORD = "client-password".toCharArray();

    @TestHTTPResource(value = "/hello", ssl = true)
    private String url;

    @TestHTTPResource(value = "/hello")
    private String insecureUrl;

    @Test
    public void https() throws IOException, GeneralSecurityException {
        SSLContext sslContext = SSLContexts.custom()
                .setKeyStoreType("pkcs12")
                .loadTrustMaterial(new File("target/client-truststore.pkcs12"), CLIENT_PASSWORD)
                .build();
        try (CloseableHttpClient httpClient = HttpClients.custom()
                .setSSLContext(sslContext)
                .setSSLHostnameVerifier(new DefaultHostnameVerifier())
                .build()) {

            String response = Executor.newInstance(httpClient)
                    .execute(Request.Get(url))
                    .returnContent().asString();
            assertEquals("Hello, world!", response);
        }
    }

    @Test
    public void https_serverCertificateUnknownToClient() throws IOException {
        SSLContext sslContext = SSLContexts.createDefault();
        try (CloseableHttpClient httpClient = HttpClients.custom()
                .setSSLContext(sslContext)
                .setSSLHostnameVerifier(new DefaultHostnameVerifier())
                .build()) {

            assertTlsHandshakeError(() -> {
                Executor.newInstance(httpClient).execute(Request.Get(url));
            });
        }
    }

    @Test
    public void http_redirect() throws IOException {
        try (CloseableHttpClient httpClient = HttpClients.custom()
                .disableRedirectHandling()
                .build()) {

            HttpResponse response = Executor.newInstance(httpClient)
                    .execute(Request.Get(insecureUrl))
                    .returnResponse();
            assertEquals(301, response.getStatusLine().getStatusCode());
            assertEquals(url, response.getFirstHeader("Location").getValue());
        }
    }
}
