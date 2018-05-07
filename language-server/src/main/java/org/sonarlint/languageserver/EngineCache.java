package org.sonarlint.languageserver;

import javax.annotation.CheckForNull;
import javax.annotation.Nullable;
import org.sonarsource.sonarlint.core.client.api.connected.ConnectedSonarLintEngine;
import org.sonarsource.sonarlint.core.client.api.standalone.StandaloneSonarLintEngine;

/**
 * Common interface to create, cache and modify SonarLint engines.
 */
public interface EngineCache {

  /**
   * Get or create and start a standalone engine.
   */
  StandaloneSonarLintEngine getOrCreateStandaloneEngine();

  /**
   * Get or create and start a connected engine to the specified server.
   *
   * Returns null if the engine cannot be created.
   */
  @CheckForNull
  ConnectedSonarLintEngine getOrCreateConnectedEngine(ServerInfo serverInfo);

  /**
   * Add extra property. Will apply to newly created engines only.
   */
  void putExtraProperty(String name, String value);

  /**
   * Clear the cache of connected engines, stopping them.
   */
  void clearConnectedEngines();
}
