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
package io.knotx.junit5.wiremock;

import com.github.tomakehurst.wiremock.common.FileSource;
import com.github.tomakehurst.wiremock.extension.Parameters;
import com.github.tomakehurst.wiremock.extension.ResponseTransformer;
import com.github.tomakehurst.wiremock.http.Request;
import com.github.tomakehurst.wiremock.http.Response;
import io.knotx.junit5.util.FileReader;
import org.apache.commons.lang3.StringUtils;

/** Fix for WireMock's inability to deliver files from resources without appending various info */
class KnotxFileSource extends ResponseTransformer {

  @Override
  public Response transform(
      Request request, Response response, FileSource files, Parameters parameters) {
    String requestPath = request.getUrl();
    requestPath = StringUtils.removeStart(requestPath, "/");

    String body = FileReader.readTextSafe(requestPath);

    return Response.Builder.like(response).body(body).build();
  }

  @Override
  public String getName() {
    return "knotx-wiremock-source-changer";
  }
}
