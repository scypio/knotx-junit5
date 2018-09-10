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
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Vert.x ConfigProcessor implementation for Knot.x test purposes. Allows to use hierarchical
 * structure when loading Knot.x configurations rather than loading them as files completely
 * independent from each other.<br>
 * <br>
 * Used internally in {@linkplain KnotxExtension} to override ports retrieved from {@linkplain
 * KnotxWiremockExtension}.
 */
public class KnotxConcatConfigProcessor implements ConfigProcessor {

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
          JsonArray paths =
              Optional.of(configuration.getJsonArray("paths")).orElse(new JsonArray());
          JsonObject overrides =
              Optional.of(configuration.getJsonObject("overrides")).orElse(new JsonObject());

          // configurations are stored in order of overriding - base first, overrides last
          List<String> configs = new ArrayList<>();
          try {
            // load user configurations
            configs = paths.stream()
                .map(String::valueOf)
                .map(s -> vertx.fileSystem().readFileBlocking(s).toString())
                .collect(Collectors.toList());

            // put base files as last
            Collections.reverse(configs);

            // overrides first
            Config fullConfig = ConfigFactory.parseString(overrides.encode());

            // rest of files as fallbacks
            for (String reader : configs) {
              fullConfig = fullConfig.withFallback(ConfigFactory.parseString(reader));
            }

            // and render everything
            fullConfig = fullConfig.resolve();
            ConfigRenderOptions options =
                ConfigRenderOptions.concise().setJson(true).setComments(false).setFormatted(false);
            String output = fullConfig.root().render(options);

            future.complete(new JsonObject(output));
          } catch (Exception e) {
            future.fail(e);
          }
        },
        handler);
  }
}
