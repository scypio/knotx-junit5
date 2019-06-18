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

import java.nio.charset.StandardCharsets;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Entities;
import org.jsoup.parser.Parser;
import org.junit.jupiter.api.Assertions;

public final class HtmlMarkupAssertions {

  private static final Document.OutputSettings OUTPUT_SETTINGS =
      new Document.OutputSettings()
          .escapeMode(Entities.EscapeMode.xhtml)
          .indentAmount(2)
          .prettyPrint(true);

  /** Util class */
  private HtmlMarkupAssertions() {}

  public static void assertHtmlBodyMarkupsEqual(String expectedHtml, String actualHtml) {
    assertHtmlBodyMarkupsEqual(expectedHtml, actualHtml, null);
  }

  public static void assertHtmlBodyMarkupsEqual(
      String expectedHtml, String actualHtml, String message) {
    final String expectedBodyMarkup = getFormattedBodyOfAFullPage(expectedHtml);
    final String actualBodyMarkup = getFormattedBodyOfAFullPage(actualHtml);
    Assertions.assertEquals(expectedBodyMarkup, actualBodyMarkup, message);
  }

  private static String getFormattedBodyOfAFullPage(String html) {
    return Jsoup.parse(html, StandardCharsets.UTF_8.name(), Parser.xmlParser())
        .outputSettings(OUTPUT_SETTINGS)
        .body()
        .html()
        .trim();
  }
}
