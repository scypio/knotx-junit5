/*
 * Copyright (C) 2018 Knot.x Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.knotx.junit5;

import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;
import com.typesafe.config.ConfigRenderOptions;
import io.knotx.junit5.wiremock.KnotxWiremockExtension;
import io.vertx.config.spi.ConfigProcessor;
import io.vertx.core.AsyncResult;
import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.file.FileSystem;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Vert.x ConfigProcessor implementation for Knot.x test purposes. Allows to use hierarchical
 * structure when loading Knot.x configurations rather than loading them as files completely
 * independent from each other.<br>
 * <br>
 * Used internally in {@linkplain KnotxExtension} to override ports retrieved from {@linkplain
 * KnotxWiremockExtension}.
 */
public class KnotxConcatConfigProcessor implements ConfigProcessor {

  private static final String OVERRIDES_KEY = "overrides";
  private static final String PATHS_KEY = "paths";

  @Override
  public String name() {
    return "knotx";
  }

  @Override
  public void process(
      Vertx vertx,
      JsonObject configuration,
      Buffer input,
      Handler<AsyncResult<JsonObject>> handler) {
    vertx.executeBlocking(
        future -> {
          try {
            Config finalConfig = createHoconConfig(vertx.fileSystem(), configuration);

            JsonObject result = resolveConfig(finalConfig);

            future.complete(result);
          } catch (Exception e) {
            future.fail(e);
          }
        },
        handler);
  }

  /**
   * Evaluate passed <code>configuration</code> and return as HOCON Config object.<br>
   * For further reference on configuration format please refer to README.md, section "Vert.x Config
   * limitations".
   *
   * @param fileSystem Vert.x object used to resolve file contents declared in configuration
   * @param configuration JSON in format described in README.md
   * @return full HOCON Config fallback chain
   */
  public Config createHoconConfig(FileSystem fileSystem, JsonObject configuration) {
    Stream<String> base = getBase(fileSystem, configuration);
    Stream<String> overrides = getOverrides(configuration);

    // configurations are stored in order of overriding - base first, overrides last
    // but for actual config creation we need them together and in reverse order
    List<String> configs = concatAndReverseOrder(base, overrides);

    return createConfigFallbackChain(configs);
  }

  private Stream<String> getOverrides(JsonObject configuration) {
    return Optional.of(configuration.getJsonArray(OVERRIDES_KEY))
        .orElse(new JsonArray().add(configuration.getValue(OVERRIDES_KEY)))
        .stream()
        .filter(Objects::nonNull)
        .map(
            o -> {
              if (!(o instanceof JsonObject)) {
                throw new IllegalArgumentException(
                    "Overrides must be instances of JsonObject, got: '" + o.getClass() + "'");
              }
              return (JsonObject) o;
            })
        .map(JsonObject::encode);
  }

  private Stream<String> getBase(final FileSystem fileSystem, JsonObject configuration) {
    return Optional.of(configuration.getJsonArray(PATHS_KEY))
        .orElse(new JsonArray())
        .stream()
        .map(String::valueOf)
        .map(s -> fileSystem.readFileBlocking(s).toString());
  }

  private List<String> concatAndReverseOrder(Stream<String> base, Stream<String> overrides) {
    List<String> configs =
        Stream.concat(base.sequential(), overrides.sequential()).collect(Collectors.toList());

    Collections.reverse(configs);

    return configs;
  }

  private Config createConfigFallbackChain(List<String> configs) {
    Config fullConfig = ConfigFactory.empty();

    for (String config : configs) {
      fullConfig = fullConfig.withFallback(ConfigFactory.parseString(config));
    }
    return fullConfig;
  }

  private JsonObject resolveConfig(Config finalConfig) {
    ConfigRenderOptions options =
        ConfigRenderOptions.concise().setJson(true).setComments(false).setFormatted(false);

    finalConfig = finalConfig.resolve();
    String jsonString = finalConfig.root().render(options);

    return new JsonObject(jsonString);
  }
}
