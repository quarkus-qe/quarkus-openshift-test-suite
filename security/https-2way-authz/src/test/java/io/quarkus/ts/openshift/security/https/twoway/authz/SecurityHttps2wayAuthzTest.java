package io.quarkus.ts.openshift.security.https.twoway.authz;

import io.quarkus.test.common.http.TestHTTPResource;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.ts.openshift.common.DisabledOnQuarkus;
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

import static io.quarkus.ts.openshift.common.util.HttpsAssertions.assertTls13OnlyHandshakeError;
import static io.quarkus.ts.openshift.common.util.HttpsAssertions.assertTlsHandshakeError;
import static org.junit.jupiter.api.Assertions.assertEquals;

@QuarkusTest
@DisabledOnQuarkus(version = "1\\.[345]\\..*", reason = "https://github.com/quarkusio/quarkus/pull/8991")
public class SecurityHttps2wayAuthzTest {
    // not using RestAssured because we want 100% control over certificate & hostname verification

    private static final char[] CLIENT_PASSWORD = "client-password".toCharArray();

    @TestHTTPResource(value = "/", ssl = true)
    private String url;

    @TestHTTPResource(value = "/secured", ssl = true)
    private String urlWithAuthz;

    @TestHTTPResource(value = "/")
    private String insecureUrl;

    @Test
    public void https_authenticatedAndAuthorizedClient() throws IOException, GeneralSecurityException {
        SSLContext sslContext = SSLContexts.custom()
                .setKeyStoreType("pkcs12")
                .loadKeyMaterial(new File("target/client-keystore.pkcs12"), CLIENT_PASSWORD, CLIENT_PASSWORD)
                .loadTrustMaterial(new File("target/client-truststore.pkcs12"), CLIENT_PASSWORD)
                .build();
        try (CloseableHttpClient httpClient = HttpClients.custom()
                .setSSLContext(sslContext)
                .setSSLHostnameVerifier(new DefaultHostnameVerifier())
                .build()) {

            Executor executor = Executor.newInstance(httpClient);

            {
                String response = executor.execute(Request.Get(url)).returnContent().asString();
                assertEquals("Hello CN=client, HTTPS: true, isUser: true, isGuest: false", response);
            }

            {
                String response = executor.execute(Request.Get(urlWithAuthz)).returnContent().asString();
                assertEquals("Client certificate: CN=client", response);
            }
        }
    }

    @Test
    public void https_authenticatedButUnauthorizedClient() throws IOException, GeneralSecurityException {
        SSLContext sslContext = SSLContexts.custom()
                .setKeyStoreType("pkcs12")
                .loadKeyMaterial(new File("target/guest-client-keystore.pkcs12"), CLIENT_PASSWORD, CLIENT_PASSWORD)
                .loadTrustMaterial(new File("target/client-truststore.pkcs12"), CLIENT_PASSWORD)
                .build();
        try (CloseableHttpClient httpClient = HttpClients.custom()
                .setSSLContext(sslContext)
                .setSSLHostnameVerifier(new DefaultHostnameVerifier())
                .build()) {

            Executor executor = Executor.newInstance(httpClient);

            {
                String response = executor.execute(Request.Get(url)).returnContent().asString();
                assertEquals("Hello CN=guest-client, HTTPS: true, isUser: false, isGuest: true", response);
            }

            {
                HttpResponse response = executor.execute(Request.Get(urlWithAuthz)).returnResponse();
                assertEquals(403, response.getStatusLine().getStatusCode());
            }
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

            Executor executor = Executor.newInstance(httpClient);

            assertTls13OnlyHandshakeError(() -> {
                String response = executor.execute(Request.Get(url)).returnContent().asString();
                assertEquals("Hello <anonymous>, HTTPS: true, isUser: false, isGuest: false", response);
            });

            assertTls13OnlyHandshakeError(() -> {
                HttpResponse response = executor.execute(Request.Get(urlWithAuthz)).returnResponse();
                assertEquals(403, response.getStatusLine().getStatusCode());
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
        assertEquals("Hello <anonymous>, HTTPS: false, isUser: false, isGuest: false", response);
    }
}
