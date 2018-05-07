package org.sonarlint.languageserver;

import java.net.URL;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import org.sonarsource.sonarlint.core.StandaloneSonarLintEngineImpl;
import org.sonarsource.sonarlint.core.client.api.common.LogOutput;
import org.sonarsource.sonarlint.core.client.api.standalone.StandaloneGlobalConfiguration;
import org.sonarsource.sonarlint.core.client.api.standalone.StandaloneSonarLintEngine;

class StandaloneEngineFactory {
  private final Collection<URL> analyzers;
  private final LogOutput logOutput;
  private final Logger logger;

  private final Map<String, String> extraProperties = new HashMap<>();

  StandaloneEngineFactory(Collection<URL> analyzers, LogOutput logOutput, Logger logger) {
    this.analyzers = analyzers;
    this.logOutput = logOutput;
    this.logger = logger;
  }

  StandaloneSonarLintEngine create() {
    logger.info("Starting standalone SonarLint engine...");
    logger.info("Using " + analyzers.size() + " analyzers");

    StandaloneSonarLintEngine engine;
    try {
      StandaloneGlobalConfiguration.Builder builder = StandaloneGlobalConfiguration.builder()
        .setLogOutput(logOutput)
        .setExtraProperties(extraProperties)
        .addPlugins(analyzers.toArray(new URL[0]));

      engine = new StandaloneSonarLintEngineImpl(builder.build());
    } catch (Exception e) {
      logger.error("Error starting standalone SonarLint engine", e);
      throw new IllegalStateException(e);
    }

    logger.info("Standalone SonarLint engine started");

    return engine;
  }

  void putExtraProperty(String name, String value) {
    extraProperties.put(name, value);
  }
}
