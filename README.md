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
The annotation allows to specify one and more Knot.x configuration/s. It accepts a paths array and loads
all configuration entries using [Vert.x Config](https://vertx.io/docs/vertx-config/java/) file stores. 
It supports two configuration semantics:

- JSON (files with `json` extension)
- HOCON (files with `conf` extension)

The order of the configuration files is important as it defines the overloading. 
For conflicting key, configurations arriving last overloads the value provided by the previous 
configuration files.

See [Vert.x Config overloading rules](https://vertx.io/docs/vertx-config/java/#_overloading_rules) 
for more details.

### KnotxWiremockExtension

Standalone WireMock server injection and lifecycle management, currently allows for specifying on which port WireMockServer instance should be present*.

**Warning:** if you use KnotxExtension, you *must not inject* KnotxWiremockExtension, as the functionality of the latter is included in the former.

**Example usage:**

```java
@ExtendWith(KnotxWiremockExtension.class)
public class WireMockTest {

  private static final int MOCK_SERVICE_PORT_NUMBER = 3000;

  @Test
  public void testWiremockRunningOnPort(
      @KnotxWiremock(port = MOCK_SERVICE_PORT_NUMBER) WireMockServer server)
      throws IOException, URISyntaxException {
    // ...
  }
}
```

<sub>* At the time of creation of this module, there are no widely available extensions that have this function</sub>

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
