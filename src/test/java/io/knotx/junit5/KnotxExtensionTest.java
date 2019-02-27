package io.knotx.junit5;

import static io.restassured.RestAssured.given;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

@ExtendWith(KnotxExtension.class)
@KnotxApplyConfiguration({"config/example_random_config.conf", "config/modules_config.conf" })
class KnotxExtensionTest {

  private static final String ANY_ENDPOINT = "/any";

  @Test
  @DisplayName("Expect response from HTTP server working on port from random section.")
  void callServerWithRandomPortFromRandomSection(io.vertx.reactivex.core.Vertx vertx,
      @RandomPort Integer globalServerPort) {
    // @formatter:off
    given().
        port(globalServerPort).
    when().
      get(ANY_ENDPOINT).
    then().assertThat().
        statusCode(200);
    // @formatter:on
  }

  @Test
  @DisplayName("Expect null when no random section defined.")
  @KnotxApplyConfiguration("config/modules_wiremock_config.conf")
  void callServerWithRandomPortFromWiremockSection(io.vertx.reactivex.core.Vertx vertx,
      @RandomPort Integer wGlobalServerPort) {
    Assertions.assertNull(wGlobalServerPort);
  }

}
