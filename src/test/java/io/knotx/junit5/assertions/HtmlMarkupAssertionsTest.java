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

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

import io.knotx.junit5.util.FileReader;
import org.junit.jupiter.api.Test;

class HtmlMarkupAssertionsTest {

  @Test
  void forIdenticalMarkup_shouldPass() {
    String markup = FileReader.readTextSafe("html/first.html");

    assertDoesNotThrow(() -> HtmlMarkupAssertions.assertHtmlBodyMarkupsEqual(markup, markup));
  }

  @Test
  void forDifferentMarkup_shouldFail() {
    String first = FileReader.readTextSafe("html/first.html");
    String second = FileReader.readTextSafe("html/second.html");

    assertThrows(
        AssertionError.class, () -> HtmlMarkupAssertions.assertHtmlBodyMarkupsEqual(first, second));
  }

  @Test
  void forDifferentTextInMarkup_shouldFail() {
    String first = FileReader.readTextSafe("html/first.html");
    // removing all dots - this way we don't touch the structure
    String second = first.replaceAll("\\.", "");

    assertThrows(
        AssertionError.class, () -> HtmlMarkupAssertions.assertHtmlBodyMarkupsEqual(first, second));
  }
}
