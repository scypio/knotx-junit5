package io.knotx.junit5.wiremock;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.anyUrl;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static io.knotx.junit5.wiremock.KnotxWiremockExtension.stubForServer;
import static io.restassured.RestAssured.given;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

import com.github.tomakehurst.wiremock.WireMockServer;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.TestInstance.Lifecycle;
import org.junit.jupiter.api.extension.ExtendWith;

@ExtendWith(KnotxWiremockExtension.class)
@TestInstance(Lifecycle.PER_CLASS)
class KnotxWiremockExtensionTest {

  private static final int STATIC_PORT = 1234;
  private static final int STATIC_LOCAL_PORT = 2345;
  private static final String SERVER_URL = "/service/endpoint.json";

  @KnotxWiremock(port = STATIC_PORT)
  private WireMockServer staticPortServer;

  @KnotxWiremock
  private WireMockServer dynamicPortServer;

  @BeforeAll
  void initMocks() {
    stubForServer(staticPortServer,
        get(anyUrl())
            .willReturn(
                aResponse()
                    .withStatus(200)
                    .withHeader("scope", "class")
            ));
    stubForServer(dynamicPortServer,
        get(anyUrl())
            .willReturn(
                aResponse()
                    .withStatus(200)
                    .withHeader("scope", "class")
            ));
  }

  @Test
  @DisplayName("Expect field-injected server when call endpoint with static port.")
  void serverWithStaticPort() {
    // @formatter:off
    given().
        port(STATIC_PORT).
    when().
      get(SERVER_URL).
    then().assertThat().
        statusCode(200).
        header("scope", "class");
    // @formatter:on
  }

  @Test
  @DisplayName("Expect method-injected server call endpoint with static local port.")
  void serverWithStaticPort(@KnotxWiremock(port = STATIC_LOCAL_PORT) WireMockServer server) {
    stubForServer(server,
        get(anyUrl())
            .willReturn(
                aResponse()
                    .withStatus(200)
                    .withHeader("scope", "method")));
    // @formatter:off
    given().
        port(STATIC_LOCAL_PORT).
    when().
      get(SERVER_URL).
    then().assertThat().
        statusCode(200).
        header("scope", "method");
    // @formatter:on
  }

  @Test
  @DisplayName("Expect field-injected server when injected server has the same name as field one.")
  void overloadServer(@KnotxWiremock WireMockServer staticPortServer) {
    // @formatter:off
    given().
        port(staticPortServer.port()).
    when().
      get(SERVER_URL).
    then().assertThat().
        statusCode(200).
        header("scope", "class");
    // @formatter:on
  }

  @Test
  @DisplayName("Expect method-injected server when injected server has static port and the same name as field one.")
  void overloadServerWithStaticPort(
      @KnotxWiremock(port = STATIC_LOCAL_PORT) WireMockServer staticPortServer) {
    stubForServer(staticPortServer,
        get(anyUrl())
            .willReturn(
                aResponse()
                    .withStatus(200)
                    .withHeader("scope", "method")));
    // @formatter:off
    given().
        port(STATIC_LOCAL_PORT).
    when().
      get(SERVER_URL).
    then().assertThat().
        statusCode(200).
        header("scope", "method");
    // @formatter:on
  }

  @Test
  @DisplayName("Expect field-injected server port when injected server has the same name as field one.")
  void overloadServerWithPortInjection(@KnotxWiremock Integer staticPortServer) {
    // @formatter:off
    given().
        port(staticPortServer).
    when().
      get(SERVER_URL).
    then().assertThat().
        statusCode(200).
        header("scope", "class");
    // @formatter:on
  }

  @Test
  @DisplayName("Expect method-injected server when injected server has static port and the same name as field one.")
  void overloadServerWithStaticPortInjection(
      @KnotxWiremock(port = STATIC_LOCAL_PORT) Integer staticPortServer) {
    // @formatter:off
    given().
        port(staticPortServer).
    when().
      get(SERVER_URL).
    then().assertThat().
        statusCode(404);
    // @formatter:on
  }

  @Test
  @DisplayName("Expect method-injected server when call server with random port.")
  void serverWithDynamicPort() {
    assertNotEquals(0, dynamicPortServer.port());
    // @formatter:off
    given().
        port(dynamicPortServer.port()).
    when().
      get(SERVER_URL).
    then().assertThat().
        statusCode(200).
        header("scope", "class");
    // @formatter:on
  }

}
