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

import io.knotx.dataobjects.Fragment;
import io.vertx.core.json.JsonObject;
import java.io.IOException;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.converter.ArgumentConversionException;
import org.junit.jupiter.params.converter.SimpleArgumentConverter;

/**
 * Simplifies argument type conversion for parameterized tests (these annotated with {@linkplain
 * ParameterizedTest})
 */
public class KnotxArgumentConverter extends SimpleArgumentConverter {

  @Override
  protected Object convert(Object source, Class<?> targetType) throws ArgumentConversionException {
    if (!String.class.equals(source.getClass())) {
      throw new ArgumentConversionException("This converter supports only String as source object");
    }

    String toConvert = String.valueOf(source);

    if (targetType.equals(Fragment.class)) {
      try {
        return KnotxConverters.createFragmentMock(toConvert);
      } catch (IOException e) {
        throw new ArgumentConversionException("Exception thrown during conversion", e);
      }
    }
    if (targetType.equals(JsonObject.class)) {
      return KnotxConverters.createJsonObject(toConvert);
    }

    throw new ArgumentConversionException(
        "Unsupported object type for conversion: " + targetType.toString());
  }
}
