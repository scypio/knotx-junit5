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

import com.google.common.collect.ImmutableMap;
import com.typesafe.config.Config;
import io.knotx.junit5.util.FreePortFinder;
import io.knotx.junit5.wiremock.ClasspathResourcesMockServer;
import io.knotx.junit5.wiremock.KnotxWiremockExtension;
import io.vertx.config.ConfigRetrieverOptions;
import io.vertx.config.ConfigStoreOptions;
import io.vertx.core.DeploymentOptions;
import io.vertx.core.Verticle;
import io.vertx.core.Vertx;
import io.vertx.core.VertxException;
import io.vertx.core.json.JsonObject;
import io.vertx.junit5.VertxExtension;
import java.lang.reflect.Executable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import org.apache.commons.lang3.StringUtils;
import org.junit.jupiter.api.extension.AfterAllCallback;
import org.junit.jupiter.api.extension.AfterEachCallback;
import org.junit.jupiter.api.extension.AfterTestExecutionCallback;
import org.junit.jupiter.api.extension.BeforeEachCallback;
import org.junit.jupiter.api.extension.BeforeTestExecutionCallback;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.api.extension.ExtensionContext.Store;
import org.junit.jupiter.api.extension.ParameterContext;
import org.junit.jupiter.api.extension.ParameterResolutionException;
import org.junit.jupiter.api.extension.ParameterResolver;
import org.junit.jupiter.api.extension.TestInstancePostProcessor;
import org.junit.jupiter.api.extension.TestInstantiationException;

/**
 * Support for field and parameter injection for Knot.x tests <br>
 * <br>
 * Injects and manages instances of:
 *
 * <ul>
 *   <li>{@linkplain io.vertx.core.Vertx}, with Knot.x config injection via {@linkplain
 *       KnotxApplyConfiguration}
 *   <li>{@linkplain io.vertx.reactivex.core.Vertx}, same as above
 *   <li>{@linkplain com.github.tomakehurst.wiremock.WireMockServer} when annotated with {@linkplain
 *       ClasspathResourcesMockServer}
 * </ul>
 */
