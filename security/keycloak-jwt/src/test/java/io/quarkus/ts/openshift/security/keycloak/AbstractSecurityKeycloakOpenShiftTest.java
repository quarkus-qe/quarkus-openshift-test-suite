package io.quarkus.ts.openshift.security.keycloak;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.Optional;

import org.apache.http.HttpStatus;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.gargoylesoftware.htmlunit.FailingHttpStatusCodeException;
import com.gargoylesoftware.htmlunit.SilentCssErrorHandler;
import com.gargoylesoftware.htmlunit.WebClient;
import com.gargoylesoftware.htmlunit.html.HtmlForm;
import com.gargoylesoftware.htmlunit.html.HtmlPage;

public abstract class AbstractSecurityKeycloakOpenShiftTest {

    private static final String REALM = "test-realm";

    protected abstract String getAuthServerUrl();

    protected abstract String getAppUrl();

    private WebClient webClient;
    private HtmlPage page;

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

    @Test
    public void secured_asAdmin_everyoneEndpoint() throws Exception {
        whenGoTo("/secured/everyone");
        thenRedirectToLoginPage();

        whenLoginAs("test-admin");
        thenPageReturns("Hello, test-admin, your token was issued by " + getAuthServerUrl());
    }

    @Test
    public void secured_asAdmin_adminEndpoint() throws Exception {
        whenGoTo("/secured/admin");
        thenRedirectToLoginPage();

        whenLoginAs("test-admin");
        thenPageReturns("Restricted area! Admin access granted!");
    }

    @Test
    public void secured_asUser_everyoneEndpoint() throws Exception {
        whenGoTo("/secured/everyone");
        thenRedirectToLoginPage();

        whenLoginAs("test-user");
        thenPageReturns("Hello, test-user, your token was issued by " + getAuthServerUrl());
    }

    @Test
    public void secured_asUser_adminEndpoint() throws Exception {
        whenGoTo("/secured/admin");
        thenRedirectToLoginPage();

        thenReturnsForbiddenWhenLoginAs("test-user");
    }

    private void whenLoginAs(String user) throws Exception {
        HtmlForm loginForm = page.getForms().get(0);

        loginForm.getInputByName("username").setValueAttribute(user);
        loginForm.getInputByName("password").setValueAttribute(user);

        page = loginForm.getInputByName("login").click();
    }

    private void whenGoTo(String path) throws Exception {
        page = webClient.getPage(getAppUrl() + path);
    }

    private void thenRedirectToLoginPage() {
        assertEquals("Log in to " + REALM, page.getTitleText(),
                "Login page title should display application realm");
    }

    private void thenPageReturns(String expectedMessage) {
        assertEquals(expectedMessage, page.asNormalizedText(), "Page content should match with expected content");
    }

    private void thenReturnsForbiddenWhenLoginAs(String user) {
        FailingHttpStatusCodeException exception = assertThrows(FailingHttpStatusCodeException.class,
                () -> whenLoginAs(user), "Should return HTTP status exception");
        assertEquals(HttpStatus.SC_FORBIDDEN, exception.getStatusCode());
    }
}
