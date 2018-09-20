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

import static io.knotx.junit5.util.HoconUtil.getObjectOrDefault;
import static io.knotx.junit5.util.HoconUtil.getStringOrDefault;

import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.core.Options;
import com.github.tomakehurst.wiremock.http.HttpHeader;
import com.github.tomakehurst.wiremock.http.HttpHeaders;
import com.typesafe.config.Config;
import java.util.Collections;
import java.util.Map;
import java.util.Map.Entry;
import org.apache.commons.lang3.StringUtils;

/**
 * Representation of {@linkplain WireMockServer}'s configurations stored in {@linkplain
 * KnotxWiremockExtension}.
 */
class KnotxMockConfig {
  static final String PATH_INHERIT = "!inherit";
  static final String MIMETYPE_AUTODETECT = "!autodetect";
  static final String URL_MATCHING_ALL = ".*";
  static final int RANDOM_PORT = Options.DYNAMIC_PORT;

  final String reference;
  final int port;
  final String prependRequestPath;
  final String urlMatching;
  final String mimetype;
  final HttpHeaders additionalHeaders;
  final String callToConfigure;

  KnotxMockConfig(String reference, int port) {
    this.reference = reference;
    this.port = port;
    this.prependRequestPath = PATH_INHERIT;
    this.urlMatching = URL_MATCHING_ALL;
    this.mimetype = MIMETYPE_AUTODETECT;
    this.additionalHeaders = HttpHeaders.noHeaders();
    this.callToConfigure = null;
  }

  KnotxMockConfig(KnotxMockConfig parent, int newPort) {
    this.reference = parent.reference;
    this.port = newPort;
    this.prependRequestPath = parent.prependRequestPath;
    this.urlMatching = parent.urlMatching;
    this.mimetype = parent.mimetype;
    this.additionalHeaders = parent.additionalHeaders;
    this.callToConfigure = parent.callToConfigure;
  }

  private KnotxMockConfig(
      String reference,
      Integer port,
      String prependRequestPath,
      String urlMatching,
      String mimetype,
      HttpHeaders additionalHeaders,
      String callToConfigure) {
    this.reference = reference;
    this.port = port;
    this.prependRequestPath = prependRequestPath;
    this.urlMatching = urlMatching;
    this.mimetype = mimetype;
    this.additionalHeaders = additionalHeaders;
    this.callToConfigure = callToConfigure;
  }

  static KnotxMockConfig createMockConfig(Config config, String reference, String base) {
    int port = RANDOM_PORT;
    String prependRequestPath;
    String urlMatching;
    String mimetype;
    Map<String, Object> headers;
    String callMethod;

    // get port only if it's integer and not null
    if (config.hasPathOrNull(base + ".port")
        && !config.getIsNull(base + ".port")
        && config.getAnyRef(base + ".port") instanceof Integer) {
      port = config.getInt(base + ".port");
    }

    prependRequestPath =
        getStringOrDefault(config, base + ".prependRequestPath", StringUtils.EMPTY);
    urlMatching = getStringOrDefault(config, base + ".urlMatching", URL_MATCHING_ALL);
    mimetype = getStringOrDefault(config, base + ".mimetype", MIMETYPE_AUTODETECT);
    headers = getObjectOrDefault(config, base + ".additionalHeaders", Collections.emptyMap());

    HttpHeaders httpHeaders = new HttpHeaders();
    for (Entry<String, Object> entry : headers.entrySet()) {
      httpHeaders = httpHeaders.plus(new HttpHeader(entry.getKey(), entry.getValue().toString()));
    }

    callMethod = getStringOrDefault(config, base + ".callToConfigure", null);

    return new KnotxMockConfig(
        reference, port, prependRequestPath, urlMatching, mimetype, httpHeaders, callMethod);
  }
}
