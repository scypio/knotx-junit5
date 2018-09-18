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
import com.github.tomakehurst.wiremock.http.HttpHeader;
import com.github.tomakehurst.wiremock.http.HttpHeaders;
import com.github.tomakehurst.wiremock.http.Request;
import com.github.tomakehurst.wiremock.http.Response;
import com.github.tomakehurst.wiremock.http.Response.Builder;
import com.google.common.collect.ImmutableMap;
import io.knotx.junit5.util.FileReader;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Map;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang3.StringUtils;

/** Fix for WireMock's inability to deliver files from resources without appending various info */
class KnotxFileSource extends ResponseTransformer {

  private static final String CHARSET_APPEND = "; charset=UTF-8";

  private static Map<String, String> extensionMapping =
      ImmutableMap.of(
          "html", "text/html",
          "json", "application/json",
          "txt", "text/plain");

  private boolean autodetectMime;
  private final KnotxMockConfig config;

  public KnotxFileSource(KnotxMockConfig config) {
    autodetectMime = KnotxMockConfig.MIMETYPE_AUTODETECT.equals(config.mimetype);
    this.config = config;
  }

  @Override
  public Response transform(
      Request request, Response response, FileSource files, Parameters parameters) {
    String requestPath;

    try {
      requestPath = new URL(request.getAbsoluteUrl()).getPath();
    } catch (MalformedURLException e) {
      throw new IllegalStateException("Malformed request URL", e);
    }

    Builder builder = Builder.like(response);

    if (autodetectMime) {
      String extension = FilenameUtils.getExtension(requestPath);
      String mime = extensionMapping.get(extension);

      if (StringUtils.isNotBlank(mime)) {
        mime += CHARSET_APPEND;
        builder.headers(
            HttpHeaders.copyOf(response.getHeaders())
                .plus(HttpHeader.httpHeader("Content-Type", mime)));
      }
    }

    requestPath = StringUtils.removeStart(requestPath, "/");

    String body = FileReader.readTextSafe(requestPath);

    return builder.body(body).build();
  }

  @Override
  public String getName() {
    return "knotx-wiremock-source-changer";
  }
}
