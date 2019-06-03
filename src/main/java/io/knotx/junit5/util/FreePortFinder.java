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

public class FreePortFinder {

  /** Roll a port number and ensure it's available */
  public static int findFreeLocalPort() {
    int port;
    do {
      // IANA Ephemeral Port range
      port = RandomUtils.nextInt(49152, 65535);
    } while (!isFree(port));

    return port;
  }

  private synchronized static boolean isFree(int port) {
    try (ServerSocket ignored = new ServerSocket(port)) {
      // noop
    } catch (IOException e) {
      return false;
    }
    return true;
  }
}
