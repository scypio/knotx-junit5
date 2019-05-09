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
package io.knotx.junit5;

import static io.restassured.RestAssured.given;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

@ExtendWith(KnotxExtension.class)
@KnotxApplyConfiguration({"config/example_random_config.conf", "config/modules_config.conf" })
class KnotxExtensionTest {

  private static final String ANY_ENDPOINT = "/any";

  @Test
  @DisplayName("Expect response from HTTP server working on port from random section.")
  void callServerWithRandomPortFromRandomSection(io.vertx.reactivex.core.Vertx vertx,
      @RandomPort Integer globalServerPort) {
    // @formatter:off
    given().
        port(globalServerPort).
    when().
      get(ANY_ENDPOINT).
    then().assertThat().
        statusCode(200);
    // @formatter:on
  }

  @Test
  @DisplayName("Expect null when no random section defined.")
  @KnotxApplyConfiguration("config/modules_wiremock_config.conf")
  void callServerWithRandomPortFromWiremockSection(io.vertx.reactivex.core.Vertx vertx,
      @RandomPort Integer wGlobalServerPort) {
    Assertions.assertNull(wGlobalServerPort);
  }

}
