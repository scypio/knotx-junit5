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

import java.util.HashMap;
import java.util.stream.Stream;

public final class StreamUtil {
  private StreamUtil() {}

  public static <T> boolean anyKeyStartsWith(
      HashMap<String, T> map, String startsWith) {
    return map.keySet().stream().anyMatch(s -> s.startsWith(startsWith));
  }

  public static <T> Stream<T> concatValues(
      HashMap<String, T> first, HashMap<String, T> second) {
    return Stream.concat(
        first.values().stream(), second.values().stream());
  }
}
