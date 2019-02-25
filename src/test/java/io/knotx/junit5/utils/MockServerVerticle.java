package io.knotx.junit5.utils;

import io.vertx.core.Context;
import io.vertx.core.Future;
import io.vertx.core.Verticle;
import io.vertx.core.Vertx;
import io.vertx.core.http.HttpServer;
import io.vertx.ext.web.Router;

public class MockServerVerticle implements Verticle {

  private Vertx vertx;
  private HttpServer httpServer;
  private Integer port;

  @Override
  public void init(Vertx vertx, Context context) {
    this.vertx = vertx;
    port = context.config().getInteger("port");
  }

  @Override
  public void start(Future<Void> future) throws Exception {
    Router router = Router.router(vertx);

    router.route().handler(
        routingContext -> routingContext.response().putHeader("content-type", "text/html")
            .end("End!"));

    httpServer = vertx.createHttpServer();
    httpServer
        .requestHandler(router)
        .listen(port, server -> future.complete());
  }

  @Override
  public void stop(Future<Void> future) throws Exception {
    httpServer.close(a -> future.complete());
  }

  @Override
  public Vertx getVertx() {
    return vertx;
  }
}
