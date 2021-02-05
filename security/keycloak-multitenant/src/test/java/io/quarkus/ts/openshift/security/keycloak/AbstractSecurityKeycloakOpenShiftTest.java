package io.quarkus.ts.openshift.security.keycloak;

import com.gargoylesoftware.htmlunit.SilentCssErrorHandler;
import com.gargoylesoftware.htmlunit.TextPage;
import com.gargoylesoftware.htmlunit.WebClient;
import com.gargoylesoftware.htmlunit.WebRequest;
import com.gargoylesoftware.htmlunit.html.HtmlForm;
import com.gargoylesoftware.htmlunit.html.HtmlPage;
import org.apache.commons.lang3.StringUtils;
import org.apache.http.impl.client.HttpClients;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.keycloak.authorization.client.AuthzClient;
import org.keycloak.authorization.client.Configuration;

import java.net.URI;
import java.util.Collections;
import java.util.Optional;

import static io.restassured.RestAssured.given;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public abstract class AbstractSecurityKeycloakOpenShiftTest {

    private static final String USER = "test-user";
    private static final String REALM = "test-realm";

    protected abstract String getAuthServerUrl();

    protected abstract String getAppUrl();

    private WebClient webClient;

    @BeforeEach
    public void setup() {
        webClient = new WebClient();
        webClient.setCssErrorHandler(new SilentCssErrorHandler());
        webClient.getOptions().setRedirectEnabled(true);
    }

    @AfterEach
    public void tearDown() {
        Optional.ofNullable(webClient).ifPresent(WebClient::close);
    }

    @ParameterizedTest
    @EnumSource(value = Tenant.class, names = { "WEBAPP", "JWT" })
    public void testLocationParamsForWebAppTenants(Tenant webAppTenant) throws Exception {
        webClient.getOptions().setRedirectEnabled(false);
        String loc = webClient.loadWebResponse(new WebRequest(URI.create(getEndpointByTenant(webAppTenant)).toURL()))
                .getResponseHeaderValue("location");

        assertTrue(loc.startsWith(getAuthServerUrl()), "Unexpected location for " + getAuthServerUrl() + ". Got: " + loc);
        assertTrue(loc.contains("scope=openid"), "Unexpected scope. Got: " + loc);
        assertTrue(loc.contains("response_type=code"), "Unexpected response type. Got: " + loc);
        assertTrue(loc.contains("client_id=" + webAppTenant.getClientId()),
                "Unexpected client id for " + webAppTenant.getClientId() + " . Got: " + loc);
    }

    @ParameterizedTest
    @EnumSource(value = Tenant.class, names = { "WEBAPP", "JWT" })
    public void testAuthenticationForWebAppTenants(Tenant webAppTenant) throws Exception {
        HtmlPage loginPage = webClient.getPage(getEndpointByTenant(webAppTenant));
        assertEquals("Log in to " + REALM, loginPage.getTitleText(), "Login page title should display application realm");

        TextPage resourcePage = whenLogin(loginPage, USER);

        assertEndpointMessage(webAppTenant, resourcePage.getContent());
    }

    @Test
    public void testEndpointNeedsAuthenticationForServiceTenant() {
        given().when().get(getEndpointByTenant(Tenant.SERVICE))
                .then().statusCode(401);
    }

    @Test
    public void testAuthenticationForServiceTenant() {
        Tenant serviceTenant = Tenant.SERVICE;

        String actualResponse = given()
                .when().auth().oauth2(getAccessToken(serviceTenant))
                .get(getEndpointByTenant(serviceTenant))
                .then().statusCode(200)
                .extract().asString();

        assertEndpointMessage(serviceTenant, actualResponse);
    }

    @Test
    public void testSameTokenShouldBeValidForTenantsUsingSameRealm() throws Exception {
        // When login using WebApp tenant
        HtmlPage loginPage = webClient.getPage(getEndpointByTenant(Tenant.WEBAPP));
        TextPage webAppPage = whenLogin(loginPage, USER);

        assertEndpointMessage(Tenant.WEBAPP, webAppPage.getContent());

        // Then JWT tenant should not request a new login
        TextPage jwtPage = webClient.getPage(getEndpointByTenant(Tenant.JWT));
        assertEndpointMessage(Tenant.JWT, jwtPage.getContent());
    }

    private TextPage whenLogin(HtmlPage loginPage, String user) throws Exception {
        HtmlForm loginForm = loginPage.getForms().get(0);

        loginForm.getInputByName("username").setValueAttribute(USER);
        loginForm.getInputByName("password").setValueAttribute(USER);
        return loginForm.getInputByName("login").click();
    }

    private void assertEndpointMessage(Tenant tenant, String actualResponse) {
        assertEquals("Hello, user " + USER + " using tenant " + tenant.getValue(), actualResponse);
    }

    private String getAccessToken(Tenant serviceTenant) {
        AuthzClient authzClient = AuthzClient.create(new Configuration(
                StringUtils.substringBefore(getAuthServerUrl(), "/realms"),
                REALM,
                serviceTenant.getClientId(),
                Collections.singletonMap("secret", serviceTenant.getClientSecret()),
                HttpClients.createDefault()));

        return authzClient.obtainAccessToken(USER, USER).getToken();
    }

    private String getEndpointByTenant(Tenant tenant) {
        return getAppUrl() + "/user/" + tenant.getValue();
    }
}
