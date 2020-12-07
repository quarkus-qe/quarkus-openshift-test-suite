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
This will use a specific Quarkus version, which can be modified by setting the `version.quarkus` property.
Alternatively, you can use `-Dquarkus-core-only` to run the test suite against Quarkus `999-SNAPSHOT`.
In that case, make sure you have built Quarkus locally prior to running the tests.

All the tests currently use the RHEL 7 OpenJDK 11 image.
This is configured in the `application.properties` file in each module.
Since this is standard Quarkus configuration, it's possible to override using a system property.
Therefore, if you want to run the tests with a different Java S2I image, run `mvn clean verify -Dquarkus.s2i.base-jvm-image=...`.

## Running against Red Hat build of Quarkus
When running against released Red Hat build of Quarkus make sure https://maven.repository.redhat.com/ga/ repository is defined in settings.xml.

Example command for released Red Hat build of Quarkus:
```
mvn -fae clean verify \
 -Dts.use-ephemeral-namespaces -Dts.authenticated-registry \
 -Dversion.quarkus=1.3.4.Final-redhat-00004 \
 -Dquarkus.platform.group-id=com.redhat.quarkus
```

Example command for not yet released version of Red Hat build of Quarkus:
```
mvn -fae clean verify \
 -Dts.use-ephemeral-namespaces -Dts.authenticated-registry \
 -Dversion.quarkus=1.7.1.Final-redhat-00001 \
 -Dquarkus.platform.group-id=com.redhat.quarkus \
 -Dmaven.repo.local=/Users/rsvoboda/Downloads/rh-quarkus-1.7.1.GA-maven-repository/maven-repository
```

## Branching Strategy
The `master` branch is always meant for latest upstream/downstream development. For each downstream major.minor version, there's a corresponding maintenance branch:

  - `1.3` for Red Hat build of Quarkus 1.3.z (corresponding upstream version: `1.3.0.Final+`)

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
- `KnativeClient`: the Fabric8 Knative client, in default configuration
- `AppMetadata`: provides convenient access to data collected in `target/app-metadata.properties`
- `AwaitUtil`: utility to wait for some OpenShift resources
- `OpenShiftUtil`: utility to perform higher-level actions on some OpenShift resources
- `Config`: simple configuration utility for the test suite; currently only reads system properties
- `URL`: URL of deployed route, application route is the default, can be adjusted using `@WithName` annotation

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

The `@DisabledOnQuarkus` annotation can be used to selectively disable execution of tests based on Quarkus version.
The Quarkus version used in the test suite is matched against a regular expression provided in the annotation, and if it matches, the test is disabled.

This can be used to disable tests that are known to fail on certain Quarkus versions.
In such case, the `reason` attribute of the annotation should point to corresponding issue (or pull request).

### Custom application deployment

If the test class is annotated `@ManualApplicationDeployment`, the `target/kubernetes/openshift.yml` file is ignored and the test application is _not_ deployed automatically.
Instead, you should use `@AdditionalResources`, `@CustomizeApplicationDeployment` and `@CustomizeApplicationUndeployment` to deploy the application manually.

This can be used to write tests that excersise an external application.
You have full control over the application deployment and undeployment process, but the rest of the test can be written as if the application was part of the test suite.
In such case, the deployed application can't use the `app-metadata` Quarkus extension, and you have to use the `@CustomAppMetadata` annotation to provide all the necessary information.

### Image overrides

It is sometimes useful to globally override certain images, for example when testing with a pre-release version of an image that is not yet available publicly.
In such case, you can set `-Dts.image-overrides` to a path of a file that looks like this:

```
registry.access.redhat.com/openjdk/openjdk-11-rhel7=registry.access.redhat.com/ubi8/openjdk-11
registry.access.redhat.com/rhscl/postgresql-10-rhel7=registry.redhat.io/rhscl/postgresql-12-rhel7
```

This looks like a `.properties` format, but it is not!
Specifically, the format is:

- empty lines and lines that begin with `#` are ignored;
- other lines must have a source image name (possibly with a tag), followed by `=`, followed by a target image name (possibly with a tag).

