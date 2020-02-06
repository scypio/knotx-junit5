[![Build Status](https://dev.azure.com/knotx/Knotx/_apis/build/status/Knotx.knotx-junit5?branchName=master)](https://dev.azure.com/knotx/Knotx/_build/latest?definitionId=6&branchName=master)
[![CodeFactor](https://www.codefactor.io/repository/github/knotx/knotx-junit5/badge)](https://www.codefactor.io/repository/github/knotx/knotx-junit5)
[![codecov](https://codecov.io/gh/Knotx/knotx-junit5/branch/master/graph/badge.svg)](https://codecov.io/gh/Knotx/knotx-junit5)
[![Gradle Status](https://gradleupdate.appspot.com/Knotx/knotx-commons/status.svg)](https://gradleupdate.appspot.com/Knotx/knotx-commons/status)

# Knot.x JUnit5
JUnit 5 extensions and data type converters for Knot.x integration tests. Those tests allow to setup
Knot.x instance with declared modules. It can be used both for module tests and regression tests.

## Extensions
Provides following helpers:

### KnotxExtension

Knot.x-specific extension that manages test Vert.x instances (with Knot.x configuration injection) 
and WireMock servers (through KnotxWiremockExtension).

**Example usage:**

```java
@ExtendWith(KnotxExtension.class)
public class ExampleIntegrationTest {

  @ClasspathResourcesMockServer(port = 4001)
  protected WireMockServer mockRepository;

  @Test
  @KnotxApplyConfiguration({"default.conf", "overloaded.conf"})
  public void callModule_validKnotContextResult(VertxTestContext context, Vertx vertx) {
    // ...
  }
}
```
See more examples in [`ExampleKnotxJUnit5Test`](https://github.com/Knotx/knotx-junit5/blob/master/src/test/java/io/knotx/junit5/examples/ExampleKnotxJUnit5Test.java).

See [Vert.x JUnit 5 integration](https://vertx.io/docs/vertx-junit5/java/) for guide
how to interact with `VertxTestContext` instances. Also, under `io.knotx.junit5.util.RequestUtil`
there are some common methods that should make working with Vert.x tests contexts easier.

#### @KnotxApplyConfiguration

The annotation allows to specify one and more Knot.x configuration(s) to load for given test.
It accepts a paths array and loads all configuration entries using 
[Vert.x Config](https://vertx.io/docs/vertx-config/java/) file stores, through a custom 
implementation enhanced with support for HOCON hierarchical configuration loading and cross-file
variable references (for more details see [KnotxConcatConfigProcessor](docs/CROSS_FILES_CONFIGURATION.md) reference). 
It supports two configuration semantics:

- JSON (files with `json` extension)
- HOCON (files with `conf` extension)

The order of the configuration files is important, as it defines the overloading order 
(base file first, overrides last).
For conflicting (identical) keys in configuration, configuration entries arriving last 
will overwrite the value provided by the previous configuration files.

See [Vert.x Config overloading rules](https://vertx.io/docs/vertx-config/java/#_overloading_rules) 
for more details.

KnotxApplyConfiguration annotation can be placed on class, method, and parameter level. As such, configuration for
parameter level will override method and class level, and method will override class level. For quick example see
test package namespace `io.knotx.junit5.example`.


#### @RandomPort

If you want to randomize a port for using inside your test, you can define a namespace inside your HOCON config:

```hocon
test {
  # random values generation section
  random {
    # all <name>.port entries will be substituted for different random ports
    globalServer.port = 12345
    actionAdapterService.port = 12345
  }
}
```
(example taken from [`example_random_config.conf`](https://github.com/Knotx/knotx-junit5/blob/master/src/test/resources/example_random_config.conf) file)

Then you can reference such variables from anywhere inside your configs; placeholder values will be substituted for real available
port numbers:
```java
@Test
@KnotxApplyConfiguration("config/example_random_config.conf")
public void injectRandomizedPort(@RandomPort Integer globalServerPort) {
  // integer parameter will be filled with generated port from section 'random' for entry 'globalServer'
}
```

The working example is defined in `io.knotx.junit5.examples.ExampleKnotxJUnit5Test#injectRandomizedPort`
method from test classes.

### KnotxWiremockExtension
Standalone WireMockServer injection and lifecycle management. Allows for:
 
- Specifying on which port WireMockServer instance should be present,
- If no port is specified on annotation, a random one will be assigned to given instance,
- Running multiple mocked server instances,
- Referencing mocked servers' port numbers in Knot.x configuration (more details below).

***Warning:*** if you use KnotxExtension, you **must not inject** KnotxWiremockExtension, 
as the functionality of the latter gets auto-imported into the former.

#### ClasspathResourcesMockServer injection and naming

WireMockServer instances are recognized by their identifier - either test class instance variable name 
or test method parameter name.

For example below, two servers will be available for `testWiremockRunningOnPort` method: `mockServiceRandom` 
with randomly assigned port, and `server` on port `3000`.

```java
@ExtendWith(KnotxWiremockExtension.class)
public class WireMockTest {

  private static final int MOCK_SERVICE_PORT_NUMBER = 3000;

  @ClasspathResourcesMockServer // will be started on a random port
  private WireMockServer mockServiceRandom;

  @Test
  public void testWiremockRunningOnPort(
      @ClasspathResourcesMockServer(port = MOCK_SERVICE_PORT_NUMBER) WireMockServer server)
      throws IOException, URISyntaxException {
    // ...
  }
}
```

**One exception applies:** one WireMockServer instance will be created per identifier within test given class:

```java
@ExtendWith(KnotxWiremockExtension.class)
public class WireMockTest {

  @ClasspathResourcesMockServer
  private WireMockServer mockServiceRandom;

  @Test
  public void testWiremockEquality(
      @ClasspathResourcesMockServer WireMockServer mockServiceRandom) {
    assertTrue(this.mockServiceRandom == mockServiceRandom);
  }
}
```

Only one WireMockServer instance with random port is created (`mockServiceRandom`) 
and injected both into instance field and method parameter.

#### Referencing WireMockServer ports in Knot.x configuration

With KnotxExtension, created WireMockServer instances' ports will be available 
for referencing in Knot.x configuration under `test.wiremock.<wiremockserver_identifier>.port` variables
(HOCON syntax only).

## How to configure?

First we need to add Knot.x Junit5 to dependencies. We can get the module version from 
[Knot.x Dependencies](https://github.com/Knotx/knotx-dependencies).
```
dependencies {
  implementation(platform("io.knotx:knotx-dependencies:${project.version}"))
  testImplementation(group = "io.knotx", name = "knotx-junit5")
  testImplementation(group = "io.vertx", name = "vertx-junit5")
}
```
The `KnotxExtension` and `KnotxWiremockExtension` use parameters names to correctly initialize 
injected fields and parameters (see `@ClasspathResourcesMockServer`). It **requires** to compile modules with
```
tasks.withType(JavaCompile) {
  options.compilerArgs << "-parameters"
}
```

## Frequently asked questions

#### Is parallel test execution possible?

Currently not supported due to unknown Knot.x internal error that ends up in a segfault. However, all required functionality
is implemented inside `knotx-junit5` module.

#### Where can I find real examples how to use this extension?

Some simple examples are available in test package namespace `io.knotx.junit5.example`.  
For other use cases see following Knot.x projects that use this Knot.x JUnit5 module:
- [Knot.x Stack](https://github.com/Knotx/knotx-stack)
- [Knot.x HTTP Server](https://github.com/Knotx/knotx-server-http)
- [Knot.x Data Bridge](https://github.com/Knotx/knotx-data-bridge)

## Bugs
All feature requests and bugs can be filed as issues on [Gitub](https://github.com/Knotx/knotx-junit5/issues).
Do not use Github issues to ask questions, post them on the [User Group](https://groups.google.com/forum/#!forum/knotx) or [Gitter Chat](https://gitter.im/Knotx/Lobby).

## Licence
**Knot.x modules** are licensed under the [Apache License, Version 2.0 (the "License")](https://www.apache.org/licenses/LICENSE-2.0.txt)
