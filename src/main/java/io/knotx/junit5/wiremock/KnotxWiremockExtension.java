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

import static io.knotx.junit5.util.ReflectUtil.forEachWiremockFields;
import static io.knotx.junit5.util.StreamUtil.anyKeyStartsWith;

import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.client.MappingBuilder;
import com.github.tomakehurst.wiremock.client.WireMock;
import com.github.tomakehurst.wiremock.core.Options;
import com.github.tomakehurst.wiremock.core.WireMockConfiguration;
import com.github.tomakehurst.wiremock.stubbing.StubMapping;
import com.google.common.collect.ImmutableMap;
import com.typesafe.config.Config;
import io.knotx.junit5.KnotxBaseExtension;
import io.knotx.junit5.KnotxExtension;
import io.knotx.junit5.util.HoconUtil;
import io.knotx.junit5.util.ReflectUtil;
import io.knotx.junit5.util.StreamUtil;
import io.vertx.core.json.JsonObject;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.concurrent.locks.ReentrantLock;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.apache.commons.lang3.StringUtils;
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

  private static final String WIREMOCK_NAMESPACE = "test.wiremock";

  private static final ReentrantLock globalMapsLock = new ReentrantLock(true);
  private static final HashMap<Integer, KnotxWiremockServer> portToServerMap = new HashMap<>();
  private static final HashMap<String, KnotxWiremockServer> serviceNameToServerMap =
      new HashMap<>();
  private final HashMap<String, KnotxWiremockServer> localInstanceServers = new HashMap<>();

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
      if (type.equals(WireMockServer.class) || type.equals(Integer.class)) {
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

    return parameterContext
        .findAnnotation(KnotxWiremock.class).map(knotxWiremock -> {
          String nameReference = getFullyQualifiedName(extensionContext, parameterContext,
              knotxWiremock);
          WireMockServer server = setupWiremockServer(nameReference, knotxWiremock);
          Class<?> type = getType(parameterContext);
          if (type.equals(WireMockServer.class)) {
            return server;
          } else if (type.equals(Integer.class)) {
            return server.port();
          } else {
            throw new IllegalStateException("This should never happen!");
          }
        }).orElseThrow(() -> new IllegalStateException(
            "Not supported parameter: " + parameterContext.getParameter().getName()));

  }

  private String getFullyQualifiedName(ExtensionContext extensionContext,
      ParameterContext parameterContext, KnotxWiremock knotxWiremock) {
    String paramName = checkAndGetParameterName(parameterContext);
    if (knotxWiremock.port() == Options.DYNAMIC_PORT) {
      String fieldName = getClassFieldName(extensionContext, paramName);
      if (localInstanceServers.containsKey(fieldName)) {
        return fieldName;
      }
    }
    return getClassMethodParameterName(extensionContext, parameterContext);

  }

  private String checkAndGetParameterName(ParameterContext parameterContext) {
    String name = parameterContext.getParameter().getName();
    if (name.startsWith("arg")) {
      throw new IllegalStateException(
          "Please configure 'options.compilerArgs << \"-parameters\"', please check the README file.");
    }
    return name;
  }

  @Override
  public void afterAll(ExtensionContext context) {
    globalMapsLock.lock();

    // cleanup our local instances, operate only on port numbers as they're unique
    localInstanceServers.forEach(
        (service, server) -> {
          if (server.isRunning()) {
            server.shutdown();
          }

          // remove instances from global map
          int port = server.getMockConfig().port;
          portToServerMap.remove(port);
          serviceNameToServerMap.values().removeIf(s -> s.getMockConfig().port == port);
        });

    globalMapsLock.unlock();
  }

  /**
   * Sets up all annotated fields in test class
   */
  @Override
  public void postProcessTestInstance(Object testInstance, ExtensionContext context) {
    Optional<Class<?>> testClass = context.getTestClass();
    if (!testClass.isPresent()) {
      return;
    }

    forEachWiremockFields(
        testClass.get(),
        field -> {
          String reference = getClassFieldName(context, field);
          KnotxWiremock wiremockAnnotation = field.getAnnotation(KnotxWiremock.class);

          WireMockServer server = setupWiremockServer(reference, wiremockAnnotation);

          ReflectUtil.setField(testInstance, field, server);
        });
  }

  public void addMissingInstanceServers(String forClass, ExtensionContext context) {
    Optional<Object> instance = context.getTestInstance();

    if (!instance.isPresent()) {
      return;
    }
    if (anyKeyStartsWith(localInstanceServers, forClass)) {
      return;
    }
    if (anyKeyStartsWith(serviceNameToServerMap, forClass)) {
      return;
    }

    Class<?> testClass = context.getRequiredTestClass();

    try {
      globalMapsLock.lock();

      forEachWiremockFields(
          testClass,
          field -> {
            String name = getClassFieldName(context, field);

            Object wiremockObject = ReflectUtil.fieldValue(instance.get(), field);
            KnotxWiremockServer wiremockServer;

            if (wiremockObject instanceof KnotxWiremockServer) {
              wiremockServer = ((KnotxWiremockServer) wiremockObject);

              int port = wiremockServer.port();

              // DON'T YOU EVEN DARE TO PUT THIS WIREMOCK INSTANCE INTO LOCAL INSTANCES MAP
              portToServerMap.put(port, wiremockServer);
              serviceNameToServerMap.put(name, wiremockServer);
            }
          });

    } finally {
      globalMapsLock.unlock();
    }
  }

  @Override
  public void addToOverrides(Config config, List<JsonObject> overrides, String forClass) {
    if (!config.hasPath(WIREMOCK_NAMESPACE)) {
      return;
    }
    List<String> serverNames = getServerNames(config);

    // build KnotxMockConfig objects from
    for (String serverName : serverNames) {
      String base = KnotxWiremockExtension.WIREMOCK_NAMESPACE + "." + serverName;
      String reference = forClass + serverName;

      KnotxMockConfig mockConfig = KnotxMockConfig.createMockConfig(config, reference, base);
      KnotxWiremockServer server = setupWiremockServer(mockConfig);

      if (StringUtils.isEmpty(mockConfig.callToConfigure)) {
        String[] httpMethods =
            HoconUtil.getStringOrDefault(config, base + ".httpMethods", "GET").split("[|]");

        for (String method : httpMethods) {
          MappingBuilder builder;

          if ("GET".equalsIgnoreCase(method)) {
            builder =
                WireMock.get(WireMock.urlMatching(mockConfig.urlMatching))
                    .willReturn(WireMock.aResponse().withHeaders(mockConfig.additionalHeaders));

            stubForServer(server, builder);
          } else if ("POST".equalsIgnoreCase(method)) {
            builder =
                WireMock.post(WireMock.urlMatching(mockConfig.urlMatching))
                    .willReturn(WireMock.aResponse().withHeaders(mockConfig.additionalHeaders));

            stubForServer(server, builder);
          }
        }
      } else {
        // callToConfigure must be in format: io.whatever.ClassName#methodName
        ReflectUtil.configureServerViaMethod(server);
      }
    }

    Map<String, Object> serversConfig = new HashMap<>();

    try {
      globalMapsLock.lock();

      Stream<KnotxWiremockServer> stream =
          StreamUtil.concatValues(serviceNameToServerMap, localInstanceServers);

      // get entries for given class, trim class name for results
      stream
          .map(KnotxWiremockServer::getMockConfig)
          .filter(mockConfig -> mockConfig.reference.startsWith(forClass))
          .forEach(
              mockConfig -> {
                String trimmed = mockConfig.reference.substring(forClass.length());
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
  
  private List<String> getServerNames(Config config) {
    return config.getConfig(WIREMOCK_NAMESPACE).entrySet().stream()
        .map(Entry::getKey)
        .map(key -> {
          if (key.contains(".")) {
            return key.substring(0, key.indexOf("."));
          } else {
            return key;
          }
        }).collect(Collectors.toList());
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

  private KnotxWiremockServer setupWiremockServer(String reference, KnotxWiremock knotxWiremock) {
    KnotxMockConfig config = new KnotxMockConfig(reference, knotxWiremock.port());
    return setupWiremockServer(config);
  }

  private KnotxWiremockServer setupWiremockServer(KnotxMockConfig config) {
    int port = config.port;
    String reference = config.reference;

    try {
      globalMapsLock.lock();

      if (localInstanceServers.containsKey(reference)) {
        return localInstanceServers.get(reference);
      }
      if (serviceNameToServerMap.containsKey(reference)) {
        return portToServerMap.get(serviceNameToServerMap.get(reference).port());
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
      serviceNameToServerMap.put(reference, server);
      localInstanceServers.put(reference, server);

      return server;
    } finally {
      globalMapsLock.unlock();
    }
  }
}
