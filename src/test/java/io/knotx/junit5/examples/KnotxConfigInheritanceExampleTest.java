package io.knotx.junit5.examples;

import io.knotx.junit5.KnotxApplyConfiguration;
import io.knotx.junit5.KnotxExtension;
import io.vertx.reactivex.core.Vertx;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

@ExtendWith(KnotxExtension.class)
@KnotxApplyConfiguration("example_config.conf")
public class KnotxConfigInheritanceExampleTest {

  @Test
  @KnotxApplyConfiguration("method_level_config.conf")
  public void vertxWithParam(@KnotxApplyConfiguration("param_level_config.conf") Vertx vertx) {
    // config content from param level will override method and class level
  }

}
