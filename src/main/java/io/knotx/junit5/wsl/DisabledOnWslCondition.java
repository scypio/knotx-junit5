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
package io.knotx.junit5.wsl;

import java.util.Properties;
import org.junit.jupiter.api.extension.ConditionEvaluationResult;
import org.junit.jupiter.api.extension.ExecutionCondition;
import org.junit.jupiter.api.extension.ExtensionContext;

/**
 * Do not use this class directly, instead use the {@linkplain DisabledOnWsl} annotation for
 * selected test classes or test methods.
 *
 * <p>On WSL (Windows Subsystem for Linux) some tests may not be able to function correctly,
 * resulting in some bad SEGFAULTs or various other errors (due to the fact that WSL is technically
 * not a full Linux implementation). But some developers want to use WSL anyway, so this condition
 * allows them to keep their sanity while testing Knot.x functionalities.
 */
public class DisabledOnWslCondition implements ExecutionCondition {

  @Override
  public ConditionEvaluationResult evaluateExecutionCondition(ExtensionContext context) {
    Properties props = System.getProperties();

    if ("Linux".equals(props.getProperty("os.name"))
        && props.getProperty("os.version").contains("-Microsoft")) {
      return ConditionEvaluationResult.disabled(
          "Windows Subsystem for Linux detected, test disabled");
    }

    return ConditionEvaluationResult.enabled(
        "Not running in Windows Subsystem for Linux, test enabled");
  }
}
