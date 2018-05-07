package org.sonarlint.languageserver;

import java.util.HashMap;
import java.util.Map;
import org.sonarsource.sonarlint.core.client.api.connected.ConnectedSonarLintEngine;
import org.sonarsource.sonarlint.core.client.api.standalone.StandaloneSonarLintEngine;

public class DefaultEngineCache implements EngineCache {

  private final Map<String, ConnectedSonarLintEngine> cache = new HashMap<>();

  private final StandaloneEngineFactory standaloneEngineFactory;
  private final ConnectedEngineFactory connectedEngineFactory;

  private final Lazy<StandaloneSonarLintEngine> standaloneEngine;

  DefaultEngineCache(StandaloneEngineFactory standaloneEngineFactory, ConnectedEngineFactory connectedEngineFactory) {
    this.standaloneEngineFactory = standaloneEngineFactory;
    this.connectedEngineFactory = connectedEngineFactory;

    standaloneEngine = new Lazy<>(standaloneEngineFactory::create);
  }

  @Override
  public StandaloneSonarLintEngine getOrCreateStandaloneEngine() {
    return standaloneEngine.get();
  }

  @Override
  public ConnectedSonarLintEngine getOrCreateConnectedEngine(ServerInfo serverInfo) {
    ConnectedSonarLintEngine engine = cache.get(serverInfo.serverId);
    if (engine == null) {
      engine = connectedEngineFactory.create(serverInfo);
      if (engine != null) {
        cache.put(serverInfo.serverId, engine);
      }
    }
    return cache.get(serverInfo.serverId);
  }

  @Override
  public void putExtraProperty(String name, String value) {
    standaloneEngineFactory.putExtraProperty(name, value);
    connectedEngineFactory.putExtraProperty(name, value);
  }

  @Override
  public void clearConnectedEngines() {
    cache.values().forEach(engine -> engine.stop(false));
    cache.clear();
  }
}