When a YAML file refers to the source image, it is changed to use the target image before it is deployed.
If there's no tag in the configuration of the source image, it will match all tags.

Note that this is _not_ dumb string search & replace.
We actually edit the Kubernetes resources on a few specific places (such as container definition or image stream definition), the rest is left unchanged.

This currently works automatically for the `target/kubernetes/openshift.yml` file and all the files deployed with `@AdditionalResources`.

Note that it is usually a good idea to set `-Dts.image-overrides` to a _full_ path, because Maven changes the current working directory when running tests.

### Native image

The test suite contains a Maven profile configuring required system properties for native image build. The profile is
activated by specifying the `native` Quarkus profile (`-Dquarkus.profile=native`). This profile does not specify the 
container runtime for Quarkus native build, so by default, native build will use local GraalVM. The following command 
will execute the whole test suite using Docker to run containers for native build:

```
./mvnw clean verify \
  -Dquarkus.profile=native \ 
  -Dquarkus.native.container-runtime=docker \
  -Dts.authenticated-registry
```

Currently used builder image is `quarkus/ubi-quarkus-mandrel` and the base image for OpenShift deployment is 
`quarkus/ubi-quarkus-native-binary-s2i`.

### OpenShift Serverless / Knative

The test suite contains a Maven profile activated using the `include.serverless` property or `serverless` profile name.
This profile includes additional modules with serverless test coverage into the execution of the testsuite.
Serverless test coverage supports both JVM and Native mode.

The following command will execute the whole test suite including serverless tests:

```
./mvnw clean verify -Dinclude.serverless
```

### TODO

There's a lot of possible improvements that haven't been implemented yet.
The most interesting probably are:

