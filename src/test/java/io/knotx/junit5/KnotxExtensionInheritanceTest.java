package io.knotx.junit5;

import io.knotx.junit5.wiremock.KnotxWiremock;
import io.vertx.reactivex.core.Vertx;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

@ExtendWith(KnotxExtension.class)
@KnotxApplyConfiguration({"example_wiremock_config.conf", "modules_config.conf" })
public class KnotxExtensionInheritanceTest {

  @Test
  // TODO @DisplayName()
  void configurationClassScope(@KnotxWiremock Integer minimalRequiredService) {
    Assertions.assertEquals(Integer.valueOf(3000), minimalRequiredService);
  }

  @Test
  @KnotxApplyConfiguration("method_level_config.conf")
  void configurationMethodScope(@KnotxWiremock Integer mockService) {
    Assertions.assertEquals(new Integer(4001), mockService);
  }

  @Test
  void configurationParamScope(@KnotxApplyConfiguration("param_level_config.conf") Vertx vertx,
      @KnotxWiremock Integer queryOnlyRepository) {
    Assertions.assertEquals(new Integer(5001), queryOnlyRepository);
  }

  @Test
  @KnotxApplyConfiguration("method_level_config.conf")
  void configurationMixedScope(@KnotxApplyConfiguration("param_level_config.conf") Vertx vertx,
      @KnotxWiremock Integer minimalRequiredService,
      @KnotxWiremock Integer mockService,
      @KnotxWiremock Integer queryOnlyRepository) {
    Assertions.assertEquals(Integer.valueOf(3000), minimalRequiredService);
    Assertions.assertEquals(new Integer(4001), mockService);
    Assertions.assertEquals(new Integer(5001), queryOnlyRepository);
  }

  @Test
  @KnotxApplyConfiguration("method_level_config.conf")
  void configurationMethodParamScope(
      @KnotxApplyConfiguration("param_level_config.conf") Vertx vertx,
      @KnotxWiremock Integer allPropertiesService) {
    Assertions.assertEquals(Integer.valueOf(6002), allPropertiesService);
  }

}
