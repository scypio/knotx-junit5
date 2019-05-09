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
package io.knotx.junit5.utils;

import io.vertx.core.Context;
import io.vertx.core.Future;
import io.vertx.core.Verticle;
import io.vertx.core.Vertx;
import io.vertx.core.http.HttpServer;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.Router;

public class TestConfigurationHttpServer implements Verticle {

  private Vertx vertx;
  private HttpServer httpServer;
  private Integer port;
  private JsonObject config;

  @Override
  public void init(Vertx vertx, Context context) {
    this.vertx = vertx;
    port = context.config().getInteger("port");
    config = context.config().getJsonObject("extensionConfig");
  }

  @Override
  public void start(Future<Void> future) {
    Router router = Router.router(vertx);
    router.route()
        .handler(
            routingContext -> routingContext.response()
                .putHeader("content-type", "application/json")
                .end(config.encode()));

    try {
      httpServer = vertx.createHttpServer();
      httpServer
          .requestHandler(router)
          .listen(port, server -> future.complete());
    } catch (Exception e) {
      future.fail(e);
    }
  }

  @Override
  public void stop(Future<Void> future) {
    httpServer.close(a -> future.complete());
  }

  @Override
  public Vertx getVertx() {
    return vertx;
  }
}
