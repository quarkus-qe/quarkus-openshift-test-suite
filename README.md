# Quarkus OpenShift Test Suite

For running the tests, it is expected that the user is logged into an OpenShift project:
- with OpenShift 4, run `oc login https://api.<cluster name>.<domain>:6443`
- with Minishift (which provides OpenShift 3.11), run something like `oc login https://192.168.99.100:8443`

To verify that you're logged into correct project, you can run `oc whoami` and `oc project`.

If you don't have a project, use `oc new-project ...` to create one.
Alternatively, see _Running tests in ephemeral namespaces_ below.

All tests will run in the project you're logged into, so it should be empty.
If there are resources deployed in the project, you should not expect they will survive.

Running the tests amounts to standard `mvn clean verify`.
If the test suite depends on Quarkus `999-SNAPSHOT` (it does currently), make sure you have built Quarkus locally prior to running the tests.

## Test Framework

The `app-metadata` and `common` directories contain a tiny framework for testing Quarkus applications on OpenShift.
This is how you use it.

Your test application needs to include the `app-metadata` dependency in the default scope:

```xml
<dependency>
    <groupId>io.quarkus.ts.openshift</groupId>
    <artifactId>app-metadata</artifactId>
</dependency>
```

> It is actually a little Quarkus extension.
> It produces a file `target/app-metadata.properties` with some data that the test framework needs to know about the application.

Your test application also needs to include the `common` dependency in the `test` scope:

```xml
<dependency>
    <groupId>io.quarkus.ts.openshift</groupId>
    <artifactId>common</artifactId>
    <scope>test</scope>
</dependency>
```

> This is the actual test framework.
> It is in fact a JUnit 5 extension.

You will also typically have dependencies on Quarkus JUnit, RestAssured and Awaitility:

```xml
<dependency>
    <groupId>io.quarkus</groupId>
    <artifactId>quarkus-junit5</artifactId>
    <scope>test</scope>
</dependency>
<dependency>
    <groupId>io.rest-assured</groupId>
    <artifactId>rest-assured</artifactId>
    <scope>test</scope>
</dependency>
<dependency>
    <groupId>org.awaitility</groupId>
    <artifactId>awaitility</artifactId>
    <scope>test</scope>
</dependency>
```

Test classes named `*Test` will be typical Quarkus tests, executed by Surefire, using Quarkus JUnit.
OpenShift tests should be named `*OpenShiftIT` and be executed by Failsafe.

> Quarkus typically usurps the `*IT` suffix for native tests.
> In my opinion, this is wrong.
> We don't have native tests here yet, but when we do, I propose it's a dedicated Failsafe execution and test classes are named `*TestNative`.
> In any case, the OpenShift tests _must_ be executed by Failsafe, because the test framework expects the application to be already built.
> (In other words, the tests must run after `mvn package`, which is when OpenShift resources for deploying the application are created.)

The test class should be annotated with `@OpenShiftTest`:

```java
@OpenShiftTest
public class HelloOpenShiftIT {
    @Test
    public void hello() {
        ...
    }
}
```

