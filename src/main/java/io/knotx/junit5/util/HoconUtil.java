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

import com.typesafe.config.Config;
import java.util.Map;

/** Easing access to HOCON structures */
public final class HoconUtil {

  /** Util class - no instantiation */
  private HoconUtil() {}

  /**
   * Retrieve string if present, or return default value
   *
   * @param config complete HOCON config
   * @param path where to look for value
   * @param orDefault default return value if not found under path
   * @return config string
   */
  public static String getStringOrDefault(Config config, String path, String orDefault) {
    if (config.hasPath(path)) {
      return config.getString(path);
    }
    return orDefault;
  }

  /**
   * Retrieve object if present, or return default value
   *
   * @param config complete HOCON config
   * @param path where to look for value
   * @param orDefault default return value if not found under path
   * @return unwrapped config map
   */
  public static Map<String, Object> getObjectOrDefault(
      Config config, String path, Map<String, Object> orDefault) {
    if (config.hasPath(path)) {
      return config.getObject(path).unwrapped();
    }
    return orDefault;
  }
}
