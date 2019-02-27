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
package io.knotx.junit5.examples;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.urlMatching;
import static io.knotx.junit5.wiremock.KnotxWiremockExtension.stubForServer;

import com.github.tomakehurst.wiremock.WireMockServer;
import io.knotx.junit5.KnotxApplyConfiguration;
import io.knotx.junit5.KnotxExtension;
import io.knotx.junit5.RadomPort;
import io.knotx.junit5.wiremock.ClasspathResourcesMockServer;
import io.vertx.reactivex.core.Vertx;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

@ExtendWith(KnotxExtension.class)
@KnotxApplyConfiguration("config/modules_config.conf")
public class ExampleKnotxJUnit5Test {

  public static void configureMock(WireMockServer server) {
    // do whatever you need to do with this server
    stubForServer(
        server,
        get(urlMatching("/service/mock/.*"))
            .willReturn(
                aResponse()
                    .withHeader("Cache-control", "no-cache, no-store, must-revalidate")
                    .withHeader("Content-Type", "application/json; charset=UTF-8")
                    .withHeader("X-Server", "Knot.x")));
  }

  @Test
  @KnotxApplyConfiguration("config/example_random_config.conf")
  public void vertxMethod(Vertx vertx) {
    // Knot.x config from method and class level will be injected into Vert.x instance
    // you can of course skip class level Knot.x config
  }

  @Test
  @KnotxApplyConfiguration({"config/example_random_config.conf", "config/param_level_example_config.conf" })
  public void vertxMultiple(Vertx vertx) {
    // can also declare multiple configs in the same annotation
  }

  @Test
  @KnotxApplyConfiguration("config/example_random_config.conf")
  public void noVertxInstance() {
    // no Vertx parameter - behavior is undefined
  }

  @Test
  @KnotxApplyConfiguration("config/example_random_config.conf")
  public void injectRandomizedPort(@RadomPort Integer globalServerPort) {
    // integer parameter will be filled with generated port
    // from 'example_config.conf' from section 'random' for entry 'globalServer'
  }

  @Test
  @KnotxApplyConfiguration("config/example_random_config.conf")
  public void injectWireMockServer(
      @ClasspathResourcesMockServer WireMockServer globalServer,
      @ClasspathResourcesMockServer Integer minimalRequiredService) {
    // injects server/port number into given parameters
    // this is different from @RadomPort mechanism
  }
}
