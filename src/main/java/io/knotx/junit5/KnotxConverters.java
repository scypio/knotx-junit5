/*
 * Copyright (C) 2018 Knot.x Project
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
package io.knotx.junit5;

import static org.mockito.Mockito.when;

import io.knotx.dataobjects.Fragment;
import io.knotx.fragments.SnippetPatterns;
import io.knotx.options.SnippetOptions;
import io.vertx.core.json.JsonObject;
import java.io.IOException;
import org.apache.commons.lang3.StringUtils;
import org.mockito.Mockito;

class KnotxConverters {

  private static final String DEFAULT_SNIPPET_TAG_NAME = "script";
  private static final String DEFAULT_SNIPPET_PARAM_PREFIX = "data-knotx-";
  private static final String PARAMETER_SEPARATOR = "\\|";

  /**
   * Builds mock of {@link io.knotx.dataobjects.Fragment} from provided fragmentParameters. The only
   * mandatory value of the fragmentParameters is fragment content file, that will be the fragment
   * content. There 2 additional params:
   *
   * <ul>
   *   <li>custom snippet tag name (default value is <bb>script</bb>)
   *   <li>custom snippet parameters prefix (default value is <bb>data-knotx-</bb>)
   * </ul>
   *
   * You can embed them in the fragmentParameters, separated by <bb>|</bb> character.<br>
   * <br>
   * <i>Example:</i> "content.txt;knotx:snippet;data-knotx-" will produce Fragment mock with content
   * read from content.txt, snippet tag name set to "knotx:snippet" and snippet parameters prefix
   * set to "data-knotx-". If the last character of fragmentParameters is the separator <bb>|</bb>,
   * then snippet parameters prefix will be set to empty string (no prefix).
   */
  static Fragment createFragmentMock(String fragmentParameters) throws IOException {
    final String[] params = fragmentParameters.split(PARAMETER_SEPARATOR);
    final String fragmentContentFile = params[0];
    final String snippetTagName = extractSnippetTagName(params);
    final String fragmentContent = KnotxTestUtils.readText(fragmentContentFile);
    final String snippetParamPrefix = extractSnippetParamPrefix(params, fragmentParameters);
    final SnippetPatterns patterns =
        new SnippetPatterns(buildOptions(snippetTagName, snippetParamPrefix));

    Fragment fragmentMock = Mockito.mock(Fragment.class);
    when(fragmentMock.content()).thenReturn(fragmentContent);
    when(fragmentMock.isRaw())
        .thenReturn(!patterns.getAnySnippetPattern().matcher(fragmentContent).matches());
    return fragmentMock;
  }

  private static SnippetOptions buildOptions(String snippetTagName, String snippetParamPrefix) {
    return new SnippetOptions().setTagName(snippetTagName).setParamsPrefix(snippetParamPrefix);
  }

  private static String extractSnippetTagName(String[] params) {
    if (params.length > 1) {
      return params[1];
    }
    return DEFAULT_SNIPPET_TAG_NAME;
  }

  private static String extractSnippetParamPrefix(String[] params, String fragmentParameters) {
    if (params.length > 2) {
      return params[2];
    } else if (StringUtils.endsWith(fragmentParameters, "|")) {
      return  "";
    }
    return DEFAULT_SNIPPET_PARAM_PREFIX;
  }

  static JsonObject createJsonObject(String input) {
    return new JsonObject(input);
  }
}
