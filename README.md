# Knot.x JUnit 5
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

  @KnotxWiremock(port = 4001)
  protected WireMockServer mockRepository;

  @Test
  @KnotxApplyConfiguration({"default.conf", "overloaded.conf"})
  public void callModule_validKnotContextResult(VertxTestContext context, Vertx vertx) {
    // ...
  }
}
```

#### @KnotxApplyConfiguration

The annotation allows to specify one and more Knot.x configuration(s) to load for given test.
It accepts a paths array and loads all configuration entries using 
[Vert.x Config](https://vertx.io/docs/vertx-config/java/) file stores (though through a custom 
implementation which fixes a few things; see section [KnotxConcatConfigProcessor](#KnotxConcatConfigProcessor)). 
It supports two configuration semantics:

- JSON (files with `json` extension)
- HOCON (files with `conf` extension)

The order of the configuration files is important, as it defines the overloading order 
(base file first, overrides last).
For conflicting (identical) keys in configuration, configuration entries arriving last 
will overwrite the value provided by the previous configuration files.

See [Vert.x Config overloading rules](https://vertx.io/docs/vertx-config/java/#_overloading_rules) 
for more details.

#### KnotxConcatConfigProcessor

This implementation of Vert.x [ConfigProcessor](https://vertx.io/docs/vertx-config/java/#_extending_the_config_retriever)
enables KnotxExtension to load Knot.x configuration files in a true hierarchical manner,
including cross-file references to variables (only with HOCON files).
 
From a user standpoint, HOCON cross-references should be available out-of-box in Vert.x's processing of configurations, since when you load configs
programatically, you provide a source for given file and its format, and only JSON and HOCON are supported so it would be quite logical assumption.  
This is not the case, however, and what Vert.x does internally is load all given files independently, evaluate them independently of each other,
and only after that's been done, stiches all files together to create a final result that gets served to Knot.x for processing.

KnotxConcatConfigProcessor works around this problem. It creates a new config format, in which you specify a JSON file:

```json
{ 
  "paths": [ "base.conf", "specific.json" ], 
  "overrides": {"baseKey": "newValue"}
}
```

Files from `paths` are loaded in given order, and `overrides` are applied directly on top of resulting HOCON Config object, and only then
the configuration gets resolved and effectively returned for Knot.x for processing.

Please refer to [HOCON readme](https://github.com/lightbend/config/blob/master/README.md) if you have any more questions regarding config loading
behavior.

### KnotxWiremockExtension

Standalone WireMockServer injection and lifecycle management. Allows for:
 
- Specifying on which port WireMockServer instance should be present,
- If no port is specified on annotation, a random one will be assigned to given instance,
- Running multiple mocked server instances,
- Autoinjecting mocked servers' port numbers into Knot.x configuration (more details below).

***Warning:*** if you use KnotxExtension, you **must not inject** KnotxWiremockExtension, 
as the functionality of the latter gets auto-imported into the former.

When used through KnotxExtension, WireMockServer instances' ports will be available 
for referencing in configuration under `test.wiremock.<mock_name>.port` variables (using HOCON syntax).  
Server's `mock_name` for injecting into configuration is currently taken from class variable or parameter name.
Mock name is an unique identifier, so if two different test classes specify the same mock name, only one mock instance 
will be created and injected into both class fields/method parameters.

For example below, two servers will be available for `testWiremockRunningOnPort` method: `mockServiceRandom` 
with randomly assigned port, and `server` on port `3000`.

**Example usage:**

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

## Bugs
All feature requests and bugs can be filed as issues on [Gitub](https://github.com/Knotx/knotx-junit5/issues).
Do not use Github issues to ask questions, post them on the [User Group](https://groups.google.com/forum/#!forum/knotx) or [Gitter Chat](https://gitter.im/Knotx/Lobby).

## Licence
**Knot.x modules** are licensed under the [Apache License, Version 2.0 (the "License")](https://www.apache.org/licenses/LICENSE-2.0.txt)
