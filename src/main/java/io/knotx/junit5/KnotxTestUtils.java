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

import io.knotx.junit5.util.FileReader;
import io.knotx.junit5.util.RequestUtil;
import io.reactivex.Observable;
import io.vertx.core.http.HttpMethod;
import io.vertx.reactivex.core.http.HttpClient;
import io.vertx.reactivex.core.http.HttpClientRequest;
import io.vertx.reactivex.core.http.HttpClientResponse;
import java.io.IOException;
import java.util.function.Consumer;

@Deprecated
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
    return FileReader.readText(path);
  }

  /**
   * Use this instead: {@linkplain RequestUtil#asyncRequest(HttpClient, HttpMethod, int, String, String, Consumer)}
   */
  public static Observable<HttpClientResponse> asyncRequest(
      HttpClient client,
      HttpMethod method,
      int port,
      String domain,
      String uri,
      Consumer<HttpClientRequest> requestBuilder) {
    return RequestUtil.asyncRequest(client, method, port, domain, uri, requestBuilder);
  }
}
