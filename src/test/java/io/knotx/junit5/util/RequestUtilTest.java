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
package io.knotx.junit5.util;

import static io.reactivex.Flowable.fromArray;
import static org.junit.jupiter.api.Assertions.*;

import io.netty.handler.timeout.TimeoutException;
import io.reactivex.Single;
import io.reactivex.functions.Consumer;
import io.vertx.junit5.VertxExtension;
import io.vertx.junit5.VertxTestContext;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

@ExtendWith(VertxExtension.class)
class RequestUtilTest {

  private static final Function<Integer, Integer> DIVISION_BY_ZERO = o -> o / 0;
  private static final Consumer<Throwable> NO_ERROR_ASSERTIONS = e -> { };
  private static final Consumer<List<Integer>> NO_RESULT_ASSERTIONS = e -> { };

  @Test
  @DisplayName("Expect 'shouldFail' method ends with assertion error when no exception is thrown.")
  void expectShouldFailErrorWhenNoException(VertxTestContext global)
      throws InterruptedException {
    VertxTestContext context = new VertxTestContext();
    Single<List<Integer>> publisher = fromArray(1, 2, 3, 4)
        .collect(ArrayList::new, List::add);

    RequestUtil.subscribeToResult_shouldFail(context, publisher, NO_ERROR_ASSERTIONS);

    assertTrue(context.awaitCompletion(500, TimeUnit.MILLISECONDS));

    global.verify(() -> {
      assertTrue(context.causeOfFailure() instanceof AssertionError);
      global.completeNow();
    });
  }

  @Test
  @DisplayName("Expect 'shouldFail' method ends with success when exception is thrown.")
  void expectShouldFailNoErrorWhenExceptionAndAssertionsPass(VertxTestContext global)
      throws InterruptedException {
    VertxTestContext context = new VertxTestContext();
    Single<List<Integer>> publisher = fromArray(1, 2, 3, 4)
        .map(DIVISION_BY_ZERO::apply)
        .collect(ArrayList::new, List::add);

    RequestUtil.subscribeToResult_shouldFail(context, publisher,
        e -> assertEquals(ArithmeticException.class, e.getClass()));

    assertTrue(context.awaitCompletion(500, TimeUnit.MILLISECONDS));

    global.verify(() -> {
      assertNull(context.causeOfFailure());
      global.completeNow();
    });
  }

  @Test
  @DisplayName("Expect 'shouldFail' method ends with assertion error when exception is thrown but assertions not pass.")
  void expectShouldFailErrorWhenWhenExceptionAndAssertionsFail(VertxTestContext global)
      throws InterruptedException {
    VertxTestContext context = new VertxTestContext();
    Single<List<Integer>> publisher = fromArray(1, 2, 3, 4)
        .map(DIVISION_BY_ZERO::apply)
        .collect(ArrayList::new, List::add);

    RequestUtil.subscribeToResult_shouldFail(context, publisher,
        e -> assertEquals(TimeoutException.class, e.getClass()));

    assertTrue(context.awaitCompletion(500, TimeUnit.MILLISECONDS));

    global.verify(() -> {
      assertTrue(context.causeOfFailure() instanceof AssertionError);
      global.completeNow();
    });
  }

  @Test
  @DisplayName("Expect 'shouldSucceed' method ends with success when no exception is thrown.")
  void expectShouldSucceedSuccess(VertxTestContext global)
      throws InterruptedException {
    VertxTestContext context = new VertxTestContext();
    Single<List<Integer>> publisher = fromArray(1, 2, 3, 4)
        .collect(ArrayList::new, List::add);

    RequestUtil.subscribeToResult_shouldSucceed(context, publisher, NO_RESULT_ASSERTIONS);

    assertTrue(context.awaitCompletion(500, TimeUnit.MILLISECONDS));

    global.verify(() -> {
      assertNull(context.causeOfFailure());
      global.completeNow();
    });
  }

  @Test
  @DisplayName("Expect 'shouldSucceed' method ends with an exception when exception is thrown.")
  void expectShouldSucceedErrorWhenException(VertxTestContext global)
      throws InterruptedException {
    VertxTestContext context = new VertxTestContext();
    Single<List<Integer>> publisher = fromArray(1, 2, 3, 4)
        .map(DIVISION_BY_ZERO::apply)
        .collect(ArrayList::new, List::add);

    RequestUtil.subscribeToResult_shouldSucceed(context, publisher, NO_RESULT_ASSERTIONS);

    assertTrue(context.awaitCompletion(500, TimeUnit.MILLISECONDS));

    global.verify(() -> {
      assertTrue(context.causeOfFailure() instanceof ArithmeticException);
      global.completeNow();
    });
  }

  @Test
  @DisplayName("Expect 'shouldSucceed' method ends with an assertion error when no exception is thrown but assertions no pass.")
  void expectShouldSucceedErrorWhenNoExceptionAndAssertionsFail(VertxTestContext global)
      throws InterruptedException {
    VertxTestContext context = new VertxTestContext();
    Single<List<Integer>> publisher = fromArray(1, 2, 3, 4)
        .collect(ArrayList::new, List::add);

    RequestUtil.subscribeToResult_shouldSucceed(context, publisher,
        r -> assertEquals(Collections.emptyList(), r));

    assertTrue(context.awaitCompletion(500, TimeUnit.MILLISECONDS));

    global.verify(() -> {
      assertTrue(context.causeOfFailure() instanceof AssertionError);
      global.completeNow();
    });
  }
}