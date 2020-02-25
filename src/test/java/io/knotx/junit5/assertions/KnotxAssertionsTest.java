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

import static io.knotx.junit5.assertions.KnotxAssertions.assertEqualsIgnoreWhitespace;
import static io.knotx.junit5.assertions.KnotxAssertions.assertJsonEquals;
import static org.junit.jupiter.api.Assertions.fail;

import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class KnotxAssertionsTest {

  // ****************************
  // assertEqualsIgnoreWhitespace
  // ****************************

  @Test
  void stringThatDifferSpaceAreEqual() {
    final String actual = " a b c ";
    final String expected = "a b c";
    assertEqualsIgnoreWhitespace(actual, expected);
  }

  @Test
  void stringsThatDifferTabAreEqual() {
    final String actual = "\ta\tb\tc\t";
    final String expected = "a b c";
    assertEqualsIgnoreWhitespace(actual, expected);
  }

  @Test
  void stringsThatDifferNewlineAreEqual() {
    final String actual = "\na\nb\nc\n";
    final String expected = "a b c";
    assertEqualsIgnoreWhitespace(actual, expected);
  }

  // ****************************
  // assertJsonEquals
  // ****************************

  @DisplayName("Empty Jsons are equal.")
  @Test
  void emptyJSONs() {
    assertJsonEquals(
        new JsonObject(),
        new JsonObject());
  }

  @DisplayName("Jsons are equal.")
  @Test
  void flatJSONs() {
    assertJsonEquals(
        new JsonObject().put("A", 1),
        new JsonObject().put("A", 1));
  }

  @DisplayName("Missing entry in Json ends with failure.")
  @Test
  void missingEntry() {
    String missingKey = "A";
    shouldFail(
        new JsonObject().put(missingKey, 1),
        new JsonObject(),
        "[" + missingKey + "]");
  }

  @DisplayName("Different entry in Json ends with failure.")
  @Test
  void differentEntry() {
    String differentKey = "A";
    shouldFail(
        new JsonObject().put(differentKey, 1),
        new JsonObject().put(differentKey, 2),
        "[" + differentKey + "]");
  }

  @DisplayName("Ignore entries not defined in expected Json.")
  @Test
  void ignoreOtherEntriesJSONs() {
    assertJsonEquals(
        new JsonObject().put("A", 1),
        new JsonObject().put("A", 1).put("ignored", true));
  }

  @DisplayName("Json(Json) are equal.")
  @Test
  void nestedJSONs() {
    assertJsonEquals(
        new JsonObject().put("A", new JsonObject().put("B", true)),
        new JsonObject().put("A", new JsonObject().put("B", true)));
  }

  @DisplayName("Missing entry in Json(Json) ends with failure.")
  @Test
  void missingNestedEntry() {
    shouldFail(
        new JsonObject().put("A", new JsonObject().put("B", true)),
        new JsonObject().put("A", new JsonObject()),
        "[A.B]");
  }

  @DisplayName("Different entry in Json(Json) ends with failure.")
  @Test
  void differentNestedEntry() {
    shouldFail(
        new JsonObject().put("A", new JsonObject().put("B", true)),
        new JsonObject().put("A", new JsonObject().put("B", 1)),
        "[A.B]");
  }

  @DisplayName("Json(Array) are equal.")
  @Test
  void arrayInJson() {
    assertJsonEquals(
        new JsonObject().put("A", new JsonArray().add("B")),
        new JsonObject().put("A", new JsonArray().add("B")));
  }

  @DisplayName("Missing entry in JSON(Array) ends with failure.")
  @Test
  void missingArrayEntry() {
    shouldFail(
        new JsonObject().put("A", new JsonArray().add("B")),
        new JsonObject().put("A", new JsonArray()),
        "[A.[]]");
  }

  @DisplayName("Different entry in Json(Array) ends with failure.")
  @Test
  void differentArrayEntry() {
    shouldFail(
        new JsonObject().put("A", new JsonArray().add("B")),
        new JsonObject().put("A", new JsonArray().add("C")),
        "[A.[]]");
  }

  @DisplayName("Json(Array(Array) are equal.")
  @Test
  void arrayInArrayInJson() {
    assertJsonEquals(
        new JsonObject().put("A", new JsonArray().add(new JsonArray().add("B"))),
        new JsonObject().put("A", new JsonArray().add(new JsonArray().add("B"))));
  }

  @DisplayName("Missing entry in Json(Array(Array)) ends with failure.")
  @Test
  void missingArrayInArrayEntry() {
    shouldFail(
        new JsonObject().put("A", new JsonArray().add(new JsonArray().add("B"))),
        new JsonObject().put("A", new JsonArray().add(new JsonArray())),
        "[A.[].[]]");
  }

  @DisplayName("Different entry in Json(Array(Array)) ends with failure.")
  @Test
  void differentArrayInArrayEntry() {
    shouldFail(
        new JsonObject().put("A", new JsonArray().add(new JsonArray().add("B"))),
        new JsonObject().put("A", new JsonArray().add(new JsonArray().add("C"))),
        "[A.[].[]]");
  }

  @DisplayName("Json(Array(Json)) are equal.")
  @Test
  void jsonInArrayInJson() {
    assertJsonEquals(
        new JsonObject().put("A", new JsonArray().add(new JsonObject().put("B", 1))),
        new JsonObject().put("A", new JsonArray().add(new JsonObject().put("B", 1))));
  }

  @DisplayName("Missing entry in Json(Array(Json)) ends with failure.")
  @Test
  void missingJsonInArrayEntry() {
    shouldFail(
        new JsonObject().put("A", new JsonArray().add(new JsonObject().put("B", 1))),
        new JsonObject().put("A", new JsonArray().add(new JsonObject())),
        "[A.[].B]");
  }

  @DisplayName("Different entry in Json(Array(Json)) ends with failure.")
  @Test
  void differentJsonInArrayEntry() {
    shouldFail(
        new JsonObject().put("A", new JsonArray().add(new JsonObject().put("B", 1))),
        new JsonObject().put("A", new JsonArray().add(new JsonObject().put("B", 2))),
        "[A.[].B]");
  }

  private void shouldFail(JsonObject expected, JsonObject current, String expectedPath) {
    try {
      assertJsonEquals(expected, current);
    } catch (AssertionError error) {
      if (!error.getMessage().contains(expectedPath)) {
        throw new AssertionError(
            "Path " + expectedPath + " not found in [" + error.getMessage() + "]");
      }
      return;
    }
    fail("Should fail.");
  }
}