package io.knotx.junit5;

import com.github.tomakehurst.wiremock.WireMockServer;
import io.knotx.junit5.wiremock.KnotxWiremock;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

@Disabled
@ExtendWith(KnotxExtension.class)
class KnotxExtensionTest {

  @Test
  @DisplayName("Exception when applying configuration to test method without Vert.x instance.")
  void noApplyConfigurationWithoutCoreVertx() {
  }

  @Test
  @DisplayName("Exception when applying configuration to test method without Vert.x instance.")
  @KnotxApplyConfiguration("simple.conf")
  void applyConfigurationWithoutCoreVertx() {
    Assertions.fail("Test should not init properly");
  }

  @Test
  @DisplayName("Success when applying configuration to test method with Vert.x instance.")
  @KnotxApplyConfiguration("simple.conf")
  void applyConfigurationWithVertx(io.vertx.core.Vertx vertx) {
    Assertions.assertTrue(true);
  }

  @Test
  @DisplayName("Success when applying configuration to test method with Vert.x instance Rx delegate.")
  @KnotxApplyConfiguration("simple.conf")
  void applyConfigurationWithVertx(io.vertx.reactivex.core.Vertx vertx) {
    Assertions.assertTrue(true);
  }

  @Test
  @DisplayName("Exception when applying configuration to test method with Vert.x instance RxJava 1 delegate.")
  @KnotxApplyConfiguration("simple.conf")
  void applyConfigurationWithVertx(io.vertx.rxjava.core.Vertx vertx) {
    Assertions.fail("Test should not init properly");
  }

  @Test
  @DisplayName("Static port when applying configuration.")
  @KnotxApplyConfiguration("simple.conf")
  void loadConfigurationWithStaticPort(io.vertx.reactivex.core.Vertx vertx,
      @KnotxWiremock WireMockServer staticPortServer) {
    Assertions.assertEquals(1234, staticPortServer.port());
  }

  @Test
  @DisplayName("Dynamic port when applying configuration with '0' port value.")
  @KnotxApplyConfiguration("simple.conf")
  void loadConfigurationWithDynamicPort(io.vertx.reactivex.core.Vertx vertx,
      @KnotxWiremock WireMockServer dynamicWiremockPortServer) {
    Assertions.assertNotEquals(0, dynamicWiremockPortServer.port());
  }

  @Test
  @DisplayName("Dynamic port when applying configuration with random section.")
  @KnotxApplyConfiguration("simple.conf")
  void loadConfigurationWithRandomSection(io.vertx.reactivex.core.Vertx vertx,
      @KnotxWiremock WireMockServer dynamicRandomPortServer) {
    Assertions.assertNotEquals(4000, dynamicRandomPortServer.port());
  }

//  @Test
//  @DisplayName("Exception when @KnotxWiremockPort is not Integer")
//  @KnotxApplyConfiguration("simple.conf")
//  void loadConfigurationNonIntegerPortInjection(io.vertx.reactivex.core.Vertx vertx,
//      @KnotxWiremockPort("dynamicPortServer") String port) {
//    Assertions.fail("Test should not init properly");
//  }
//
//  @Test
//  @DisplayName("Exception when server name from @KnotxWiremockPort not found")
//  @KnotxApplyConfiguration("simple.conf")
//  void loadConfigurationWithInvalidServerName(io.vertx.reactivex.core.Vertx vertx,
//      @KnotxWiremockPort("invalidPortServer") String port) {
//    Assertions.fail("Test should not init properly");
//  }
//
//  @Test
//  @DisplayName("Dynamic port when 'test.random' section server.")
//  @KnotxApplyConfiguration("simple.conf")
//  void loadConfigurationWithDynamicPortFromRandomSection(io.vertx.reactivex.core.Vertx vertx,
//      @KnotxWiremockPort("dynamicRandomPortServer") Integer port) {
//    Assertions.assertNotEquals(0, port);
//  }
//
//  @Test
//  @DisplayName("Dynamic port when 'test.wiremock' section server.")
//  @KnotxApplyConfiguration("simple.conf")
//  void loadConfigurationWithDynamicPortFromWiremockSection(io.vertx.reactivex.core.Vertx vertx,
//      @KnotxWiremockPort("dynamicWiremockPortServer") Integer port) {
//    Assertions.assertNotEquals(0, port);
//  }
//
//  @Test
//  @DisplayName("Static port when applying configuration with 'test.wiremock' section.")
//  @KnotxApplyConfiguration("simple.conf")
//  void loadConfigurationWithStaticPortFromWiremocSection(io.vertx.reactivex.core.Vertx vertx,
//      @KnotxWiremockPort("staticPortServer") Integer port) {
//    Assertions.assertNotEquals(1234, port);
//  }
//
//  @Test
//  @DisplayName("Exception when two servers with the same name defined")
//  @KnotxApplyConfiguration("simple.conf")
//  void loadConfigurationWithRandomSection(io.vertx.reactivex.core.Vertx vertx,
//      @KnotxWiremockPort("dynamicPortServer") Integer port) {
//    Assertions.fail("Test should not init properly");
//  }
//
//  @Test
//  @DisplayName("Exception when two servers with the same name defined")
//  @KnotxApplyConfiguration("modules.conf")
//  void loadModulesConfigurationWithRandomSection(io.vertx.reactivex.core.Vertx vertx,
//      @KnotxWiremockPort("dynamicPortServer") Integer port) {
//    Assertions.fail("Test should not init properly");
//  }

}
