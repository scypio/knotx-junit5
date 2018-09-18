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
import java.util.function.Consumer;
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

  private static final ReentrantLock globalMapsLock = new ReentrantLock(true);
  private static final HashMap<Integer, KnotxWiremockServer> portToServerMap = new HashMap<>();
  private static final HashMap<String, KnotxWiremockServer> serviceNameToServerMap =
      new HashMap<>();

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

      String name = getClassFieldName(extensionContext, parameterContext);

      return setupWiremockServer(name, knotxWiremock);
    }

    throw new IllegalStateException("This should not happen");
  }

  @Override
  public void afterAll(ExtensionContext context) {
    // fixme: mocks are shut down before all execution ends in parallel environment
    // fixme: proper fix is to implement #17 and launch new mock instance for each test
    //shutdownWiremock();
  }

  /** Sets up all annotated fields in test class */
  @Override
  public void postProcessTestInstance(Object testInstance, ExtensionContext context) {
    Optional<Class<?>> testClass = context.getTestClass();
    if (!testClass.isPresent()) {
      return;
    }

    forEachWiremockFields(
        testClass.get(),
        field -> {
          String name = getClassFieldName(context, field);
          WireMockServer server =
              setupWiremockServer(name, field.getAnnotation(KnotxWiremock.class));

          field.setAccessible(true);
          try {
            field.set(testInstance, server);
          } catch (IllegalAccessException | IllegalArgumentException e) {
            throw new IllegalStateException(
                "Could not inject wiremock server into requested field", e);
          }
        });
  }

  public void addMissingInstanceServers(String forClass, ExtensionContext context) {
    Optional<Object> instance = context.getTestInstance();

    if (!instance.isPresent()) {
      return;
    }
    if (serviceNameToServerMap.keySet().stream().anyMatch(s -> s.startsWith(forClass))) {
      return;
    }

    Class<?> testClass = context.getRequiredTestClass();

    forEachWiremockFields(
        testClass,
        field -> {
          String name = getClassFieldName(context, field);

          try {
            field.setAccessible(true);
            Object wiremockObject = field.get(instance.get());
            KnotxWiremockServer wiremockServer;

            if (wiremockObject instanceof KnotxWiremockServer) {
              wiremockServer = ((KnotxWiremockServer) wiremockObject);

              int port = wiremockServer.port();

              portToServerMap.put(port, wiremockServer);
              serviceNameToServerMap.put(name, wiremockServer);
            }
          } catch (IllegalAccessException e) {
            throw new IllegalStateException(
                "Could not retrieve WireMockServer field value on alternative fork", e);
          }
        });
  }

  @Override
  public void addToOverrides(Config config, List<JsonObject> overrides, String forClass) {
    Map<String, Object> serversConfig = new HashMap<>();

    try {
      globalMapsLock.lock();

      // get entries for given class, trim class name for results
      serviceNameToServerMap
          .values()
          .stream()
          .map(KnotxWiremockServer::getMockConfig)
          .filter(mockConfig -> mockConfig.name.startsWith(forClass))
          .forEach(
              mockConfig -> {
                String trimmed = mockConfig.name.substring(forClass.length());
                serversConfig.put(trimmed, ImmutableMap.of("port", mockConfig.port));
              });
    } finally {
      globalMapsLock.unlock();
    }

    if (serversConfig.isEmpty()) {
      return;
    }

    JsonObject json = new JsonObject();
    json.put("test", ImmutableMap.of("wiremock", serversConfig));
    overrides.add(json);
  }

  private static WireMock getOrCreateWiremock(int port) {
    try {
      globalMapsLock.lock();

      if (portToServerMap.containsKey(port)) {
        return portToServerMap.get(port).getWireMock();
      }

      return new WireMock("localhost", port);
    } finally {
      globalMapsLock.unlock();
    }
  }

  private void forEachWiremockFields(Class<?> testClass, Consumer<Field> consumer) {
    Field[] fields = testClass.getDeclaredFields();

    Arrays.stream(fields)
        .filter(
            field ->
                field.isAnnotationPresent(KnotxWiremock.class)
                    && field.getType().equals(WireMockServer.class))
        .forEach(consumer);
  }

  private WireMockServer setupWiremockServer(String name, KnotxWiremock knotxWiremock) {
    KnotxMockConfig config = new KnotxMockConfig(name, knotxWiremock.port());
    return setupWiremockServer(config);
  }

  private WireMockServer setupWiremockServer(KnotxMockConfig config) {
    int port = config.port;
    String name = config.name;

    try {
      globalMapsLock.lock();

      if (serviceNameToServerMap.containsKey(name)) {
        return portToServerMap.get(serviceNameToServerMap.get(name).port());
      }
      if (portToServerMap.containsKey(port)) {
        return portToServerMap.get(port);
      }

      WireMockConfiguration wireMockConfiguration = new WireMockConfiguration();
      wireMockConfiguration.extensions(new KnotxFileSource(config));

      if (port == KnotxMockConfig.RANDOM_PORT) {
        wireMockConfiguration.dynamicPort();
      } else {
        wireMockConfiguration.port(port);
      }

      KnotxWiremockServer server = new KnotxWiremockServer(wireMockConfiguration);
      server.start();

      port = server.port();
      config = new KnotxMockConfig(config, port);
      server.setMockConfig(config);
      server.setWireMock(getOrCreateWiremock(port));

      portToServerMap.put(port, server);
      serviceNameToServerMap.put(name, server);

      return server;
    } finally {
      globalMapsLock.unlock();
    }
  }

  private void shutdownWiremock() {
    globalMapsLock.lock();

    // calling WireMock.shutdown() would shutdown only the default instance
    portToServerMap.forEach(
        (port, server) -> {
          if (server.isRunning()) {
            server.shutdown();
          }
        });
    portToServerMap.clear();
    serviceNameToServerMap.clear();

    globalMapsLock.unlock();
  }
}
