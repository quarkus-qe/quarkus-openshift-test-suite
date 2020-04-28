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
import javax.net.ssl.SSLException;
import javax.net.ssl.SSLHandshakeException;

import java.io.File;
import java.io.IOException;
import java.security.GeneralSecurityException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

@QuarkusTest
public class SecurityHttps2wayTest {
    // not using RestAssured because we want 100% control over certificate & hostname verification

    @TestHTTPResource(value = "/hello", ssl = true)
    private String url;

    @TestHTTPResource(value = "/hello")
    private String insecureUrl;

    @Test
    public void https() throws IOException, GeneralSecurityException {
        char[] clientPassword = "client-password".toCharArray();
        SSLContext sslContext = SSLContexts.custom()
                .setKeyStoreType("pkcs12")
                .loadKeyMaterial(new File("target/client-keystore.pkcs12"), clientPassword, clientPassword)
                .loadTrustMaterial(new File("target/client-truststore.pkcs12"), "client-password".toCharArray())
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
    public void https_serverCertificateUnknownToClient() throws IOException, GeneralSecurityException {
        char[] clientPassword = "client-password".toCharArray();
        SSLContext sslContext = SSLContexts.custom()
                .setKeyStoreType("pkcs12")
                .loadKeyMaterial(new File("target/client-keystore.pkcs12"), clientPassword, clientPassword)
                .build();
        try (CloseableHttpClient httpClient = HttpClients.custom()
                .setSSLContext(sslContext)
                .setSSLHostnameVerifier(new DefaultHostnameVerifier())
                .build()) {

            assertThrows(SSLHandshakeException.class, () -> {
                Executor.newInstance(httpClient).execute(Request.Get(url));
            });
        }
    }

    @Test
    public void https_clientCertificateUnknownToServer() throws IOException, GeneralSecurityException {
        char[] clientPassword = "client-password".toCharArray();
        SSLContext sslContext = SSLContexts.custom()
                .setKeyStoreType("pkcs12")
                .loadKeyMaterial(new File("target/unknown-client-keystore.pkcs12"), clientPassword, clientPassword)
                .loadTrustMaterial(new File("target/client-truststore.pkcs12"), clientPassword)
                .build();
        try (CloseableHttpClient httpClient = HttpClients.custom()
                .setSSLContext(sslContext)
                .setSSLHostnameVerifier(new DefaultHostnameVerifier())
                .build()) {

            assertThrows(SSLException.class, () -> {
                Executor.newInstance(httpClient).execute(Request.Get(url));
            });
        }
    }

    @Test
    public void https_serverCertificateUnknownToClient_clientCertificateUnknownToServer() throws IOException, GeneralSecurityException {
        char[] clientPassword = "client-password".toCharArray();
        SSLContext sslContext = SSLContexts.custom()
                .setKeyStoreType("pkcs12")
                .loadKeyMaterial(new File("target/unknown-client-keystore.pkcs12"), clientPassword, clientPassword)
                .build();
        try (CloseableHttpClient httpClient = HttpClients.custom()
                .setSSLContext(sslContext)
                .setSSLHostnameVerifier(new DefaultHostnameVerifier())
                .build()) {

            assertThrows(SSLException.class, () -> {
                Executor.newInstance(httpClient).execute(Request.Get(url));
            });
        }
    }
}