This will make sure that OpenShift resources are deployed before this test class is executed, and also undeployed after this test class is executed.
It is expected that a YAML file with a complete list of OpenShift resources to deploy the application is present in `target/kubernetes/openshift.yml`.
This is what the [Quarkus Kubernetes](https://quarkus.io/guides/kubernetes) extension (or the [Quarkus OpenShift](https://quarkus.io/guides/deploying-to-openshift) extension) does, when configured correctly.

After the application is deployed, the application's route is awaited.
If there's a readiness probe, it is used, otherwise a liveness probe is used if it exists; if there's no health probe, the root path `/` is awaited.
Only after the route responds successfully with status code 200 will the tests be allowed to run.

The `@Test` methods can use RestAssured, which is preconfigured to point to the deployed application.
When you need to wait for something to happen, use Awaitility; don't write your own wait loops with `Thread.sleep` etc.

```java
@OpenShiftTest
public class HelloOpenShiftIT {
    @Test
    public void hello() {
        // this is an EXAMPLE, waiting here isn't necessary! the test framework
        // already waits for the application to start responding
        await().atMost(1, TimeUnit.MINUTES).untilAsserted(() -> {
            when()
                    .get("/hello")
            .then()
                    .statusCode(200)
                    .body("content", is("Hello, World!"));
        });
    }
}
```

### Dependency injection

For more complex test scenarios, you can obtain some useful objects by annotating a test instance field or a test method parameter with `@TestResource`:

```java
@OpenShiftTest
public class HelloOpenShiftIT {
    @TestResource
    private OpenShiftClient oc;

    @Test
    public void hello() {
        oc.routes().list().getItems().forEach(System.out::println);

        ...
    }
}
```

The full set of objects that you can inject is:

- `OpenShiftClient`: the Fabric8 Kubernetes client, in default configuration
- `AppMetadata`: provides convenient access to data collected in `target/app-metadata.properties`
- `AwaitUtil`: utility to wait for some OpenShift resources
- `OpenShiftUtil`: utility to perform higher-level actions on some OpenShift resources
- `Config`: simple configuration utility for the test suite; currently only reads system properties

### Deploying additional resources

The test application might require additional OpenShift resources to be deployed, such as ConfigMaps or other applications.
To do that, you can use the `@AdditionalResources` annotation:

```java
@OpenShiftTest
@AdditionalResources("classpath:configmap.yaml")
public class HelloOpenShiftIT {
    ...
}
```

These resources are deployed _before_ the test application is deployed, and are also undeployed _after_ the test application is undeployed.
This annotation is `@Repeatable`, so you can include it more than once.

### Running tests in ephemeral namespaces

By default, the test framework expects that the user is logged into an OpenShift project, and that project is used for all tests.

If you start the tests with `-Dts.use-ephemeral-namespaces`, the test framework will create an ephemeral namespace for each test.
After the test is finished, the ephemeral namespace is automatically dropped.

The ephemeral namespaces are named `ts-<unique suffix>`, where the unique suffix is 10 random `a-z` characters.

### Retaining resources on failure

When the test finishes, all deployed resources are deleted.
Sometimes, that's not what you want: if the test fails, you might want all the OpenShift resources to stay intact, so that you can investigate the problem.
To do that, run the tests with `-Dts.retain-on-failure`.

This works with and without ephemeral namespaces, but note that if you're not using ephemeral namespaces, all the tests run in a single namespace.
In such case, when you enable retaining resources on test failure, it's best to only run a single test.

### Enabling/disabling tests

The `@OnlyIfConfigured` annotation can be used to selectively enable/disable execution of tests based on a configuration property.

This can be used for example to disable tests that require access to authenticated registry.
The tests that do require such access are annotated `@OnlyIfConfigured("ts.authenticated-registry")` and are not executed by default.
When executing tests against an OpenShift cluster that has configured access to the authenticated registry, you just run the tests with `-Dts.authenticated-registry` and the tests will be executed.

### TODO

There's a lot of possible improvements that haven't been implemented yet.
The most interesting probably are:

- To be able to customize where the `openshift.yml` file is loaded from.
  Or, more generally, to be able to customize the test application deployment.
  Currently, we support binary S2I builds, with the expectation that `quarkus-kubernetes` (or `quarkus-openshift`) is used.
  We could support S2I-less deployments relatively easily (would currently require Docker to be installed, but Docker-less builds are being prototyped).
  Supporting S2I source builds is more complex, as you need the application source stored in some Git repo, and when you're working on the test suite, you want your local changes, not something out there on GitHub.
- To be able to customize URL path for route awaiting.
- To be able to cleanup the project before running the test.
  Could be just a simple annotation `@CleanupProject` added on the test class.
- To be able to inject more resources automatically.
  For example, `URI` or `URL` of the application, or the OpenShift objects corresponding to the application (such as `DeploymentConfig`, `Service`, `Route`, or even `Pod` if there's only one).
- To be able to configure the connection to OpenShift cluster (that is, how `OpenShiftClient` is created).
  Currently, this isn't possible, and doesn't make too much sense, because for some actions, we just run `oc`, while for some, we use the Java client.
  We could possibly move towards doing everything through the Java library, but some things are hard (e.g. `oc start-build`).

It would also be possible to create a Kubernetes variant: `@KubernetesTest`, `kubernetes.yml`, injection of `KubernetesClient`, etc.
Most of the code could easily be shared.
The main difference is that there's no S2I in Kubernetes, so we'd have to do the S2I-less thing mentioned above.

## Existing tests

### `http`

A smoke test, really.
Verifies that you can deploy a simple HTTP endpoint to OpenShift and access it.

### `configmap`

Checks that the application can read configuration from a ConfigMap.
The ConfigMap is exposed by mounting it into the container file system.

### `sql-db`

Verifies that the application can connect to a SQL database and persist data using Hibernate ORM with Panache.
The application also uses RESTEasy to expose a RESTful API, Jackson for JSON serialization, and Hibernate Validator to validate inputs.
There are actually multiple Maven modules in the `sql-db` directory:

- `app`: the main application and the test code; no JDBC drivers (except H2 for unit test)
- `postgresql`: depends on `app` and the PostgreSQL JDBC driver; produces the PostgreSQL-specific build of the application and runs the OpenShift test with PostgreSQL
- `mysql`: same for MysQL
- `mariadb`: same for MariaDB
- `mssql`: same for MSSQL

All the tests deploy a SQL database directly into OpenShift, alongside the application.
This might not be recommended for production, but is good enough for test.
Container images used in the tests are:

- PostgreSQL:
  - version 10: `registry.access.redhat.com/rhscl/postgresql-10-rhel7`
  - version 12: `registry.redhat.io/rhscl/postgresql-12-rhel7` (only if `ts.authenticated-registry` is set)
- MySQL:
  - version 8.0: `registry.access.redhat.com/rhscl/mysql-80-rhel7`
- MariaDB:
  - version 10.2: `registry.access.redhat.com/rhscl/mariadb-102-rhel7`
  - version 10.3: `registry.redhat.io/rhscl/mariadb-103-rhel7` (only if `ts.authenticated-registry` is set)
- MSSQL: `mcr.microsoft.com/mssql/rhel/server`

### `security/basic`

Verifies the simplest way of doing authn/authz.
Authentication is HTTP `Basic`, with users/passwords/roles defined in `application.properties`.
Authorization is based on roles, restrictions are defined using common annotations (`@RolesAllowed` etc.).

### `security/jwt`

Verifies token-based authn/authz.
Authentication is MicroProfile JWT.
Authorization is based on roles, which are embedded in the token.
Restrictions are defined using common annotations (`@RolesAllowed` etc.).

### `security/https-1way`

Verifies that accessing an HTTPS endpoint is posible.
Uses a self-signed certificate generated during the build, so that the test is fully self-contained.

This test doesn't run on OpenShift (yet).
