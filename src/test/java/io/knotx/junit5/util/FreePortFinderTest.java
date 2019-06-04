package io.knotx.junit5.util;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.net.ServerSocket;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

class FreePortFinderTest {

  @Test
  void forRandomPort_mustBeAvailable() {
    int port = FreePortFinder.findFreeLocalPort();

    assertTrue(FreePortFinder.available(port));
  }

  @Test
  @Disabled("Broken on WSL")
  void forRandomPort_whenUsed_mustNotBeAvailable() {
    int port = FreePortFinder.findFreeLocalPort();

    assertDoesNotThrow(
        () -> {
          try (ServerSocket ignore = new ServerSocket(port, 50)) {
            assertFalse(FreePortFinder.available(port));
          }
        });
  }
}
