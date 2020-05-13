/*
 * Copyright (C) 2019 Knot.x Project
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
package io.knotx.junit5.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.fail;

import io.vertx.core.Vertx;
import io.vertx.junit5.VertxExtension;
import io.vertx.junit5.VertxTestContext;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

@ExtendWith(VertxExtension.class)
class HoconLoaderTest {

  @Test
  void testNotExistingFile(Vertx vertx) {
    try {
      HoconLoader.verify("config/not-defined.conf",
          json -> fail("Expect file not found exception!"), vertx);
    } catch (Throwable e) {
      // expected
    }
  }

  @Test
  void testExistingFile(Vertx vertx) throws Throwable {
    HoconLoader.verify("config/modules_config.conf",
        json -> assertEquals("io.knotx.junit5.util.TestConfigurationHttpServer",
            json.getJsonObject("modules").getString("server")), vertx);
  }

  @Test
  void testExistingFile(io.vertx.reactivex.core.Vertx vertx)
      throws Throwable {
    HoconLoader.verify("config/modules_config.conf",
        json -> assertEquals("io.knotx.junit5.util.TestConfigurationHttpServer",
            json.getJsonObject("modules").getString("server")), vertx);
  }

  @Test
  void testExistingFileAsync(Vertx vertx, VertxTestContext testContext) throws Throwable {
    HoconLoader.verifyAsync("config/modules_config.conf", json -> {
      assertEquals("io.knotx.junit5.util.TestConfigurationHttpServer",
          json.getJsonObject("modules").getString("server"));
      testContext.completeNow();
    }, testContext, vertx);
  }

  @Test
  void testExistingFileAsync(io.vertx.reactivex.core.Vertx vertx, VertxTestContext testContext)
      throws Throwable {
    HoconLoader.verifyAsync("config/modules_config.conf", json -> {
      assertEquals("io.knotx.junit5.util.TestConfigurationHttpServer",
          json.getJsonObject("modules").getString("server"));
      testContext.completeNow();
    }, testContext, vertx);
  }
}