package io.knotx.junit5;

import static io.vertx.config.impl.spi.PropertiesConfigProcessor.closeQuietly;

import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;
import com.typesafe.config.ConfigRenderOptions;
import io.vertx.config.spi.ConfigProcessor;
import io.vertx.core.AsyncResult;
import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import java.io.Reader;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.Collections;

public class HoconConcatConfigProcessor implements ConfigProcessor {

  @Override
  public String name() {
    return "knotx";
  }

  @Override
  public void process(
      Vertx vertx,
      JsonObject configuration,
      Buffer input,
      Handler<AsyncResult<JsonObject>> handler) {
    vertx.executeBlocking(
        future -> {
          JsonArray paths = configuration.getJsonArray("paths");
          JsonObject overrides = configuration.getJsonObject("overrides");

          // readers are stored in order of overriding - base first, overrides last
          ArrayList<Reader> readers = new ArrayList<>(4);
          try {
            // load user configurations
            for (Object o : paths) {
              String path = String.valueOf(o);
              String value = vertx.fileSystem().readFileBlocking(path).toString();

              readers.add(new StringReader(value));
            }

            // add overrides
            readers.add(new StringReader(overrides.encode()));

            // put overrides first
            Collections.reverse(readers);

            Config fullConfig = ConfigFactory.empty();

            for (Reader reader : readers) {
              fullConfig = fullConfig.withFallback(ConfigFactory.parseReader(reader));
            }

            // and render everything
            fullConfig = fullConfig.resolve();
            ConfigRenderOptions options =
                ConfigRenderOptions.concise().setJson(true).setComments(false).setFormatted(false);
            String output = fullConfig.root().render(options);

            future.complete(new JsonObject(output));
          } catch (Exception e) {
            future.fail(e);
          } finally {
            for (Reader reader : readers) {
              closeQuietly(reader);
            }
          }
        },
        handler);
  }
}
