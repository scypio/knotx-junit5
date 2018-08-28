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
package io.knotx.junit5.wiremock;

import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.client.WireMock;
import com.github.tomakehurst.wiremock.core.Options;
import com.github.tomakehurst.wiremock.core.WireMockConfiguration;
import io.knotx.junit5.KnotxBaseExtension;
import io.knotx.junit5.KnotxExtension;
import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Optional;
import org.junit.jupiter.api.extension.AfterAllCallback;
import org.junit.jupiter.api.extension.AfterEachCallback;
import org.junit.jupiter.api.extension.BeforeAllCallback;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.api.extension.ExtensionContext.Store;
import org.junit.jupiter.api.extension.ParameterContext;
import org.junit.jupiter.api.extension.ParameterResolutionException;
import org.junit.jupiter.api.extension.ParameterResolver;

/**
 * Manages {@linkplain WireMockServer} instances for parameter and field injection. Standalone
 * extension, can be used separately from {@linkplain KnotxExtension}.
 */
public class KnotxWiremockExtension extends KnotxBaseExtension
    implements ParameterResolver, BeforeAllCallback, AfterAllCallback, AfterEachCallback {

  private static final String WIREMOCK_INSTANCES_MAP_STORE_KEY = "WiremockInstancesMap";

  @Override
  public boolean supportsParameter(
      ParameterContext parameterContext, ExtensionContext extensionContext)
      throws ParameterResolutionException {
    Class<?> type = getType(parameterContext);

    if (parameterContext.getParameter().isAnnotationPresent(KnotxWiremock.class)) {
      if (type.equals(WireMockServer.class)) {
        return true;
      }
      if (type.equals(String.class)) {
        throw new ParameterResolutionException(
            "Annotating String with KnotxWiremock is not supported");
      }
    }

    return false;
  }

  @Override
  public Object resolveParameter(
      ParameterContext parameterContext, ExtensionContext extensionContext)
      throws ParameterResolutionException {
    Class<?> type = getType(parameterContext);

    if (type == WireMockServer.class) {
      KnotxWiremock knotxWiremock =
          parameterContext
              .findAnnotation(KnotxWiremock.class)
              .orElseThrow(IllegalStateException::new);

      Store store = getStore(extensionContext);

      return setupWiremockServer(knotxWiremock, store);
    }

    throw new IllegalStateException("This should not happen");
  }

  @Override
  public void afterAll(ExtensionContext context) throws Exception {
    cleanupWiremock(context);
  }

  /** Sets up all annotated fields in test class */
  @Override
  public void beforeAll(ExtensionContext context) throws Exception {
    Optional<Class<?>> testClass = context.getTestClass();
    if (!testClass.isPresent()) {
      return;
    }

    Field[] fields = testClass.get().getDeclaredFields();

    Arrays.stream(fields)
        .filter(
            field ->
                field.isAnnotationPresent(KnotxWiremock.class)
                    && field.getDeclaringClass().equals(WireMockServer.class))
        .forEach(
            field -> {
              Store store = getStore(context);
              WireMockServer server =
                  setupWiremockServer(field.getAnnotation(KnotxWiremock.class), store);

              field.setAccessible(true);
              try {
                field.set(WireMockServer.class.newInstance(), server);
              } catch (IllegalAccessException | InstantiationException e) {
                throw new RuntimeException(
                    "Could not inject wiremock server into requested field", e);
              }
            });
  }

  @Override
  public void afterEach(ExtensionContext context) throws Exception {
    cleanupWiremock(context);
  }

  private WireMockServer setupWiremockServer(KnotxWiremock knotxWiremock, Store store) {
    int port = knotxWiremock.port();
    WiremockMap map = getWiremockMap(store);

    if (map.containsKey(port)) {
      return map.get(port);
    }

    WireMockConfiguration config = new WireMockConfiguration();

    if (port == Options.DYNAMIC_PORT) {
      config.dynamicPort();
    } else {
      config.port(port);
    }

    WireMockServer server = new WireMockServer(config);
    server.start();

    port = server.port();
    WireMock.configureFor("localhost", port);
    map.put(port, server);

    return server;
  }

  /** Cleanup known Wiremock instances */
  private void cleanupWiremock(ExtensionContext context) {
    Store store = getStore(context);
    WiremockMap instancesMap = getWiremockMap(store);

    if (null != instancesMap) {
      instancesMap
          .values()
          .forEach(
              server -> {
                if (server.isRunning()) {
                  server.resetRequests();
                  server.resetToDefaultMappings();
                  server.stop();
                }
              });
      instancesMap.clear();
    }
  }

  private WiremockMap getWiremockMap(Store store) {
    return store.getOrComputeIfAbsent(
        WIREMOCK_INSTANCES_MAP_STORE_KEY, o -> new WiremockMap(), WiremockMap.class);
  }

  private class WiremockMap extends HashMap<Integer, WireMockServer> {}
}
