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

import io.reactivex.Observable;
import io.reactivex.Single;
import io.reactivex.functions.Consumer;
import io.vertx.core.http.HttpMethod;
import io.vertx.junit5.VertxTestContext;
import io.vertx.reactivex.core.http.HttpClient;
import io.vertx.reactivex.core.http.HttpClientRequest;
import io.vertx.reactivex.core.http.HttpClientResponse;

public interface RequestUtil {
  /**
   * Generate reactivex async request for given resource parameters
   *
   * @param client Vert.x client
   * @param method target HTTP method
   * @param port target port
   * @param domain target domain
   * @param uri resource to request
   * @param requestBuilder handler for request body and params
   * @return reactivex wrapper
   */
  static Observable<HttpClientResponse> asyncRequest(
      HttpClient client,
      HttpMethod method,
      int port,
      String domain,
      String uri,
      java.util.function.Consumer<HttpClientRequest> requestBuilder) {
    return Observable.unsafeCreate(
        subscriber -> {
          HttpClientRequest request = client.request(method, port, domain, uri);
          Observable<HttpClientResponse> resp = request.toObservable();
          resp.subscribe(subscriber);
          requestBuilder.accept(request);
          request.end();
        });
  }

  /**
   * Safely execute onError handler on given result, passing checks to given test context
   *
   * @param context test context
   * @param result to which subscribe
   * @param onError result handler, can throw exceptions
   */
  static <T> void subscribeToResult_shouldFail(
      VertxTestContext context, Single<T> result, Consumer<Throwable> onError) {
    result
        .doOnError(onError)
        .subscribe(
            response -> context.failNow(new IllegalStateException("Error should occur")),
            error -> context.completeNow());
  }

  /**
   * Safely execute onSuccess handler on given result, passing checks to given test context
   *
   * @param context test context
   * @param result to which subscribe
   * @param onSuccess result handler, can throw exceptions
   */
  static <T> void subscribeToResult_shouldSucceed(
      VertxTestContext context, Single<T> result, Consumer<T> onSuccess) {
    result
        .doOnSuccess(onSuccess)
        .subscribe(
            response -> context.completeNow(), context::failNow);
  }

  static <T> void processWithContextVerification(
      VertxTestContext context, Consumer<T> consumer, T consummable) {
    context.verify(
        () -> {
          try {
            consumer.accept(consummable);

            context.completeNow();
          } catch (Exception e) {
            context.failNow(e);
          }
        });
  }
}