- Automation for some alternative deployment options.
  Currently, we support binary S2I builds, with the expectation that `quarkus-kubernetes` (or `quarkus-openshift`) is used.
  We also support completely custom application deployment, which provides most flexibility, but also requires most work.
  At the very least, we should provide some automation for S2I-less deployments with `-Dquarkus.container-image.build -Dquarkus.kubernetes.deploy`.
  Supporting S2I source builds would be more complex (you need the application source stored in some Git repo, and when you're working on the test suite, you want your local changes, not something out there on GitHub) and is probably best left to `@ManualApplicationDeployment`.
- To be able to customize URL path for route awaiting.
- To be able to cleanup the project before running the test.
  Could be just a simple annotation `@CleanupProject` added on the test class.
- To be able to inject more resources automatically.
  For example, the OpenShift objects corresponding to the application (such as `DeploymentConfig`, `Service`, `Route`, or even `Pod` if there's only one).
- To be able to configure the connection to OpenShift cluster (that is, how `OpenShiftClient` is created).
  Currently, this isn't possible, and doesn't make too much sense, because for some actions, we just run `oc`, while for some, we use the Java client.
  We could possibly move towards doing everything through the Java library, but some things are hard (e.g. `oc start-build`).
  If we ever do this, then it becomes easily possible to consisently apply image overrides everywhere.
- Implement a build workflow with `ubi8/ubi-minimal` instead of using `quarkus/ubi-quarkus-native-binary-s2i`.

It would also be possible to create a Kubernetes variant: `@KubernetesTest`, `kubernetes.yml`, injection of `KubernetesClient`, etc.
Most of the code could easily be shared.
The main difference is that there's no S2I in Kubernetes, so we'd have to do the S2I-less thing mentioned above.

## Existing tests

### `http`

A smoke test, really.
Verifies that you can deploy a simple HTTP endpoint to OpenShift and access it.

### `configmap/file-system`

Checks that the application can read configuration from a ConfigMap.
The ConfigMap is exposed by mounting it into the container file system.

### `configmap/api-server`

Checks that the application can read configuration from a ConfigMap.
The ConfigMap is obtained directly from the Kubernetes API server.

### `config-secret/file-system`

Checks that the application can read configuration from a Secret.
The Secret is exposed by mounting it into the container file system.

### `config-secret/api-server`

Checks that the application can read configuration from a Secret.
The Secret is obtained directly from the Kubernetes API server.

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

Verifies token-based authn and role-based authz.
Authentication is MicroProfile JWT, and tokens are issued manually in the test.
Authorization is based on roles, which are embedded in the token.
Restrictions are defined using common annotations (`@RolesAllowed` etc.).

### `security/keycloak`

Verifies token-based authn and role-based authz.
Authentication is OIDC, and Keycloak is used for issuing and verifying tokens.
Authorization is based on roles, which are embedded in the token.
Restrictions are defined using common annotations (`@RolesAllowed` etc.).

A simple Keycloak realm with 1 client (protected application), 2 users and 2 roles is provided in `test-realm.json`.

### `security/keycloak-authz`

Verifies token-based authn and URL-based authz.
Authentication is OIDC, and Keycloak is used for issuing and verifying tokens.
Authorization is based on URL patterns, and Keycloak is used for defining and enforcing restrictions.

A simple Keycloak realm with 1 client (protected application), 2 users, 2 roles and 2 protected resources is provided in `test-realm.json`.

### `security/https-1way`

Verifies that accessing an HTTPS endpoint is posible.
Uses a self-signed certificate generated during the build, so that the test is fully self-contained.

This test doesn't run on OpenShift (yet).

### `security/https-2way`

Verifies that accessing an HTTPS endpoint with client certificate is possible.
The client certificate is required in this test.
Uses self-signed certificates generated during the build, so that the test is fully self-contained.

This test doesn't run on OpenShift (yet).

### `security/https-2way-authz`

Verifies that accessing an HTTPS endpoint with authentication and authorization based on client certificate is possible.
Quarkus doesn't offer a declarative way to assign roles based on a client certificate, so a `SecurityIdentityAugmentor` is used.
The client certificate is _not_ required in this test.
Uses self-signed certificates generated during the build, so that the test is fully self-contained.

This test doesn't run on OpenShift (yet).

### `microprofile`

Verifies combined usage of MicroProfile RestClient, Fault Tolerance and OpenTracing.

The test hits a "client" endpoint, which uses RestClient to invoke a "hello" endpoint.
The response is then modified in the "client" endpoint and returned back to the test.
The RestClient interface uses Fault Tolerance to guard against the "hello" endpoint errors.
It is possible to enable/disable the "hello" endpoint, which controls whether Fault Tolerance is used or not.

All HTTP endpoints and internal processing is asynchronous, so Context Propagation is also required.
JAX-RS endpoints and RestClient calls are automatically traced with OpenTracing, and some additional logging into the OpenTracing spans is also done.
Jaeger is deployed in an "all-in-one" configuration, and the OpenShift test verifies the stored traces.

Note that the Fault Tolerance annotations are currently commented out, because of https://github.com/quarkusio/quarkus/issues/8650.
This is a RESTEasy bug, there is a proposed fix: https://github.com/resteasy/Resteasy/pull/2359

### `messaging/artemis`

Verifies that JMS server is up and running and Quarkus can communicate with this service.

There is a PriceProducer that pushes a new integer "price" to a JMS queue called "prices" each second.
PriceConsumer is a loop that starts at the beginning of the application runtime and blocks on reading
from the queue called "prices". Once a value is read, the attribute lastPrice is updated.
Test checks that the value gets updated.

### `messaging/artemis-jta`

Verifies that JMS server is up and running and Quarkus can communicate with this service using either transactions or client acknowledge mode.

There are three JMS queues, `custom-prices-1` and `custom-prices-2` are used to test
a transactional write: either both are correctly updated with a new value or none of them is.

`custom-prices-cack` queue is used to check that messages remains waiting in the queue until
client "acks" them, i.e. acknowledges their processing.

### `messaging/amqp-reactive`

Verifies that JMS server is up and running and Quarkus can communicate with this service.
This module is using Reactive Messaging approach by leveraging `quarkus-smallrye-reactive-messaging-amqp` extension.

There is a PriceProducer that generates message every second, the value of the message is "tick number" multiplied by 10 modulo 100.
PriceConsumer puts received number into ConcurrentLinkedQueue of Integers.
State of this queue is exposed using PriceResource which is called from the test.

### `messaging/kafka-streams-reactive-messaging`

Verifies that `Quarkus Kafka Stream` and `Quarkus SmallRye Reactive Messaging` extensions works as expected. 

There is an EventsProducer that generate login status events every 100ms. 
A Kafka stream called `WindowedLoginDeniedStream`  will aggregate these events in fixed time windows of 3 seconds. 
So if the number of wrong access excess a threshold, then a new alert event is thrown. All aggregated events(not only unauthorized) are persisted. 

### `messaging/qpid`

Verifies that JMS server is up and running and Quarkus can communicate with this service.
Similar to `messaging/artemis` scenario but using `quarkus-qpid-jms` extension to communicate with the server.

There is a PriceProducer that pushes a new integer "price" to a JMS queue called "prices" each second.
PriceConsumer is a loop that starts at the beginning of the application runtime and blocks on reading
from the queue called "prices". Once a value is read, the attribute lastPrice is updated.
Test checks that the value gets updated.

### `scaling`

An OpenShift test verifying that an OpenShift deployment with a Quarkus application scales up and down.

This test could be extended with some metric gathering.

### `external-applications/todo-demo-app`

This test produces an S2I source deployment config for OpenShift with [todo-demo-app](https://github.com/quarkusio/todo-demo-app) 
serving a simple todo checklist. The code for this application lives outside of the test suite's codebase.

The test verifies that the application with a sample of libraries is buildable and deployable via supported means.

### `external-applications/quarkus-workshop-super-heroes`

This test produces an S2I source deployment config for OpenShift with 
[Quarkus Super heroes workshop](https://github.com/quarkusio/quarkus-workshops) application.
The code for this application lives outside of the test suite's codebase.

The test verifies that the application is buildable and deployable. It also verifies that the REST and MicroProfile APIs
function properly on OpenShift, as well as the database integrations.

### `deployment-strategies/quarkus`

A smoke test for deploying the application to OpenShift via `quarkus.kubernetes.deploy`.
The test itself only verifies that a simple HTTP endpoint can be accessed.

### `deployment-strategies/quarkus-serverless`

A smoke test for deploying the application to OpenShift Serverless using combination of `quarkus.container-image.build`
and `oc apply -f target/kubernetes/knative.yml` with slightly adjusted `knative.yml` file.
The test itself only verifies that a simple HTTP endpoint can be accessed.

## Debugging failing tests

I typically debug failing tests by reproducing everything the test suite does manually.

1. `oc delete all --all` to cleanup the existing project.
   Note that this doesn't really delete _all_ resources, e.g. ConfigMaps and secrets will stay intact.
1. `mvn -pl .,app-metadata/runtime,app-metadata/deployment,common,<the failing test suite module> clean package` to rebuild the application.
1. `cd <the failing test suite module>` just for convenience.
1. If there are `@AdditionalResources`, then `oc apply -f src/test/resources/<additional resources>.yml`.
   It is usually enough to do this just once and leave the resources deployed for the entire debugging session.
   But sometimes, you'll need to undeploy them to really start over from scratch (`oc delete -f src/test/resources/<additional resources>.yml`).
1. `oc apply -f target/kubernetes/openshift.yml` to create all the Kubernetes resources of the application.
   Note that this will not deploy the application just yet -- there's no image to run.
1. `oc start-build <name of the application> --from-dir=target --follow` to upload all the artifacts to the cluster and build the image there (using binary S2I).
   This might fail if it's done too quickly after the previous command, because the OpenJDK image stream is empty for a while.
   When the image is built, application deployment will automatically proceed.
   You may want to watch the pod's log during deployment, in case the application fails to start.
   It might happen that the pod never becomes ready, because the application always fails to start.
1. OK, so now the application is deployed.

   I start with trying to hit an endpoint that the test would also use.
   That is, either `/`, or at least `/health/ready` (as all the tests have health probes), or something like that.
   I use HTTPie (`http`) on my local machine, as its output is nicer than `curl`, but `curl` is of course also fine.
   If I'm getting an error, I try hitting the endpoint from inside the pod.
   For that, I use `curl`, because it's present in almost all images.
   That can be done from the OpenShift web console, using the _Terminal_ tab, or with `oc rsh`.

   If that fails, I inspect everything that might be related.
   Again, that can be done from the OpenShift web console, or using `oc` commands.
   I look into:
   - the pod's log
   - the pod's filesystem (on the OpenJDK image, the application should be in `/deployments`)
   - the pod's environment variables (running the `env` command inside the pod)
   - the build pod's log
   - pod logs of other application, if the test uses them
   - generally everything else that's related to `@AdditionalResources`
   - the image streams
   - ConfigMaps and secrets, if the test uses them
1. In most cases, I need to undeploy the application and deploy it again several times, before I figure out what's wrong.
   For that, I do just `oc delete -f target/kubernetes/openshift.yml`.
   This leaves all the `@AdditionalResources` deployed, which is usually fine.
1. The steps above are often enough to diagnose the issue.
   Every once in a while, though, one needs to connect a debugger to the deployed application.
   This is possible, but note that it will be very slow.
   1. The application needs to accept remote debugger connection.
      To set the usual JVM arguments, add the following to your application's `application.properties`:

      ```properties
      # for OpenJDK 8
      quarkus.s2i.jvm-arguments=-Dquarkus.http.host=0.0.0.0,-Djava.util.logging.manager=org.jboss.logmanager.LogManager,-agentlib:jdwp=transport=dt_socket\\,server=y\\,suspend=n\\,address=5005
      # for OpenJDK 11
      quarkus.s2i.jvm-arguments=-Dquarkus.http.host=0.0.0.0,-Djava.util.logging.manager=org.jboss.logmanager.LogManager,-agentlib:jdwp=transport=dt_socket\\,server=y\\,suspend=n\\,address=*:5005
      ```
      Note that for other JVMs, the exact string can be different.
      Also note the escaped `,` characters.
      If you want to verify your configuration is correct, run `mvn clean package` per above and then check the `JAVA_OPTIONS` entry in `target/kubernetes/openshift.yml`.

      This configuration assumes the common binary S2I approach that is used in this test suite.
      Other deployment approaches might require using a different option.
   1. Deploy the application as usual.
   1. Find out the pod name using `oc get pods`.
   1. `oc port-forward pod/<name of the pod> 5005` to forward the debugger port from the pod to your local machine.
   1. Connect to `localhost:5005` from your IDE.

   Note that this assumes that the application starts fine.
   If the application fails to start, setting `suspend=y` can help to debug the boot process.

## Known issues

### Security https tests and Connection refused
Security https tests failing with Connection refused and stacktrace similar to this:
```
[ERROR] https_serverCertificateUnknownToClient  Time elapsed: 0.179 s  <<< FAILURE!
org.opentest4j.AssertionFailedError: Unexpected exception type thrown ==> expected: <javax.net.ssl.SSLHandshakeException> but was: <org.apache.http.conn.HttpHostConnectException>
	at io.quarkus.ts.openshift.security.https.oneway.SecurityHttps1wayTest.https_serverCertificateUnknownToClient(SecurityHttps1wayTest.java:60)
Caused by: org.apache.http.conn.HttpHostConnectException: Connect to 0.0.0.0:8444 [/0.0.0.0] failed: Connection refused (Connection refused)
	at io.quarkus.ts.openshift.security.https.oneway.SecurityHttps1wayTest.lambda$https_serverCertificateUnknownToClient$0(SecurityHttps1wayTest.java:61)
	at io.quarkus.ts.openshift.security.https.oneway.SecurityHttps1wayTest.https_serverCertificateUnknownToClient(SecurityHttps1wayTest.java:60)
Caused by: java.net.ConnectException: Connection refused (Connection refused)
	at io.quarkus.ts.openshift.security.https.oneway.SecurityHttps1wayTest.lambda$https_serverCertificateUnknownToClient$0(SecurityHttps1wayTest.java:61)
	at io.quarkus.ts.openshift.security.https.oneway.SecurityHttps1wayTest.https_serverCertificateUnknownToClient(SecurityHttps1wayTest.java:60)
```
These are local fails during `surefire` execution, this behaviour can be noticed on macOS when the client can't
connect to `0.0.0.0` and explicit address needs to be specified like in this example:
```
mvn clean test -Dquarkus.http.host=127.0.0.1
```
