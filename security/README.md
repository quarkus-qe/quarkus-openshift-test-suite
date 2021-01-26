# Configure Quarkus OpenShift Security Test Suite

## Using Keycloak Test Resource for Quarkus Tests

In order to use a Keycloak test resource for running the Quarkus Tests, we need to use the `KeycloakQuarkusTestResource` test resource in our Quarkus test:

```java
@QuarkusTest
@QuarkusTestResource(KeycloakQuarkusTestResource.class)
public class MyQuarkusTest {

    @Test
    public void test_XXX() throws Exception {
        // ...
    }
    
}
```

The keycloak test resource is deployed by the [Testcontainers](https://www.testcontainers.org/) framework, so we'll need to add this dependency into the `pom.xml`:

```xml
<dependency>
    <groupId>org.testcontainers</groupId>
    <artifactId>testcontainers</artifactId>
    <scope>test</scope>
</dependency>
```

Also, we'll need to configure the `keycloak-realm.json` in the test classpath of our module. See the next realm configuration as an example:

```json
{
  "realm": "test-realm",
  "enabled": true,
  "sslRequired": "none",
  "roles": {
    "realm": [
      {
        "name": "test-user-role"
      }
    ]
  },
  "users": [
    {
      "username": "test-user",
      "enabled": true,
      "credentials": [
        {
          "type": "password",
          "value": "test-user"
        }
      ],
      "realmRoles": [
        "test-user-role"
      ]
    }
  ],
  "clients": [
    {
      "clientId": "test-application-client",
      "enabled": true,
      "protocol": "openid-connect",
      "standardFlowEnabled": true,
      "implicitFlowEnabled": false,
      "directAccessGrantsEnabled": true,
      "clientAuthenticatorType": "client-secret",
      "secret": "test-application-client-secret",
      "redirectUris": [
        "*"
      ]
    }
  ]
}
```

Finally, the Keycloak test resource can be used to auto-configure the Quarkus application:

- Using `KeycloakQuarkusTestResource.WithOidcConfig` to configure the OIDC Auth Url property.
- Using `KeycloakQuarkusTestResource.WithOidcAndTokenIssuerConfig` to configure the OIDC Auth Url and the Token Issuer properties.
- Using `KeycloakQuarkusTestResource.WithOAuth2Config` to configure the OAuth 2 Introspection Url.