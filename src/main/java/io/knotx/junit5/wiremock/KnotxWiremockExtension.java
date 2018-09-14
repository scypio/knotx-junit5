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
import com.github.tomakehurst.wiremock.core.WireMockConfiguration;
import com.github.tomakehurst.wiremock.stubbing.StubMapping;
import com.google.common.collect.ImmutableMap;
import com.typesafe.config.Config;
import io.knotx.junit5.KnotxBaseExtension;
import io.knotx.junit5.KnotxExtension;
import io.vertx.core.json.JsonObject;
import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.locks.ReentrantLock;
import org.junit.jupiter.api.extension.AfterAllCallback;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.api.extension.ParameterContext;
import org.junit.jupiter.api.extension.ParameterResolutionException;
import org.junit.jupiter.api.extension.ParameterResolver;
import org.junit.jupiter.api.extension.TestInstancePostProcessor;

/**
 * Manages {@linkplain WireMockServer} instances for parameter and field injection. Standalone
 * extension, can be used separately from {@linkplain KnotxExtension}.
 */
public class KnotxWiremockExtension extends KnotxBaseExtension
    implements ParameterResolver, TestInstancePostProcessor, AfterAllCallback {

  private static final ReentrantLock wiremockMapLock = new ReentrantLock(true);
  private static final HashMap<Integer, WireMock> wiremockMap = new HashMap<>();
  private static final HashMap<Integer, WireMockServer> wiremockServerMap = new HashMap<>();
  private static final HashMap<String, KnotxMockConfig> serviceNamePortMap = new HashMap<>();

  /**
   * Retrieve Wiremock for given port and add given mappings
   *
   * @param port on which server is configured
   * @param mappingBuilder given mapping
   * @return created mapping
   * @see WireMock#stubFor(MappingBuilder)
   */
  public static StubMapping stubForPort(int port, MappingBuilder mappingBuilder) {
    return getOrCreateWiremock(port).register(mappingBuilder);
  }

  /**
   * Add given mappings for this server
   *
   * @param server to which add mapping
   * @param mappingBuilder given mapping
   * @return created mapping
   * @see WireMock#stubFor(MappingBuilder)
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

      String name = getServerName(extensionContext, parameterContext);

      return setupWiremockServer(name, knotxWiremock);
    }

    throw new IllegalStateException("This should not happen");
  }

  @Override
  public void afterAll(ExtensionContext context) {
    shutdownWiremock();
  }

  /** Sets up all annotated fields in test class */
  @Override
  public void postProcessTestInstance(Object testInstance, ExtensionContext context) {
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
              String name = getServerName(context, field);
              WireMockServer server =
                  setupWiremockServer(name, field.getAnnotation(KnotxWiremock.class));

              field.setAccessible(true);
              try {
                field.set(testInstance, server);
              } catch (IllegalAccessException | IllegalArgumentException e) {
                throw new RuntimeException(
                    "Could not inject wiremock server into requested field", e);
              }
            });
  }

  private String getServerName(ExtensionContext context, Field field) {
    return getClassName(context) + field.getName();
  }

  private String getServerName(ExtensionContext context, ParameterContext parameterContext) {
    return getClassName(context) + parameterContext.getParameter().getName();
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

  private WireMockServer setupWiremockServer(String name, KnotxWiremock knotxWiremock) {
    KnotxMockConfig config = new KnotxMockConfig(name, knotxWiremock.port());
    return setupWiremockServer(config);
  }

  private WireMockServer setupWiremockServer(KnotxMockConfig config) {
    int port = config.port;
    String name = config.name;

    try {
      wiremockMapLock.lock();

      // rule: same name == same config, so was created before
      if (serviceNamePortMap.containsKey(name)) {
        return wiremockServerMap.get(serviceNamePortMap.get(name).port);
      }

      if (wiremockServerMap.containsKey(port)) {
        return wiremockServerMap.get(port);
      }

      WireMockConfiguration wireMockConfiguration = new WireMockConfiguration();
      wireMockConfiguration.extensions(new KnotxFileSource(config));

      if (port == KnotxMockConfig.RANDOM_PORT) {
        wireMockConfiguration.dynamicPort();
      } else {
        wireMockConfiguration.port(port);
      }

      WireMockServer server = new WireMockServer(wireMockConfiguration);
      server.start();

      port = server.port();
      config = new KnotxMockConfig(config, port);
      getOrCreateWiremock(port);

      wiremockServerMap.put(port, server);
      serviceNamePortMap.put(name, config);

      return server;
    } finally {
      wiremockMapLock.unlock();
    }
  }

  private void shutdownWiremock() {
    wiremockMapLock.lock();

    // calling WireMock.shutdown() would shutdown only the default instance
    wiremockServerMap.forEach((port, server) -> server.shutdown());
    wiremockServerMap.clear();
    wiremockMap.clear();
    serviceNamePortMap.clear();

    wiremockMapLock.unlock();
  }

  @Override
  public void addToOverrides(Config config, List<JsonObject> overrides, String forClass) {
    Map<String, Object> serversConfig = new HashMap<>();

    try {
      wiremockMapLock.lock();

      // get entries for given class, trim class name for results
      serviceNamePortMap
          .values()
          .stream()
          .filter(mockConfig -> mockConfig.name.startsWith(forClass))
          .forEach(
              mockConfig -> {
                String trimmed = mockConfig.name.substring(forClass.length());
                serversConfig.put(trimmed, ImmutableMap.of("port", mockConfig.port));
              });
    } finally {
      wiremockMapLock.unlock();
    }

    if (serversConfig.isEmpty()) {
      return;
    }

    JsonObject json = new JsonObject();
    json.put("test", ImmutableMap.of("wiremock", serversConfig));
    overrides.add(json);
  }
}