public class KnotxExtension extends KnotxBaseExtension
    implements ParameterResolver,
        BeforeEachCallback,
        AfterEachCallback,
        AfterTestExecutionCallback,
        BeforeTestExecutionCallback,
        AfterAllCallback,
        TestInstancePostProcessor {

  private static final long DEFAULT_TIMEOUT_SECONDS = 30;
  private static final String VERTX_INSTANCE_STORE_KEY = "VertxInstance";

  private static final String PORT = "port";
  private static final String HOCON_EXTENSION = "conf";
  private static final String JSON_EXTENSION = "json";
  private static final String RANDOM_GEN_NAMESPACE = "test.random";

  private static final ReadWriteLock referenceMapLock = new ReentrantReadWriteLock(true);
  private static final Map<String, Integer> referencePortMap = new HashMap<>();

  private final VertxExtension vertxExtension = new VertxExtension();
  private final KnotxWiremockExtension wiremockExtension = new KnotxWiremockExtension();

  @Override
  public boolean supportsParameter(
      ParameterContext parameterContext, ExtensionContext extensionContext)
      throws ParameterResolutionException {
    // what vertx supports we sometimes hijack and inject our config
    if (vertxExtension.supportsParameter(parameterContext, extensionContext)) {
      return true;
    }
    if (wiremockExtension.supportsParameter(parameterContext, extensionContext)) {
      return true;
    }

    // vertx and reactivex-vertx
    return shouldSupportVertx(parameterContext) || shouldSupportInjection(parameterContext);
  }

  @Override
  public Object resolveParameter(
      ParameterContext parameterContext, ExtensionContext extensionContext)
      throws ParameterResolutionException {

    if (shouldSupportVertx(parameterContext)) {
      return internalVertxResolve(parameterContext, extensionContext);
    }
    if (shouldSupportInjection(parameterContext)) {
      return resolveInjection(parameterContext, extensionContext);
    }
    if (wiremockExtension.supportsParameter(parameterContext, extensionContext)) {
      return wiremockExtension.resolveParameter(parameterContext, extensionContext);
    }

    return vertxExtension.resolveParameter(parameterContext, extensionContext);
  }

  @Override
  public void postProcessTestInstance(Object testInstance, ExtensionContext context) {
    wiremockExtension.postProcessTestInstance(testInstance, context);
  }

  @Override
  public void afterAll(ExtensionContext context) throws Exception {
    vertxExtension.afterAll(context);
    cleanupOurVertxes(context);
    wiremockExtension.afterAll(context);
  }

  @Override
  public void beforeEach(ExtensionContext context) throws Exception {
    vertxExtension.beforeEach(context);
  }

  @Override
  public void afterEach(ExtensionContext context) throws Exception {
    vertxExtension.afterEach(context);
    cleanupOurVertxes(context);
  }

  @Override
  public void afterTestExecution(ExtensionContext context) throws Exception {
    vertxExtension.afterTestExecution(context);
    cleanupOurVertxes(context);
  }

  @Override
  public void beforeTestExecution(ExtensionContext context) throws Exception {
    vertxExtension.beforeTestExecution(context);
  }

  @Override
  public void addToOverrides(Config config, List<JsonObject> overrides, String forReference) {
    if (config.hasPath(RANDOM_GEN_NAMESPACE)) {
      Config servicesConfig = config.getConfig(RANDOM_GEN_NAMESPACE);
      HashMap<String, Integer> servicePorts = new HashMap<>();

      // servicesConfig doesn't support
      Set<String> services = new HashSet<>(servicesConfig.root().keySet());

      // random port generation must be explicitly requested
      services.removeIf(s -> !servicesConfig.hasPath(s + "." + PORT));

      if (services.isEmpty()) {
        return;
      }

      try {
        referenceMapLock.writeLock().lock();

        services.forEach(s -> servicePorts.put(s, FreePortFinder.findFreeLocalPort()));

        JsonObject override = new JsonObject();

        servicePorts.forEach(
            (name, port) -> {
              override.put(name, ImmutableMap.of(PORT, port));
              referencePortMap.put(forReference + name, port);
            });

        overrides.add(new JsonObject().put("test", new JsonObject().put("random", override)));
      } finally {
        referenceMapLock.writeLock().unlock();
      }
    }
  }

  private Object resolveInjection(
      ParameterContext parameterContext, ExtensionContext extensionContext) {
    // need class name, method name, param name
    String forParam = checkAndGetParameterName(parameterContext);

    if (!StringUtils.endsWithIgnoreCase(forParam, PORT)) {
      throw new IllegalArgumentException(
          "Requirement: Variable name must end with 'port' for valid value injection");
    }

    // trim param name
    forParam = StringUtils.removeEndIgnoreCase(forParam, PORT);

    String reference = getClassName(extensionContext) + getMethodName(parameterContext) + forParam;

    try {
      referenceMapLock.readLock().lock();

      return referencePortMap.get(reference);
    } finally {
      referenceMapLock.readLock().unlock();
    }
  }

  private String checkAndGetParameterName(ParameterContext parameterContext) {
    String name = parameterContext.getParameter().getName();
    if (name.startsWith("arg")) {
      throw new IllegalStateException(
          "Please configure 'options.compilerArgs << \"-parameters\"', please check the README file.");
    }
    return name;
  }

  private boolean shouldSupportVertx(ParameterContext parameterContext) {
    Class<?> type = getType(parameterContext);
    return type.equals(io.vertx.reactivex.core.Vertx.class) || type.equals(Vertx.class);
  }

  private boolean shouldSupportInjection(ParameterContext parameterContext) {
    return getType(parameterContext).equals(Integer.class)
        && parameterContext.isAnnotated(RandomPort.class);
  }

  private Object internalVertxResolve(
      ParameterContext parameterContext, ExtensionContext extensionContext) {
    Class<?> type = getType(parameterContext);
    boolean isReactivex = (type == io.vertx.reactivex.core.Vertx.class);

    // create vertx obj with knotx config injection
    if (type == Vertx.class || isReactivex) {
      Vertx vertx = (Vertx) resolveVertx(isReactivex, parameterContext, extensionContext);

      List<String> knotxConfigs = resolveAnnotationConfig(parameterContext);

      String forClass = getClassName(extensionContext);
      String forMethod = getMethodName(parameterContext);

      // required when tests are executed in parallel
      // some map references go missing and need to be reconstructed
      wiremockExtension.addMissingInstanceServers(forClass, extensionContext);

      loadKnotxConfig(vertx, knotxConfigs, forClass, forMethod);

      if (isReactivex) {
        return new io.vertx.reactivex.core.Vertx(vertx);
      }
      return vertx;
    }

    throw new IllegalStateException("Please file a bug report, this shouldn't happen");
  }

  /**
   * Developer announcement: This method could be worse, but enables us to apply a whole chain of
   * different configurations taken from class, method, and parameter. User friendliness is a plus.
   */
  private List<String> resolveAnnotationConfig(ParameterContext parameter) {
    Executable executable = parameter.getDeclaringExecutable();

    KnotxApplyConfiguration classConfig =
        executable.getDeclaringClass().getAnnotation(KnotxApplyConfiguration.class);
    KnotxApplyConfiguration methodConfig = executable.getAnnotation(KnotxApplyConfiguration.class);
    KnotxApplyConfiguration parameterConfig =
        parameter.getParameter().getAnnotation(KnotxApplyConfiguration.class);

    List<KnotxApplyConfiguration> list = Arrays.asList(classConfig, methodConfig, parameterConfig);
    List<String> result = new LinkedList<>();

    for (KnotxApplyConfiguration config : list) {
      if (Objects.nonNull(config)) {
        Collections.addAll(result, config.value());
      }
    }

    return result;
  }

  private Object resolveVertx(
      boolean isReactivex, ParameterContext parameterContext, ExtensionContext extensionContext) {
    if (!isReactivex) {
      return vertxExtension.resolveParameter(parameterContext, extensionContext);
    }

    Store store = getStore(extensionContext);
    return store.getOrComputeIfAbsent(VERTX_INSTANCE_STORE_KEY, o -> Vertx.vertx());
  }

  private void cleanupOurVertxes(ExtensionContext extensionContext)
      throws TimeoutException, InterruptedException {
    Store store = getStore(extensionContext);

    if (store.get(VERTX_INSTANCE_STORE_KEY) == null) {
      return;
    }

    Vertx vertx = store.remove(VERTX_INSTANCE_STORE_KEY, Vertx.class);
    CompletableFuture<Void> toComplete = new CompletableFuture<>();

    vertx.close(
        ar -> {
          if (ar.failed()) {
            toComplete.completeExceptionally(ar.cause());
          } else {
            toComplete.complete(null);
          }
        });

    try {
      toComplete.get(DEFAULT_TIMEOUT_SECONDS, TimeUnit.SECONDS);
    } catch (ExecutionException e) {
      throw new VertxException(e);
    } catch (TimeoutException e) {
      throw new TimeoutException("Closing the Vertx context timed out");
    }
  }

  /** Load Knot.x config from given resource and apply it to Vertx instance */
  @SuppressWarnings("unchecked")
  private void loadKnotxConfig(Vertx vertx, List<String> paths, String forClass, String forMethod) {
    pathsCorrectnessGuard(paths);

    List<JsonObject> overrides = new ArrayList<>();
    Config fullConfig =
        new KnotxConcatConfigProcessor()
            .createHoconConfig(vertx.fileSystem(), createKnotxConcatConfig(paths, overrides));

    wiremockExtension.addToOverrides(fullConfig, overrides, forClass);
    this.addToOverrides(fullConfig, overrides, forClass + forMethod);

    CompletableFuture<Void> toComplete = new CompletableFuture<>();
    DeploymentOptions deploymentOptions = createDeploymentConfig(paths, overrides);

    try {
      final Class<? extends Verticle> knotxStarterVerticleClass =
          (Class<? extends Verticle>) Class.forName("io.knotx.launcher.KnotxStarterVerticle");

      vertx.deployVerticle(
          knotxStarterVerticleClass,
          deploymentOptions,
          ar -> {
            if (ar.succeeded()) {
              toComplete.complete(null);
            } else {
              toComplete.completeExceptionally(ar.cause());
            }
          });

      toComplete.get();
    } catch (InterruptedException | ExecutionException e) {
      throw new ParameterResolutionException("Couldn't create Knot.x configuration", e);
    } catch (ClassNotFoundException e) {
      throw new TestInstantiationException(
          "Couldn't find class KnotxStarterVerticle on the classpath", e);
    }
  }

  private void pathsCorrectnessGuard(List<String> paths) {
    if (paths.isEmpty()) {
      throw new IllegalArgumentException(
          "Missing @KnotxApplyConfiguration annotation with the path to configuration files");
    }

    paths.forEach(this::guardConfigFormat);
  }

  private DeploymentOptions createDeploymentConfig(List<String> paths, List<JsonObject> overrides) {
    ConfigRetrieverOptions retrieverOptions = new ConfigRetrieverOptions();

    JsonObject config = createKnotxConcatConfig(paths, overrides);

    retrieverOptions.addStore(
        new ConfigStoreOptions()
            .setType("json")
            .setFormat("knotx")
            .setOptional(false)
            .setConfig(config));

    JsonObject storesConfig = retrieverOptions.toJson();
    return new DeploymentOptions()
        .setConfig(new JsonObject().put("configRetrieverOptions", storesConfig));
  }

  private JsonObject createKnotxConcatConfig(List<String> paths, List<JsonObject> overrides) {
    return new JsonObject().put("paths", paths).put("overrides", overrides);
  }

  private void guardConfigFormat(String path) {
    String extension = path.substring(path.lastIndexOf('.') + 1);
    if (!HOCON_EXTENSION.equals(extension) && !JSON_EXTENSION.equals(extension)) {
      throw new IllegalArgumentException(
          "Configuration file format not supported for path '" + path + "'");
    }
  }
}
