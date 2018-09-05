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

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Specifies Knot.x configuration path. More details about Knot.x configuration can be found <a
 * href="https://github.com/Cognifide/knotx/wiki/Configuration">here</a>.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.METHOD})
public @interface KnotxApplyConfiguration {

  /**
   * Configuration file path.
   *
   * Knot.x uses <a href="https://github.com/lightbend/config/blob/master/HOCON.md">HOCON</a> syntax
   * for files with *.conf extension, otherwise uses JSON.
   *
   * @return configuration file path
   */
  String value();
  
}
