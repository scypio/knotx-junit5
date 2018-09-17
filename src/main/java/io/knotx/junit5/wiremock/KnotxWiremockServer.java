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

import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.client.WireMock;
import com.github.tomakehurst.wiremock.core.Options;

/**
 * WireMockServer superclass that connects server with its configuration, thus removing the need for
 * writing weird hacks when KnotxWiremockExtension is used in parallel execution environment.
 */
public class KnotxWiremockServer extends WireMockServer {

  private KnotxMockConfig mockConfig;
  private WireMock wireMock;

  KnotxWiremockServer(Options options) {
    super(options);
  }

  KnotxMockConfig getMockConfig() {
    return mockConfig;
  }

  void setMockConfig(KnotxMockConfig mockConfig) {
    this.mockConfig = mockConfig;
  }

  public WireMock getWireMock() {
    return wireMock;
  }

  public void setWireMock(WireMock wireMock) {
    this.wireMock = wireMock;
  }
}
