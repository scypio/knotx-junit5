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
package io.knotx.junit5.assertions;

import static java.lang.Character.isWhitespace;
import static org.junit.jupiter.api.Assertions.assertEquals;

public final class KnotxAssertions {

  /** Util class */
  private KnotxAssertions() {}

  public static void assertEqualsIgnoreWhitespace(String expected, String actual) {
    assertEquals(stripSpace(expected), stripSpace(actual));
  }

  private static String stripSpace(String toBeStripped) {
    final StringBuilder result = new StringBuilder();
    boolean lastWasSpace = true;
    for (int i = 0; i < toBeStripped.length(); i++) {
      char c = toBeStripped.charAt(i);
      if (isWhitespace(c)) {
        if (!lastWasSpace) {
          result.append(' ');
        }
        lastWasSpace = true;
      } else {
        result.append(c);
        lastWasSpace = false;
      }
    }
    return result.toString().trim();
  }
}
