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
package io.knotx.junit5.util;

import com.google.common.io.CharStreams;
import com.google.common.io.Resources;
import java.io.IOException;
import java.io.InputStreamReader;

public interface FileReader {

  static String readText(String path) throws IOException {
    return CharStreams
        .toString(new InputStreamReader(Resources.getResource(path).openStream(), "utf-8"));
  }

  static String readTextSafe(String path) {
    try {
      return readText(path);
    } catch (IOException e) {
      throw new IllegalArgumentException("Could not load text from [" + path + "]");
    }
  }

}
