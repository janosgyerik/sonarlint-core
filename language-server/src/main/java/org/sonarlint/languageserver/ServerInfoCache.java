package org.sonarlint.languageserver;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.BiConsumer;
import javax.annotation.Nullable;

import static org.apache.commons.lang.StringUtils.isBlank;

class ServerInfoCache {

  private final Logger logger;

  private Map<String, ServerInfo> cache = new HashMap<>();

  ServerInfoCache(Logger logger) {
    this.logger = logger;
  }

  void update(@Nullable Object servers) {
    if (servers == null) {
      return;
    }

    List<Map<String, String>> maps = (List<Map<String, String>>) servers;

    maps.forEach(m -> {
      String serverId = m.get("serverId");
      String url = m.get("serverUrl");
      String token = m.get("token");
      String organization = m.get("organizationKey");
      if (!isBlank(serverId) && !isBlank(url) && !isBlank(token)) {
        cache.put(serverId, new ServerInfo(serverId, url, token, organization));
      } else {
        logger.warn("Some required parameters are missing or blank: serverId, serverUrl, token");
      }
    });
  }

  void forEach(BiConsumer<String, ServerInfo> action) {
    cache.forEach(action);
  }

  public ServerInfo get(String serverId) {
    return cache.get(serverId);
  }
}
