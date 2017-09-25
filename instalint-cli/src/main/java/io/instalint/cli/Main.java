/*
 * instalint-cli
 * Copyright (C) 2009-2017 SonarSource SA
 * mailto:info AT sonarsource DOT com
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 */
package io.instalint.cli;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import org.sonarsource.sonarlint.core.StandaloneSonarLintEngineImpl;
import org.sonarsource.sonarlint.core.client.api.common.LogOutput;
import org.sonarsource.sonarlint.core.client.api.common.ProgressMonitor;
import org.sonarsource.sonarlint.core.client.api.common.analysis.AnalysisResults;
import org.sonarsource.sonarlint.core.client.api.common.analysis.ClientInputFile;
import org.sonarsource.sonarlint.core.client.api.common.analysis.Issue;
import org.sonarsource.sonarlint.core.client.api.common.analysis.IssueListener;
import org.sonarsource.sonarlint.core.client.api.standalone.StandaloneAnalysisConfiguration;
import org.sonarsource.sonarlint.core.client.api.standalone.StandaloneGlobalConfiguration;
import org.sonarsource.sonarlint.core.client.api.standalone.StandaloneSonarLintEngine;

public class Main {
  public static void main(String[] args) throws IOException {
    StandaloneGlobalConfiguration globalConfig = StandaloneGlobalConfiguration.builder()
      .addPlugin(new File("./core/target/plugins/sonar-java-plugin-4.8.0.9441.jar").toURI().toURL())
      .build();
    StandaloneSonarLintEngine engine = new StandaloneSonarLintEngineImpl(globalConfig);

    Path tmp = newTempDir();
    Path baseDir = newDir(tmp.resolve("base"));
    Path workDir = newDir(tmp.resolve("work"));
    InputFileFinder inputFileFinder = new InputFileFinder("**/*.java", null, null, Charset.defaultCharset());
    Iterable<ClientInputFile> inputFiles = inputFileFinder.collect(Paths.get(".").toAbsolutePath());
    // TODO get some .java files in here!
    System.out.println(inputFiles);

    Map<String, String> extraProperties = new HashMap<>();
    StandaloneAnalysisConfiguration config = new StandaloneAnalysisConfiguration(baseDir, workDir, inputFiles, extraProperties);

    IssueListener issueListener = issue -> {
      System.out.println("handle issue");
    };
    LogOutput logOutput = (formattedMessage, level) -> {
      System.out.println(formattedMessage);
    };
    ProgressMonitor monitor = new ProgressMonitor() {};

    AnalysisResults results = engine.analyze(config, issueListener, logOutput, monitor);
    System.out.println(results.failedAnalysisFiles());
    System.out.println(results.fileCount());
  }

  private static Path newDir(Path path) throws IOException {
    return Files.createDirectories(path);
  }

  private static Path newTempDir() throws IOException {
    return Files.createTempDirectory("sonarlint-");
  }
}
