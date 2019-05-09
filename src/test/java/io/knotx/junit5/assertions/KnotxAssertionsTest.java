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

import org.junit.jupiter.api.Test;

class KnotxAssertionsTest {

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
}