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

import com.typesafe.config.Config;
import io.vertx.core.json.JsonObject;
import java.util.List;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.api.extension.ExtensionContext.Namespace;
import org.junit.jupiter.api.extension.ExtensionContext.Store;
import org.junit.jupiter.api.extension.ParameterContext;

/** Common methods for JUnit 5 extension classes */
public abstract class KnotxBaseExtension {

  protected Class<?> getType(ParameterContext parameterContext) {
    return parameterContext.getParameter().getType();
  }

  protected Store getStore(ExtensionContext extensionContext) {
    return extensionContext.getStore(Namespace.create(this.getClass(), extensionContext));
  }

  protected String getClassName(ExtensionContext extensionContext) {
    return extensionContext.getTestClass().orElseThrow(IllegalStateException::new).getName();
  }

  protected String getMethodName(ParameterContext parameterContext) {
    return parameterContext.getDeclaringExecutable().getName();
  }

  protected String getParameterName(ParameterContext parameterContext) {
    return parameterContext.getParameter().getName();
  }

  public void addToOverrides(Config config, List<JsonObject> overrides, String forReference) {}
}
