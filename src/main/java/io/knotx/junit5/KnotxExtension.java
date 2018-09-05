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

import com.google.common.collect.Lists;
import io.knotx.junit5.wiremock.KnotxWiremock;
import io.knotx.junit5.wiremock.KnotxWiremockExtension;
import io.knotx.launcher.KnotxStarterVerticle;
import io.vertx.config.ConfigRetrieverOptions;
import io.vertx.config.ConfigStoreOptions;
import io.vertx.core.DeploymentOptions;
import io.vertx.core.Vertx;
import io.vertx.core.VertxException;
import io.vertx.core.json.JsonObject;
import io.vertx.junit5.VertxExtension;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import org.apache.commons.lang3.StringUtils;
import org.junit.jupiter.api.extension.AfterAllCallback;
import org.junit.jupiter.api.extension.AfterEachCallback;
import org.junit.jupiter.api.extension.AfterTestExecutionCallback;
import org.junit.jupiter.api.extension.BeforeAllCallback;
import org.junit.jupiter.api.extension.BeforeEachCallback;
import org.junit.jupiter.api.extension.BeforeTestExecutionCallback;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.api.extension.ExtensionContext.Store;
import org.junit.jupiter.api.extension.ParameterContext;
import org.junit.jupiter.api.extension.ParameterResolutionException;
import org.junit.jupiter.api.extension.ParameterResolver;

/**
 * Support for field and parameter injection for Knot.x tests <br>
 * <br>
 * Injects and manages instances of:
 *
 * <ul>
 * <li>{@linkplain io.vertx.core.Vertx}, with Knot.x config injection via {@linkplain
 * KnotxApplyConfiguration}
 * <li>{@linkplain io.vertx.reactivex.core.Vertx}, same as above
 * <li>{@linkplain com.github.tomakehurst.wiremock.WireMockServer} when annotated with {@linkplain
 * KnotxWiremock}
 * </ul>
 */
public class KnotxExtension extends KnotxBaseExtension
    implements ParameterResolver,
    BeforeEachCallback,
    AfterEachCallback,
    AfterTestExecutionCallback,
    BeforeTestExecutionCallback,
    AfterAllCallback,
    BeforeAllCallback {

  private static final long DEFAULT_TIMEOUT_SECONDS = 30;
  private static final String VERTX_INSTANCE_STORE_KEY = "VertxInstance";

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
    return shouldSupportType(parameterContext);
  }

  @Override
  public Object resolveParameter(
      ParameterContext parameterContext, ExtensionContext extensionContext)
      throws ParameterResolutionException {

    if (shouldSupportType(parameterContext)) {
      return internalResolve(parameterContext, extensionContext);
    }
    if (wiremockExtension.supportsParameter(parameterContext, extensionContext)) {
      return wiremockExtension.resolveParameter(parameterContext, extensionContext);
    }

    return vertxExtension.resolveParameter(parameterContext, extensionContext);
  }

  @Override
  public void beforeAll(ExtensionContext context) throws Exception {
    wiremockExtension.beforeAll(context);
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
    wiremockExtension.afterEach(context);
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

  private boolean shouldSupportType(ParameterContext parameterContext) {
    Class<?> type = getType(parameterContext);

    return type.equals(io.vertx.reactivex.core.Vertx.class) || type.equals(Vertx.class);
  }

  private Object internalResolve(
      ParameterContext parameterContext, ExtensionContext extensionContext) {
    Class<?> type = getType(parameterContext);
    boolean isReactivex = (type == io.vertx.reactivex.core.Vertx.class);

    // create vertx obj with knotx config injection
    if (type == Vertx.class || isReactivex) {
      Vertx vertx = (Vertx) resolveVertx(isReactivex, parameterContext, extensionContext);
      KnotxApplyConfiguration knotxConfig;

      knotxConfig =
          parameterContext
              .findAnnotation(KnotxApplyConfiguration.class)
              .orElseGet(
                  () ->
                      parameterContext
                          .getDeclaringExecutable()
                          .getAnnotation(KnotxApplyConfiguration.class));

      loadKnotxConfig(vertx, knotxConfig);

      if (isReactivex) {
        return new io.vertx.reactivex.core.Vertx(vertx);
      }
      return vertx;
    }

    throw new IllegalStateException("Please file a bug report, this shouldn't happen");
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

  /**
   * Load Knot.x config from given resource and apply it to Vertx instance
   */
  private void loadKnotxConfig(Vertx vertx, KnotxApplyConfiguration knotxConfig) {
    if (knotxConfig == null || StringUtils.isBlank(knotxConfig.value())) {
      throw new IllegalArgumentException(
          "Missing @KnotxApplyConfiguration annotation with the path to configuration JSON");
    }

    CompletableFuture<Void> toComplete = new CompletableFuture<>();

    vertx.deployVerticle(
        KnotxStarterVerticle.class.getName(),
        createConfig(knotxConfig.value()),
        ar -> {
          if (ar.succeeded()) {
            toComplete.complete(null);
          } else {
            toComplete.completeExceptionally(ar.cause());
          }
        });

    try {
      toComplete.get();
    } catch (ExecutionException ignore) {
    } catch (InterruptedException e) {
      throw new ParameterResolutionException("Couldn't create Knot.x configuration", e);
    }
  }

  private DeploymentOptions createConfig(String path) {
    return new DeploymentOptions()
        .setConfig(
            new JsonObject()
                .put(
                    "configRetrieverOptions",
                    new ConfigRetrieverOptions()
                        .setStores(
                            Lists.newArrayList(
                                new ConfigStoreOptions()
                                    .setType("file")
                                    .setFormat(getConfigFormat(path))
                                    .setConfig(new JsonObject().put("path", path))))
                        .toJson()));
  }

  private String getConfigFormat(String path) {
    String extension = path.substring(path.lastIndexOf('.') + 1);
    if ("conf".equals(extension)) {
      return "hocon";
    } else if ("json".equals(extension)) {
      return "json";
    } else {
      throw new IllegalArgumentException("Configuration file format not supported!");
    }
  }
}
