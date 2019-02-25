# Knot.x JUnit5
JUnit 5 extensions and data type converters for Knot.x integration tests. Those tests allow to setup
Knot.x instance with declared modules. It can be used both for module tests and regression tests.


## Setup

### Gradle 5 & Kotlin DSL: 
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
injected fields and parameters (see `@KnotxWiremock`). It requires to compile modules with
```
tasks.withType(JavaCompile) {
  options.compilerArgs << "-parameters"
}
```

## Extensions
Provides following helpers:

### KnotxExtension

Knot.x-specific extension that manages test Vert.x instances (with Knot.x configuration injection) 
and WireMock servers (through KnotxWiremockExtension).

**Example usage:**

```java
@ExtendWith(KnotxExtension.class)
public class ExampleIntegrationTest {

  @KnotxWiremock(port = 4001)
  protected WireMockServer mockRepository;

  @Test
  @KnotxApplyConfiguration({"default.conf", "overloaded.conf"})
  public void callModule_validKnotContextResult(VertxTestContext context, Vertx vertx) {
    // ...
  }
}
```

See [Vert.x JUnit 5 integration](https://vertx.io/docs/vertx-junit5/java/) for guide
how to interact with `VertxTestContext` instances. Also, under `io.knotx.junit5.util.RequestUtil`
there are some common methods that should make working with Vert.x tests contexts easier.

#### @KnotxApplyConfiguration

The annotation allows to specify one and more Knot.x configuration(s) to load for given test.
It accepts a paths array and loads all configuration entries using 
[Vert.x Config](https://vertx.io/docs/vertx-config/java/) file stores, through a custom 
implementation enhanced with support for HOCON hierarchical configuration loading and cross-file
variable references (for more details see section [KnotxConcatConfigProcessor](#KnotxConcatConfigProcessor) down below). 
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

#### KnotxConcatConfigProcessor

This implementation of Vert.x [ConfigProcessor](https://vertx.io/docs/vertx-config/java/#_extending_the_config_retriever)
enables KnotxExtension to load Knot.x configuration files hierarchically,
including cross-file references to variables/placeholders (only with HOCON files).

##### Vert.x Config limitations

The HOCON cross-references are not available out-of-box in Vert.x Config mechanism.
Knot.x supports JSON and HOCON configurations only, so that functionality would be useful in, for example, 
integration tests that can run in parrallel (i.e. randomization of ports, references to other parts of configuration).

However, Vert.x loads all given files (stores), evaluates them independently and then stiches
all files together to create a final result (JSON).

###### Cross-references example

We have two stores definitions:

```hocon
"stores": [
      {
        "type": "file",
        "format": "hocon",
        "config": { "path": "config/application.conf" }
      },
      {
        "type": "json",
        "config": { "test.wiremock.mockService.port": 4321 }
      }
    ]
```

The `application.conf` file contains

```hocon
config.somemodule.options.config {
  clientOptions {
    port = ${test.wiremock.mockService.port}
  }
}
test.wiremock {
  mockService {
    port = 1234
  }
}
```

The default Vert.x Config processing result:

```JSON
{
  "config": {
    "somemodule": {
      "options": {
        "config": {
          "clientOptions": {
            "port": 1234
          }
        }
      }
    }
  },
  "test": {
    "wiremock": {
      "mockService": {
        "port": 4321
      }
    }
  }
}
```

Expected behaviour:

```JSON
{
  "config": {
    "somemodule": {
      "options": {
        "config": {
          "clientOptions": {
            "port": 4321
          }
        }
      }
    }
  },
  "test": {
    "wiremock": {
      "mockService": {
        "port": 4321
      }
    }
  }
}
```

##### Solution

KnotxConcatConfigProcessor works around this problem. It creates a new config format, in which you specify a JSON file:

```json
{ 
  "paths": [ "base.conf", "specific.json" ], 
  "overrides": [ 
    { "baseKey": "newValue" },
    { "specificKey": [ "entry1", "entry2" ] }
  ]
}
```

Files from `paths` are loaded in given order, and each override object from `overrides` (JSON object) is applied 
on top of resulting HOCON Config object, and only then
the configuration gets resolved and effectively returned for Knot.x for processing.

Please refer to [HOCON readme](https://github.com/lightbend/config/blob/master/README.md) if you have any more questions regarding config loading
behavior.

#### Randomizing ports for usage inside tests

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
(example taken from `example_config.conf` file)

Then you can reference such variables from anywhere inside your configs; placeholder values will be substituted for real available
port numbers. For an example how to reference such values see `io.knotx.junit5.examples.ExampleKnotxJUnit5Test#injectRandomizedPort`
method from test classes.

### KnotxWiremockExtension

Standalone WireMockServer injection and lifecycle management. Allows for:
 
- Specifying on which port WireMockServer instance should be present,
- If no port is specified on annotation, a random one will be assigned to given instance,
- Running multiple mocked server instances,
- Referencing mocked servers' port numbers in Knot.x configuration (more details below).

***Warning:*** if you use KnotxExtension, you **must not inject** KnotxWiremockExtension, 
as the functionality of the latter gets auto-imported into the former.

#### WireMockServer injection and naming

WireMockServer instances are recognized by their identifier - either test class instance variable name 
or test method parameter name.

For example below, two servers will be available for `testWiremockRunningOnPort` method: `mockServiceRandom` 
with randomly assigned port, and `server` on port `3000`.

```java
@ExtendWith(KnotxWiremockExtension.class)
public class WireMockTest {

  private static final int MOCK_SERVICE_PORT_NUMBER = 3000;

  @KnotxWiremock // will be started on a random port
  private WireMockServer mockServiceRandom;

  @Test
  public void testWiremockRunningOnPort(
      @KnotxWiremock(port = MOCK_SERVICE_PORT_NUMBER) WireMockServer server)
      throws IOException, URISyntaxException {
    // ...
  }
}
```

**One exception applies:** one WireMockServer instance will be created per identifier within test given class:

```java
@ExtendWith(KnotxWiremockExtension.class)
public class WireMockTest {

  @KnotxWiremock
  private WireMockServer mockServiceRandom;

  @Test
  public void testWiremockEquality(
      @KnotxWiremock WireMockServer mockServiceRandom) {
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

### KnotxArgumentConverter

Simplifies writing parameterized tests that have data source as String but expect a specific object as a parameter. Currently supports only io.knotx.dataobjects.Fragment and io.vertx.core.json.JsonObject.

**Example usage:**

```java
public class ParameterizedTest {
  @ParameterizedTest
  @CsvSource(
      value = {
        "snippet_one_service_no_params.txt;{};1",
        "snippet_one_service_one_param.txt;{\"val\":1};1",
        "snippet_one_service_many_params.txt;{};1",
        "snippet_two_services.txt;{\"val\":1,\"val2\":2};2",
        "snippet_five_services.txt;{\"val\":3,\"val2\":4};5"
      },
      delimiter = ';')
  public void testWithParameters(
      @ConvertWith(KnotxArgumentConverter.class) Fragment fragment,
      @ConvertWith(KnotxArgumentConverter.class) JsonObject parameters,
      int number)
      throws Exception {
    // ...
  }
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
- [Knot.x Data Bridge](https://github.com/Knotx/knotx-data-bridge)
- [Knot.x Core](https://github.com/Cognifide/knotx)

## Bugs
All feature requests and bugs can be filed as issues on [Gitub](https://github.com/Knotx/knotx-junit5/issues).
Do not use Github issues to ask questions, post them on the [User Group](https://groups.google.com/forum/#!forum/knotx) or [Gitter Chat](https://gitter.im/Knotx/Lobby).

## Licence
**Knot.x modules** are licensed under the [Apache License, Version 2.0 (the "License")](https://www.apache.org/licenses/LICENSE-2.0.txt)
