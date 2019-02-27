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
