# KnotxConcatConfigProcessor

This implementation of Vert.x [ConfigProcessor](https://vertx.io/docs/vertx-config/java/#_extending_the_config_retriever)
enables KnotxExtension to load Knot.x configuration files hierarchically,
including cross-file references to variables/placeholders (only with HOCON files).

## Vert.x Config limitations

The HOCON cross-references are not available out-of-box in Vert.x Config mechanism.
Knot.x supports JSON and HOCON configurations only, so that functionality would be useful in, for example, 
integration tests that can run in parrallel (i.e. randomization of ports, references to other parts of configuration).

However, Vert.x loads all given files (stores), evaluates them independently and then stiches
all files together to create a final result (JSON).

### Cross-references example

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

## Solution

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
