package org.sonarlint.languageserver;

import java.util.HashMap;
import java.util.Map;
import org.sonarsource.sonarlint.core.ConnectedSonarLintEngineImpl;
import org.sonarsource.sonarlint.core.client.api.common.LogOutput;
import org.sonarsource.sonarlint.core.client.api.connected.ConnectedGlobalConfiguration;
import org.sonarsource.sonarlint.core.client.api.connected.ConnectedSonarLintEngine;

class ConnectedEngineFactory {

  private final LogOutput logOutput;
  private final Logger logger;

  private final Map<String, String> extraProperties = new HashMap<>();

  ConnectedEngineFactory(LogOutput logOutput, Logger logger) {
    this.logOutput = logOutput;
    this.logger = logger;
  }

  ConnectedSonarLintEngine create(ServerInfo serverInfo) {
    String serverId = serverInfo.serverId;
    logger.info("Starting connected SonarLint engine for " + serverId + "...");

    try {
      ConnectedGlobalConfiguration configuration = ConnectedGlobalConfiguration.builder()
        .setLogOutput(logOutput)
        .setServerId(serverId)
        .setExtraProperties(extraProperties)
        .build();

      ConnectedSonarLintEngineImpl engine = new ConnectedSonarLintEngineImpl(configuration);

      logger.info("Connected SonarLint engine started for " + serverId);

      return engine;
    } catch (Exception e) {
      logger.error("Error starting connected SonarLint engine for " + serverId, e);
    }
    return null;
  }

  void putExtraProperty(String name, String value) {
    extraProperties.put(name, value);
  }
}
