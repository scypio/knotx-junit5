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
import com.github.tomakehurst.wiremock.client.MappingBuilder;
import com.github.tomakehurst.wiremock.client.WireMock;
import com.github.tomakehurst.wiremock.core.Options;
import com.github.tomakehurst.wiremock.core.WireMockConfiguration;
import com.github.tomakehurst.wiremock.stubbing.StubMapping;
import io.knotx.junit5.KnotxBaseExtension;
import io.knotx.junit5.KnotxExtension;
import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Optional;
import java.util.concurrent.locks.ReentrantLock;
import org.junit.jupiter.api.extension.AfterAllCallback;
import org.junit.jupiter.api.extension.AfterEachCallback;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.api.extension.ExtensionContext.Store;
import org.junit.jupiter.api.extension.ParameterContext;
import org.junit.jupiter.api.extension.ParameterResolutionException;
import org.junit.jupiter.api.extension.ParameterResolver;
import org.junit.jupiter.api.extension.TestInstancePostProcessor;

/**
 * Manages {@linkplain WireMockServer} instances for parameter and field injection. Standalone
 * extension, can be used separately from {@linkplain KnotxExtension}.
 */
public class KnotxWiremockExtension extends KnotxBaseExtension
    implements ParameterResolver, TestInstancePostProcessor, AfterAllCallback, AfterEachCallback {

  private static final String WIREMOCK_SERVER_INSTANCES_MAP_STORE_KEY =
      "WiremockServerInstancesMap";

  private static final ReentrantLock wiremockMapLock = new ReentrantLock(true);
  private static final HashMap<Integer, WireMock> wiremockMap = new HashMap<>();

  /**
   * Retrieve Wiremock for given port and add given mappings
   *
   * @see WireMock#stubFor(MappingBuilder)
   * @param port on which server is configured
   * @param mappingBuilder given mapping
   * @return created mapping
   */
  public static StubMapping stubForPort(int port, MappingBuilder mappingBuilder) {
    return getOrCreateWiremock(port).register(mappingBuilder);
  }

  /**
   * Add given mappings for this server
   *
   * @see WireMock#stubFor(MappingBuilder)
   * @param server to which add mapping
   * @param mappingBuilder given mapping
   * @return created mapping
   */
  public static StubMapping stubForServer(WireMockServer server, MappingBuilder mappingBuilder) {
    return stubForPort(server.port(), mappingBuilder);
  }

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
    cleanupWiremockServers(context);
    shutdownWiremock();
  }

  /** Sets up all annotated fields in test class */
  @Override
  public void postProcessTestInstance(Object testInstance, ExtensionContext context)
      throws Exception {
    Optional<Class<?>> testClass = context.getTestClass();
    if (!testClass.isPresent()) {
      return;
    }

    Field[] fields = testClass.get().getDeclaredFields();

    Arrays.stream(fields)
        .filter(
            field ->
                field.isAnnotationPresent(KnotxWiremock.class)
                    && field.getType().equals(WireMockServer.class))
        .forEach(
            field -> {
              Store store = getStore(context);
              WireMockServer server =
                  setupWiremockServer(field.getAnnotation(KnotxWiremock.class), store);

              field.setAccessible(true);
              try {
                field.set(testInstance, server);
              } catch (IllegalAccessException | IllegalArgumentException e) {
                throw new RuntimeException(
                    "Could not inject wiremock server into requested field", e);
              }
            });
  }

  @Override
  public void afterEach(ExtensionContext context) throws Exception {
    cleanupWiremockServers(context);
  }

  private static WireMock getOrCreateWiremock(int port) {
    try {
      wiremockMapLock.lock();

      if (wiremockMap.containsKey(port)) {
        return wiremockMap.get(port);
      }

      WireMock instance = new WireMock("localhost", port);

      wiremockMap.put(port, instance);

      return instance;
    } finally {
      wiremockMapLock.unlock();
    }
  }

  private WireMockServer setupWiremockServer(KnotxWiremock knotxWiremock, Store store) {
    int port = knotxWiremock.port();
    WiremockServerMap map = getWiremockServerMap(store);

    if (map.containsKey(port)) {
      return map.get(port);
    }

    WireMockConfiguration config = new WireMockConfiguration();
    config.extensions(new KnotxFileSource());

    if (port == Options.DYNAMIC_PORT) {
      config.dynamicPort();
    } else {
      config.port(port);
    }

    WireMockServer server = new WireMockServer(config);
    server.start();

    port = server.port();
    getOrCreateWiremock(port);
    map.put(port, server);

    return server;
  }

  /** Cleanup known Wiremock instances */
  private void cleanupWiremockServers(ExtensionContext context) {
    Store store = getStore(context);
    WiremockServerMap instancesMap = getWiremockServerMap(store);

    if (null != instancesMap) {
      instancesMap
          .values()
          .forEach(
              server -> {
                if (server.isRunning()) {
                  server.resetRequests();
                  server.resetToDefaultMappings();
                }
              });
      instancesMap.clear();
    }
  }

  private void shutdownWiremock() {
    wiremockMapLock.lock();

    wiremockMap.values().forEach(WireMock::shutdown);
    wiremockMap.clear();

    wiremockMapLock.unlock();
  }

  private WiremockServerMap getWiremockServerMap(Store store) {
    return store.getOrComputeIfAbsent(
        WIREMOCK_SERVER_INSTANCES_MAP_STORE_KEY,
        o -> new WiremockServerMap(),
        WiremockServerMap.class);
  }

  private class WiremockServerMap extends HashMap<Integer, WireMockServer> {}
}
