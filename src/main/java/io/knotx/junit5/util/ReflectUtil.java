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

import com.github.tomakehurst.wiremock.WireMockServer;
import io.knotx.junit5.wiremock.ClasspathResourcesMockServer;
import io.knotx.junit5.wiremock.KnotxMockConfig;
import io.knotx.junit5.wiremock.KnotxWiremockServer;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.function.Consumer;
import org.apache.commons.lang3.StringUtils;
import org.junit.jupiter.api.extension.ParameterContext;

public final class ReflectUtil {

  private ReflectUtil() {}


  public static ClasspathResourcesMockServer getWiremockAnnotation(ParameterContext parameterContext) {
    return parameterContext
        .findAnnotation(ClasspathResourcesMockServer.class)
        .orElseThrow(IllegalStateException::new);
  }

  public static void forEachWiremockFields(Class<?> testClass, Consumer<Field> consumer) {
    Field[] fields = testClass.getDeclaredFields();

    Arrays.stream(fields)
        .filter(
            field ->
                field.isAnnotationPresent(ClasspathResourcesMockServer.class)
                    && field.getType().equals(WireMockServer.class))
        .forEach(consumer);
  }

  public static void configureServerViaMethod(KnotxWiremockServer server) {
    KnotxMockConfig mockConfig = server.getMockConfig();
    String clazzName = StringUtils.substringBefore(mockConfig.callToConfigure, "#");
    String methodName = StringUtils.substringAfter(mockConfig.callToConfigure, "#");
    Method method;

    try {
      Class<?> clazz = Class.forName(clazzName);

      method = clazz.getMethod(methodName, WireMockServer.class);
    } catch (ClassNotFoundException | NoSuchMethodException e) {
      throw new IllegalArgumentException("Class or method to invoke could not be found", e);
    }

    try {
      method.invoke(null, server);
    } catch (IllegalAccessException | InvocationTargetException e) {
      throw new IllegalStateException("Failed to invoke configure method for given server", e);
    }
  }

  public static Object fieldValue(Object instance, Field field) {
    try {
      field.setAccessible(true);
      return field.get(instance);
    } catch (IllegalAccessException e) {
      throw new IllegalStateException(
          "Could not retrieve field value for given instance", e);
    }
  }

  public static void setField(Object instance, Field field, Object value) {
    field.setAccessible(true);
    try {
      field.set(instance, value);
    } catch (IllegalAccessException | IllegalArgumentException e) {
      throw new IllegalStateException(
          "Could not inject value into requested field", e);
    }
  }
}
