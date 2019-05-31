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

import io.knotx.junit5.wiremock.ClasspathResourcesMockServer;
import io.vertx.reactivex.core.Vertx;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

@ExtendWith(KnotxExtension.class)
@KnotxApplyConfiguration({"config/example_wiremock_config.conf", "config/modules_config.conf"})
class KnotxExtensionInheritanceTest {

  @Test
  @DisplayName("Inject port from class level configuration file.")
  void configurationClassScope(@ClasspathResourcesMockServer Integer minimalRequiredService) {
    Assertions.assertEquals(3000, minimalRequiredService);
  }

  @Test
  @DisplayName("Inject port from method level configuration file.")
  @KnotxApplyConfiguration("config/method_level_config.conf")
  void configurationMethodScope(@ClasspathResourcesMockServer Integer mockService) {
    Assertions.assertEquals(4001, mockService);
  }

  @Test
  @DisplayName("Inject port from property level configuration file.")
  void configurationParamScope(
      @KnotxApplyConfiguration("config/param_level_config.conf") Vertx vertx,
      @ClasspathResourcesMockServer Integer queryOnlyRepository) {
    Assertions.assertEquals(5001, queryOnlyRepository);
  }

  @Test
  @DisplayName("Inject ports from class/method/param level configuration files.")
  @KnotxApplyConfiguration("config/method_level_config.conf")
  void configurationMixedScope(
      @KnotxApplyConfiguration("config/param_level_config.conf") Vertx vertx,
      @ClasspathResourcesMockServer Integer minimalRequiredService,
      @ClasspathResourcesMockServer Integer mockService,
      @ClasspathResourcesMockServer Integer queryOnlyRepository) {
    Assertions.assertEquals(3000, minimalRequiredService);
    Assertions.assertEquals(4001, mockService);
    Assertions.assertEquals(5001, queryOnlyRepository);
  }

  @Test
  @DisplayName(
      "Inject port from param level configuration files when both method and param configurations overrides the class one.")
  @KnotxApplyConfiguration("config/method_level_config.conf")
  void configurationMethodParamScope(
      @KnotxApplyConfiguration("config/param_level_config.conf") Vertx vertx,
      @ClasspathResourcesMockServer Integer allPropertiesService) {
    Assertions.assertEquals(6002, allPropertiesService);
  }
}
