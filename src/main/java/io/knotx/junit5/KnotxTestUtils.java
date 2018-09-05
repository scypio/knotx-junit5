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

import com.google.common.io.Resources;
import io.reactivex.Observable;
import io.vertx.core.http.HttpMethod;
import io.vertx.reactivex.core.http.HttpClient;
import io.vertx.reactivex.core.http.HttpClientRequest;
import io.vertx.reactivex.core.http.HttpClientResponse;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.function.Consumer;

public class KnotxTestUtils {

  /**
   * Read contents of resource and return as string. Ported from
   * io.knotx.junit.util.FileReader.readText(String) and fixed.
   *
   * @param path resource path
   * @return resource contents
   * @throws IOException resource can not be read
   */
  public static String readText(String path) throws IOException {
    return Resources.toString(Resources.getResource(path), StandardCharsets.UTF_8);
  }

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
  public static Observable<HttpClientResponse> asyncRequest(
      HttpClient client,
      HttpMethod method,
      int port,
      String domain,
      String uri,
      Consumer<HttpClientRequest> requestBuilder) {
    return Observable.unsafeCreate(
        subscriber -> {
          HttpClientRequest request = client.request(method, port, domain, uri);
          Observable<HttpClientResponse> resp = request.toObservable();
          resp.subscribe(subscriber);
          requestBuilder.accept(request);
          request.end();
        });
  }
}
