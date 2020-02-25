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

import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import java.util.Iterator;

public final class KnotxAssertions {

  /** Util class */
  private KnotxAssertions() {}

  public static void assertEqualsIgnoreWhitespace(String expected, String actual) {
    assertEquals(stripSpace(expected), stripSpace(actual));
  }

  public static void assertJsonEquals(JsonObject expected, JsonObject current) {
    assertJsonEquals(expected, current, "");
  }

  private static void assertJsonEquals(JsonObject expected, JsonObject current, String path) {
    expected.getMap().forEach((key, value) -> {
      String nextPath = path.equals("") ? key : path + "." + key;
      if (value instanceof JsonObject) {
        JsonObject currentValue = current.getJsonObject(key);
        assertJsonEquals((JsonObject) value, currentValue, nextPath);
      } else if (value instanceof JsonArray) {
        JsonArray expectedArray = (JsonArray) value;
        JsonArray currentArray = current.getJsonArray(key);
        assertJsonArrayEquals(expectedArray, currentArray, nextPath);
      } else {
        Object currentValue = current.getValue(key);
        assertEquals(value, currentValue, "Invalid JSON [" + nextPath + "] value!");
      }
    });
  }

  private static void assertJsonArrayEquals(JsonArray expected, JsonArray current, String path) {
    String nextPath = path + ".[]";
    if (expected.size() != current.size()) {
      throw new AssertionError(
          "Arrays [" + nextPath + "] have different lengths <" + expected.size() + " ! = " + current
              .size() + ">");
    }
    Iterator<Object> expectedIterator = expected.iterator();
    Iterator<Object> currentIterator = current.iterator();
    while (expectedIterator.hasNext()) {
      Object expectedValue = expectedIterator.next();
      Object currentValue = currentIterator.next();
      if (expectedValue instanceof JsonObject) {
        assertJsonEquals((JsonObject) expectedValue, (JsonObject) currentValue, nextPath);
      } else if (expectedValue instanceof JsonArray) {
        assertJsonArrayEquals((JsonArray) expectedValue, (JsonArray) currentValue, nextPath);
      } else {
        assertEquals(expectedValue, currentValue, "Invalid JSON [" + nextPath + "] value!");
      }
    }
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
