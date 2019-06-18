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
package io.knotx.junit5.util;

import java.io.IOException;
import java.net.ServerSocket;
import org.apache.commons.lang3.RandomUtils;

public final class FreePortFinder {

  /** Util class */
  private FreePortFinder() {
  }

  /**
   * Roll a port number and ensure it's available
   *
   * @return port number
   */
  public static int findFreeLocalPort() {
    int port;
    do {
      // IANA Ephemeral Port range
      port = RandomUtils.nextInt(49152, 65535);
    } while (!available(port));

    return port;
  }

  public synchronized static boolean available(int port) {
    try (ServerSocket ignored = new ServerSocket(port, 1)) {
      // noop
    } catch (IOException e) {
      return false;
    }
    return true;
  }
}
