package org.sonarlint.languageserver;

import javax.annotation.Nullable;

class ServerInfo {
  final String serverId;
  final String serverUrl;
  final String token;

  @Nullable
  final String organizationKey;

  ServerInfo(String serverId, String serverUrl, String token, @Nullable String organizationKey) {
    this.serverId = serverId;
    this.serverUrl = serverUrl;
    this.token = token;
    this.organizationKey = organizationKey;
  }
}
