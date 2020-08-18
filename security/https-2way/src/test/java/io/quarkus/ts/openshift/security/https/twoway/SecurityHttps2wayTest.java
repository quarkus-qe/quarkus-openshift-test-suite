package io.quarkus.ts.openshift.security.https.twoway;

import io.quarkus.test.common.http.TestHTTPResource;
import io.quarkus.test.junit.QuarkusTest;
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
public class SecurityHttps2wayTest {
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
                .loadKeyMaterial(new File("target/client-keystore.pkcs12"), CLIENT_PASSWORD, CLIENT_PASSWORD)
                .loadTrustMaterial(new File("target/client-truststore.pkcs12"), CLIENT_PASSWORD)
                .build();
        try (CloseableHttpClient httpClient = HttpClients.custom()
                .setSSLContext(sslContext)
                .setSSLHostnameVerifier(new DefaultHostnameVerifier())
                .build()) {

            String response = Executor.newInstance(httpClient)
                    .execute(Request.Get(url))
                    .returnContent().asString();
            assertEquals("Hello, HTTPS: true", response);
        }
    }

    @Test
    public void https_serverCertificateUnknownToClient() throws IOException, GeneralSecurityException {
        SSLContext sslContext = SSLContexts.custom()
                .setKeyStoreType("pkcs12")
                .loadKeyMaterial(new File("target/client-keystore.pkcs12"), CLIENT_PASSWORD, CLIENT_PASSWORD)
                .build();
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
    public void https_clientCertificateUnknownToServer() throws IOException, GeneralSecurityException {
        SSLContext sslContext = SSLContexts.custom()
                .setKeyStoreType("pkcs12")
                .loadKeyMaterial(new File("target/unknown-client-keystore.pkcs12"), CLIENT_PASSWORD, CLIENT_PASSWORD)
                .loadTrustMaterial(new File("target/client-truststore.pkcs12"), CLIENT_PASSWORD)
                .build();
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
    public void https_serverCertificateUnknownToClient_clientCertificateUnknownToServer() throws IOException, GeneralSecurityException {
        SSLContext sslContext = SSLContexts.custom()
                .setKeyStoreType("pkcs12")
                .loadKeyMaterial(new File("target/unknown-client-keystore.pkcs12"), CLIENT_PASSWORD, CLIENT_PASSWORD)
                .build();
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
    public void http() throws IOException {
        String response = Request.Get(insecureUrl).execute().returnContent().asString();
        assertEquals("Hello, HTTPS: false", response);
    }
}
