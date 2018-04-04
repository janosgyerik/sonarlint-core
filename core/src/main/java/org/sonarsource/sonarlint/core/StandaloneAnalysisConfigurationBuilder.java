package org.sonarsource.sonarlint.core;

import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.sonar.api.rule.RuleKey;
import org.sonarsource.sonarlint.core.client.api.common.analysis.ClientInputFile;

import static org.sonarsource.sonarlint.core.container.standalone.StandaloneGlobalContainer.RULE_EXCLUSIONS_PROP;
import static org.sonarsource.sonarlint.core.container.standalone.StandaloneGlobalContainer.RULE_EXCLUSIONS_SEPARATOR;

public class StandaloneAnalysisConfigurationBuilder {

    private final Map<String, String> extraProperties = new HashMap<>();
    private Path baseDir;
    private Path workDir;
    private Iterable<ClientInputFile> inputFiles;

    public StandaloneAnalysisConfigurationBuilder baseDir(Path baseDir) {
        this.baseDir = baseDir;
        return this;
    }

    public StandaloneAnalysisConfigurationBuilder workDir(Path workDir) {
        this.workDir = workDir;
        return this;
    }

    public StandaloneAnalysisConfigurationBuilder inputFiles(Iterable<ClientInputFile> inputFiles) {
        this.inputFiles = inputFiles;
        return this;
    }

    public StandaloneAnalysisConfigurationBuilder excludeRules(List<RuleKey> ruleKeys) {
        String serialized = ruleKeys.stream().map(RuleKey::toString).collect(Collectors.joining(RULE_EXCLUSIONS_SEPARATOR));
        this.extraProperties.put(RULE_EXCLUSIONS_PROP, serialized);
        return this;
    }
}
