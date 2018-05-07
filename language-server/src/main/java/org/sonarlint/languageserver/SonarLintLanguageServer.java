/*
 * SonarLint Language Server
 * Copyright (C) 2009-2018 SonarSource SA
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
package org.sonarlint.languageserver;

import com.google.common.annotations.VisibleForTesting;
import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonPrimitive;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.net.Socket;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Future;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import javax.annotation.CheckForNull;
import javax.annotation.Nullable;
import org.eclipse.lsp4j.CodeActionParams;
import org.eclipse.lsp4j.CodeLens;
import org.eclipse.lsp4j.CodeLensParams;
import org.eclipse.lsp4j.Command;
import org.eclipse.lsp4j.CompletionItem;
import org.eclipse.lsp4j.CompletionList;
import org.eclipse.lsp4j.CompletionParams;
import org.eclipse.lsp4j.Diagnostic;
import org.eclipse.lsp4j.DiagnosticSeverity;
import org.eclipse.lsp4j.DidChangeConfigurationParams;
import org.eclipse.lsp4j.DidChangeTextDocumentParams;
import org.eclipse.lsp4j.DidChangeWatchedFilesParams;
import org.eclipse.lsp4j.DidChangeWorkspaceFoldersParams;
import org.eclipse.lsp4j.DidCloseTextDocumentParams;
import org.eclipse.lsp4j.DidOpenTextDocumentParams;
import org.eclipse.lsp4j.DidSaveTextDocumentParams;
import org.eclipse.lsp4j.DocumentFormattingParams;
import org.eclipse.lsp4j.DocumentHighlight;
import org.eclipse.lsp4j.DocumentOnTypeFormattingParams;
import org.eclipse.lsp4j.DocumentRangeFormattingParams;
import org.eclipse.lsp4j.DocumentSymbolParams;
import org.eclipse.lsp4j.ExecuteCommandOptions;
import org.eclipse.lsp4j.ExecuteCommandParams;
import org.eclipse.lsp4j.Hover;
import org.eclipse.lsp4j.InitializeParams;
import org.eclipse.lsp4j.InitializeResult;
import org.eclipse.lsp4j.Location;
import org.eclipse.lsp4j.MessageParams;
import org.eclipse.lsp4j.MessageType;
import org.eclipse.lsp4j.Position;
import org.eclipse.lsp4j.PublishDiagnosticsParams;
import org.eclipse.lsp4j.Range;
import org.eclipse.lsp4j.ReferenceParams;
import org.eclipse.lsp4j.RenameParams;
import org.eclipse.lsp4j.SaveOptions;
import org.eclipse.lsp4j.ServerCapabilities;
import org.eclipse.lsp4j.SignatureHelp;
import org.eclipse.lsp4j.SymbolInformation;
import org.eclipse.lsp4j.TextDocumentPositionParams;
import org.eclipse.lsp4j.TextDocumentSyncKind;
import org.eclipse.lsp4j.TextDocumentSyncOptions;
import org.eclipse.lsp4j.TextEdit;
import org.eclipse.lsp4j.WorkspaceEdit;
import org.eclipse.lsp4j.WorkspaceFolder;
import org.eclipse.lsp4j.WorkspaceFoldersChangeEvent;
import org.eclipse.lsp4j.WorkspaceFoldersOptions;
import org.eclipse.lsp4j.WorkspaceServerCapabilities;
import org.eclipse.lsp4j.WorkspaceSymbolParams;
import org.eclipse.lsp4j.jsonrpc.Launcher;
import org.eclipse.lsp4j.jsonrpc.messages.Either;
import org.eclipse.lsp4j.services.LanguageServer;
import org.eclipse.lsp4j.services.TextDocumentService;
import org.eclipse.lsp4j.services.WorkspaceService;
import org.sonar.api.internal.apachecommons.lang.StringUtils;
import org.sonarsource.sonarlint.core.client.api.common.RuleDetails;
import org.sonarsource.sonarlint.core.client.api.common.analysis.AnalysisResults;
import org.sonarsource.sonarlint.core.client.api.common.analysis.ClientInputFile;
import org.sonarsource.sonarlint.core.client.api.common.analysis.Issue;
import org.sonarsource.sonarlint.core.client.api.common.analysis.IssueListener;
import org.sonarsource.sonarlint.core.client.api.connected.ConnectedAnalysisConfiguration;
import org.sonarsource.sonarlint.core.client.api.connected.ConnectedSonarLintEngine;
import org.sonarsource.sonarlint.core.client.api.connected.ServerConfiguration;
import org.sonarsource.sonarlint.core.client.api.standalone.StandaloneAnalysisConfiguration;
import org.sonarsource.sonarlint.core.client.api.standalone.StandaloneSonarLintEngine;
import org.sonarsource.sonarlint.core.telemetry.TelemetryPathManager;

import static org.apache.commons.lang.StringUtils.isBlank;

public class SonarLintLanguageServer implements LanguageServer, WorkspaceService, TextDocumentService {

  private static final String USER_AGENT = "SonarLint Language Server";

  static final String DISABLE_TELEMETRY = "disableTelemetry";
  static final String TYPESCRIPT_LOCATION = "typeScriptLocation";
  static final String TEST_FILE_PATTERN = "testFilePattern";
  private static final String ANALYZER_PROPERTIES = "analyzerProperties";
  static final String CONNECTED_MODE_SERVERS_PROP = "connectedModeServers";
  private static final String CONNECTED_MODE_PROJECT_PROP = "connectedModeProject";
  private static final String TYPESCRIPT_PATH_PROP = "sonar.typescript.internal.typescriptLocation";

  private static final String SONARLINT_CONFIGURATION_NAMESPACE = "sonarlint";
  private static final String SONARLINT_SOURCE = SONARLINT_CONFIGURATION_NAMESPACE;
  private static final String SONARLINT_OPEN_RULE_DESCRIPTION_COMMAND = "SonarLint.OpenRuleDesc";
  static final String SONARLINT_UPDATE_SERVER_STORAGE_COMMAND = "SonarLint.UpdateServerStorage";
  static final String SONARLINT_UPDATE_PROJECT_BINDING_COMMAND = "SonarLint.UpdateProjectBinding";
  private static final List<String> SONARLINT_COMMANDS = Arrays.asList(
    SONARLINT_OPEN_RULE_DESCRIPTION_COMMAND,
    SONARLINT_UPDATE_SERVER_STORAGE_COMMAND,
    SONARLINT_UPDATE_PROJECT_BINDING_COMMAND);

  private final SonarLintLanguageClient client;
  private final Future<?> backgroundProcess;
  private final LanguageClientLogOutput logOutput;

  private final Map<URI, String> languageIdPerFileURI = new HashMap<>();
  private final SonarLintTelemetry telemetry = new SonarLintTelemetry();

  private UserSettings userSettings = new UserSettings();
  private final List<String> workspaceFolders = new ArrayList<>();

  private final EngineCache engineCache;
  private final ServerInfoCache serverInfoCache;

  private ServerProjectBinding binding;

  SonarLintLanguageServer(InputStream inputStream, OutputStream outputStream,
                          BiFunction<LanguageClientLogOutput, Logger, EngineCache> engineCacheFactory) {
    Launcher<SonarLintLanguageClient> launcher = Launcher.createLauncher(this,
      SonarLintLanguageClient.class,
      inputStream,
      outputStream,
      true, new PrintWriter(System.out));

    this.client = launcher.getRemoteProxy();
    this.logOutput = new LanguageClientLogOutput(client);

    backgroundProcess = launcher.startListening();

    Logger logger = new Logger();
    this.engineCache = engineCacheFactory.apply(logOutput, logger);
    this.serverInfoCache = new ServerInfoCache(logger);
  }

  static SonarLintLanguageServer bySocket(int port, Collection<URL> analyzers) throws IOException {
    Socket socket = new Socket("localhost", port);
    BiFunction<LanguageClientLogOutput, Logger, EngineCache> factory = (logOutput, logger) -> {
      StandaloneEngineFactory standaloneEngineFactory = new StandaloneEngineFactory(analyzers, logOutput, logger);
      ConnectedEngineFactory connectedEngineFactory = new ConnectedEngineFactory(logOutput, logger);
      return new DefaultEngineCache(standaloneEngineFactory, connectedEngineFactory);
    };
    return new SonarLintLanguageServer(socket.getInputStream(), socket.getOutputStream(), factory);
  }

  private class UserSettings {
    @CheckForNull
    final String testFilePattern;
    final Map<String, String> analyzerProperties;
    final boolean disableTelemetry;

    private UserSettings() {
      this(Collections.emptyMap());
    }

    private UserSettings(Map<String, Object> params) {
      this.testFilePattern = (String) params.get(TEST_FILE_PATTERN);
      this.analyzerProperties = getAnalyzerProperties(params);
      this.disableTelemetry = (Boolean) params.getOrDefault(DISABLE_TELEMETRY, false);
    }

    private Map<String, String> getAnalyzerProperties(Map<String, Object> params) {
      Map map = (Map) params.get(ANALYZER_PROPERTIES);
      if (map == null) {
        return Collections.emptyMap();
      }
      return map;
    }
  }

  private void debug(String message) {
    client.logMessage(new MessageParams(MessageType.Log, message));
  }

  private void info(String message) {
    client.logMessage(new MessageParams(MessageType.Info, message));
  }

  void warn(String message) {
    client.logMessage(new MessageParams(MessageType.Warning, message));
  }

  void error(String message, Throwable t) {
    StringWriter sw = new StringWriter();
    PrintWriter pw = new PrintWriter(sw);
    t.printStackTrace(pw);
    client.logMessage(new MessageParams(MessageType.Error, message + "\n" + sw.toString()));
  }

  private void popupError(String message) {
    client.showMessage(new MessageParams(MessageType.Error, message));
  }

  @Override
  public CompletableFuture<InitializeResult> initialize(InitializeParams params) {
    workspaceFolders.addAll(parseWorkspaceFolders(params.getWorkspaceFolders(), params.getRootUri()));
    workspaceFolders.sort(Comparator.reverseOrder());

    Map<String, Object> options = parseToMap(params.getInitializationOptions());
    userSettings = new UserSettings(options);

    String productKey = (String) options.get("productKey");
    // deprecated, will be ignored when productKey present
    String telemetryStorage = (String) options.get("telemetryStorage");

    String productName = (String) options.get("productName");
    String productVersion = (String) options.get("productVersion");

    telemetry.init(getStoragePath(productKey, telemetryStorage), productName, productVersion);
    telemetry.optOut(userSettings.disableTelemetry);

    String typeScriptPath = (String) options.get(TYPESCRIPT_LOCATION);
    engineCache.putExtraProperty(TYPESCRIPT_PATH_PROP, typeScriptPath);

    // start standalone engine
    engineCache.getOrCreateStandaloneEngine();

    serverInfoCache.update(options.get(CONNECTED_MODE_SERVERS_PROP));
    updateBinding((Map<?, ?>) options.get(CONNECTED_MODE_PROJECT_PROP));

    InitializeResult result = new InitializeResult();
    ServerCapabilities c = new ServerCapabilities();
    c.setTextDocumentSync(getTextDocumentSyncOptions());
    c.setCodeActionProvider(true);
    c.setExecuteCommandProvider(new ExecuteCommandOptions(SONARLINT_COMMANDS));
    c.setWorkspace(getWorkspaceServerCapabilities());

    result.setCapabilities(c);
    return CompletableFuture.completedFuture(result);
  }

  private static WorkspaceServerCapabilities getWorkspaceServerCapabilities() {
    WorkspaceFoldersOptions options = new WorkspaceFoldersOptions();
    options.setSupported(true);
    options.setChangeNotifications(true);

    WorkspaceServerCapabilities capabilities = new WorkspaceServerCapabilities();
    capabilities.setWorkspaceFolders(options);
    return capabilities;
  }

  private static TextDocumentSyncOptions getTextDocumentSyncOptions() {
    TextDocumentSyncOptions textDocumentSyncOptions = new TextDocumentSyncOptions();
    textDocumentSyncOptions.setOpenClose(true);
    textDocumentSyncOptions.setChange(TextDocumentSyncKind.Full);
    textDocumentSyncOptions.setSave(new SaveOptions(true));
    return textDocumentSyncOptions;
  }


  private void handleUpdateServerStorageCommand(@Nullable List<Object> arguments) {
    engineCache.clearConnectedEngines();

    serverInfoCache.update(arguments);

    serverInfoCache.forEach((serverId, serverInfo) -> {
      ConnectedSonarLintEngine engine = engineCache.getOrCreateConnectedEngine(serverInfo);
      if (engine == null) {
        popupError("Could not start server: " + serverId);
      } else {
        debug("Updating global storage of server " + serverId + ", may take some time...");

        ServerConfiguration serverConfig = getServerConfiguration(serverInfo);
        engine.update(serverConfig, null);

        debug("Successfully updated global storage of server " + serverId);
      }
    });
  }

  private ServerConfiguration getServerConfiguration(ServerInfo serverInfo) {
    return ServerConfiguration.builder()
            .url(serverInfo.serverUrl)
            .token(serverInfo.token)
            .organizationKey(serverInfo.organizationKey)
            .userAgent(USER_AGENT)
            .build();
  }

  private void updateBinding(@Nullable Map<?, ?> connectedModeProject) {
    binding = null;
    if (connectedModeProject == null) {
      return;
    }

    Map<String, String> map = (Map<String, String>) connectedModeProject;
    String serverId = map.get("serverId");
    String projectKey = map.get("projectKey");
    if (isBlank(serverId) || isBlank(projectKey)) {
      debug("Some required parameters are missing or blank: serverId, projectKey");
      return;
    }

    binding = new ServerProjectBinding(serverId, projectKey);

    ServerInfo serverInfo = serverInfoCache.get(serverId);
    if (serverInfo == null) {
      popupError("Could not find server: " + serverId + "; update server storage and then bind again");
      return;
    }

    ConnectedSonarLintEngine engine = engineCache.getOrCreateConnectedEngine(serverInfo);
    if (engine == null) {
      popupError("Could not find server: " + serverId + "; update server storage and then bind again");
      return;
    }

    ServerConfiguration serverConfig = getServerConfiguration(serverInfo);
    try {
      engine.updateModule(serverConfig, projectKey, null);
    } catch (Exception e) {
      popupError(e.getMessage());
    }
  }

  @VisibleForTesting
  static List<String> parseWorkspaceFolders(@Nullable List<WorkspaceFolder> workspaceFolders, @Nullable String rootUri) {
    if (workspaceFolders != null && !workspaceFolders.isEmpty()) {
      return toList(workspaceFolders);
    }

    // rootURI is null when no folder is open (like opening a single file in VSCode)
    if (rootUri != null) {
      return Collections.singletonList(rootUri);
    }

    return Collections.emptyList();
  }

  private static List<String> toList(List<WorkspaceFolder> workspaceFolders) {
    return workspaceFolders.stream().map(f -> f.getUri().replaceAll("^file://", "")).collect(Collectors.toList());
  }

  @VisibleForTesting
  static Path getStoragePath(@Nullable String productKey, @Nullable String telemetryStorage) {
    if (productKey != null) {
      if (telemetryStorage != null) {
        TelemetryPathManager.migrate(productKey, Paths.get(telemetryStorage));
      }
      return TelemetryPathManager.getPath(productKey);
    }
    return telemetryStorage != null ? Paths.get(telemetryStorage) : null;
  }

  @Override
  public CompletableFuture<Object> shutdown() {
    engineCache.getOrCreateStandaloneEngine().stop();
    engineCache.clearConnectedEngines();
    telemetry.stop();
    return CompletableFuture.completedFuture("Stopped");
  }

  @Override
  public void exit() {
    backgroundProcess.cancel(true);
  }

  @Override
  public TextDocumentService getTextDocumentService() {
    return this;
  }

  @Override
  public CompletableFuture<Either<List<CompletionItem>, CompletionList>> completion(CompletionParams completionParams) {
    return null;
  }

  @Override
  public CompletableFuture<CompletionItem> resolveCompletionItem(CompletionItem unresolved) {
    return null;
  }

  @Override
  public CompletableFuture<Hover> hover(TextDocumentPositionParams position) {
    return null;
  }

  @Override
  public CompletableFuture<SignatureHelp> signatureHelp(TextDocumentPositionParams position) {
    return null;
  }

  @Override
  public CompletableFuture<List<? extends Location>> definition(TextDocumentPositionParams position) {
    return null;
  }

  @Override
  public CompletableFuture<List<? extends Location>> references(ReferenceParams params) {
    return null;
  }

  @Override
  public CompletableFuture<List<? extends DocumentHighlight>> documentHighlight(TextDocumentPositionParams position) {
    return null;
  }

  @Override
  public CompletableFuture<List<? extends SymbolInformation>> documentSymbol(DocumentSymbolParams params) {
    return null;
  }

  @Override
  public CompletableFuture<List<? extends Command>> codeAction(CodeActionParams params) {
    List<Command> commands = new ArrayList<>();
    for (Diagnostic d : params.getContext().getDiagnostics()) {
      if (SONARLINT_SOURCE.equals(d.getSource())) {
        commands.add(
          new Command("Open description of rule " + d.getCode(),
            SONARLINT_OPEN_RULE_DESCRIPTION_COMMAND,
            Collections.singletonList(d.getCode())));
      }
    }
    return CompletableFuture.completedFuture(commands);
  }

  @Override
  public CompletableFuture<List<? extends CodeLens>> codeLens(CodeLensParams params) {
    return null;
  }

  @Override
  public CompletableFuture<CodeLens> resolveCodeLens(CodeLens unresolved) {
    return null;
  }

  @Override
  public CompletableFuture<List<? extends TextEdit>> formatting(DocumentFormattingParams params) {
    return null;
  }

  @Override
  public CompletableFuture<List<? extends TextEdit>> rangeFormatting(DocumentRangeFormattingParams params) {
    return null;
  }

  @Override
  public CompletableFuture<List<? extends TextEdit>> onTypeFormatting(DocumentOnTypeFormattingParams params) {
    return null;
  }

  @Override
  public CompletableFuture<WorkspaceEdit> rename(RenameParams params) {
    return null;
  }

  @Override
  public void didOpen(DidOpenTextDocumentParams params) {
    URI uri = parseURI(params.getTextDocument().getUri());
    languageIdPerFileURI.put(uri, params.getTextDocument().getLanguageId());
    analyze(uri, params.getTextDocument().getText());
  }

  @Override
  public void didChange(DidChangeTextDocumentParams params) {
    URI uri = parseURI(params.getTextDocument().getUri());
    analyze(uri, params.getContentChanges().get(0).getText());
  }

  @Override
  public void didClose(DidCloseTextDocumentParams params) {
    URI uri = parseURI(params.getTextDocument().getUri());
    languageIdPerFileURI.remove(uri);
    // Clear issues
    client.publishDiagnostics(newPublishDiagnostics(uri));
  }

  @Override
  public void didSave(DidSaveTextDocumentParams params) {
    String content = params.getText();
    if (content != null) {
      URI uri = parseURI(params.getTextDocument().getUri());
      analyze(uri, params.getText());
    }
  }

  private static URI parseURI(String uriStr) {
    URI uri;
    try {
      uri = new URI(uriStr);
    } catch (URISyntaxException e) {
      throw new IllegalStateException(e.getMessage(), e);
    }
    return uri;
  }

  private void analyze(URI uri, String content) {
    if (!uri.toString().startsWith("file:/")) {
      warn("URI is not a file, analysis not supported");
      return;
    }

    Map<URI, PublishDiagnosticsParams> files = new HashMap<>();
    files.put(uri, newPublishDiagnostics(uri));

    IssueListener issueListener = issue -> {
      ClientInputFile inputFile = issue.getInputFile();
      if (inputFile != null) {
        URI uri1 = inputFile.getClientObject();
        PublishDiagnosticsParams publish = files.computeIfAbsent(uri1, SonarLintLanguageServer::newPublishDiagnostics);

        convert(issue).ifPresent(publish.getDiagnostics()::add);
      }
    };

    AnalysisWrapper analysisWrapper = getAnalysisWrapper();
    try {
      AnalysisResultsWrapper analysisResults = analysisWrapper.analyze(uri, content, issueListener);
      telemetry.analysisDoneOnSingleFile(StringUtils.substringAfterLast(uri.toString(), "."), analysisResults.analysisTime);

      // Ignore files with parsing error
      analysisResults.results.failedAnalysisFiles().stream().map(ClientInputFile::getClientObject).forEach(files::remove);
      files.values().forEach(client::publishDiagnostics);
    } catch (Exception e) {
      popupError(e.getMessage());
    }
  }

  private AnalysisWrapper getAnalysisWrapper() {
    if (binding != null) {
      ServerInfo serverInfo = serverInfoCache.get(binding.serverId);
      if (serverInfo != null) {
        ConnectedSonarLintEngine engine = engineCache.getOrCreateConnectedEngine(serverInfo);
        if (engine != null) {
          return new ConnectedAnalysisWrapper(engine, binding.projectKey);
        }
      }
    }

    return new StandaloneAnalysisWrapper();
  }

  static class AnalysisResultsWrapper {
    private final AnalysisResults results;
    private final int analysisTime;

    AnalysisResultsWrapper(AnalysisResults results, int analysisTime) {
      this.results = results;
      this.analysisTime = analysisTime;
    }
  }

  interface AnalysisWrapper {
    AnalysisResultsWrapper analyze(URI uri, String content, IssueListener issueListener);
  }

  class StandaloneAnalysisWrapper implements AnalysisWrapper {
    @Override
    public AnalysisResultsWrapper analyze(URI uri, String content, IssueListener issueListener) {
      Path baseDir = findBaseDir(uri);
      StandaloneAnalysisConfiguration configuration = new StandaloneAnalysisConfiguration(baseDir, baseDir.resolve(".sonarlint"),
        Collections.singletonList(new DefaultClientInputFile(baseDir, uri, content, userSettings.testFilePattern, languageIdPerFileURI.get(uri))),
        userSettings.analyzerProperties);
      debug("Analysis triggered on " + uri + " with configuration: \n" + configuration.toString());

      long start = System.currentTimeMillis();
      StandaloneSonarLintEngine engine = engineCache.getOrCreateStandaloneEngine();
      AnalysisResults analysisResults = engine.analyze(configuration, issueListener, logOutput, null);
      int analysisTime = (int) (System.currentTimeMillis() - start);

      return new AnalysisResultsWrapper(analysisResults, analysisTime);
    }
  }

  class ConnectedAnalysisWrapper implements AnalysisWrapper {
    private final ConnectedSonarLintEngine engine;
    private final String projectKey;

    ConnectedAnalysisWrapper(ConnectedSonarLintEngine engine, String projectKey) {
      this.engine = engine;
      this.projectKey = projectKey;
    }

    @Override
    public AnalysisResultsWrapper analyze(URI uri, String content, IssueListener issueListener) {
      Path baseDir = findBaseDir(uri);
      ConnectedAnalysisConfiguration configuration = new ConnectedAnalysisConfiguration(projectKey, baseDir, baseDir.resolve(".sonarlint"),
        Collections.singletonList(new DefaultClientInputFile(baseDir, uri, content, userSettings.testFilePattern, languageIdPerFileURI.get(uri))),
        userSettings.analyzerProperties);
      debug("Analysis triggered on " + uri + " with configuration: \n" + configuration.toString());

      long start = System.currentTimeMillis();
      AnalysisResults analysisResults = engine.analyze(configuration, issueListener, logOutput, null);
      int analysisTime = (int) (System.currentTimeMillis() - start);

      return new AnalysisResultsWrapper(analysisResults, analysisTime);
    }
  }

  @VisibleForTesting
  Path findBaseDir(URI uri) {
    return findBaseDir(workspaceFolders, uri);
  }

  @VisibleForTesting
  static Path findBaseDir(List<String> workspaceFolders, URI uri) {
    Path inputFilePath = Paths.get(uri);
    if (!workspaceFolders.isEmpty()) {
      String uriString = inputFilePath.toString();
      for (String folder : workspaceFolders) {
        if (uriString.startsWith(folder)) {
          return Paths.get(folder);
        }
      }
    }

    return inputFilePath.getParent();
  }

  static Optional<Diagnostic> convert(Issue issue) {
    if (issue.getStartLine() != null) {
      Range range = position(issue);
      Diagnostic diagnostic = new Diagnostic();
      DiagnosticSeverity severity = severity(issue.getSeverity());

      diagnostic.setSeverity(severity);
      diagnostic.setRange(range);
      diagnostic.setCode(issue.getRuleKey());
      diagnostic.setMessage(issue.getMessage() + " (" + issue.getRuleKey() + ")");
      diagnostic.setSource(SONARLINT_SOURCE);

      return Optional.of(diagnostic);
    }
    return Optional.empty();
  }

  private static DiagnosticSeverity severity(String severity) {
    switch (severity.toUpperCase(Locale.ENGLISH)) {
      case "BLOCKER":
      case "CRITICAL":
        return DiagnosticSeverity.Error;
      case "MAJOR":
        return DiagnosticSeverity.Warning;
      case "MINOR":
        return DiagnosticSeverity.Information;
      case "INFO":
      default:
        return DiagnosticSeverity.Hint;
    }
  }

  private static Range position(Issue issue) {
    return new Range(
      new Position(
        issue.getStartLine() - 1,
        issue.getStartLineOffset()),
      new Position(
        issue.getEndLine() - 1,
        issue.getEndLineOffset()));
  }

  private static PublishDiagnosticsParams newPublishDiagnostics(URI newUri) {
    PublishDiagnosticsParams p = new PublishDiagnosticsParams();

    p.setDiagnostics(new ArrayList<>());
    p.setUri(newUri.toString());

    return p;
  }

  @Override
  public WorkspaceService getWorkspaceService() {
    return this;
  }

  @Override
  public CompletableFuture<Object> executeCommand(ExecuteCommandParams params) {
    List<Object> args = params.getArguments();
    switch (params.getCommand()) {
      case SONARLINT_OPEN_RULE_DESCRIPTION_COMMAND:
        if (args == null) {
          break;
        }
        if (args.size() != 1) {
          warn("Expecting 1 argument");
        } else {
          String ruleKey = parseToString(args.get(0));
          RuleDetails ruleDetails = engineCache.getOrCreateStandaloneEngine().getRuleDetails(ruleKey);
          String ruleName = ruleDetails.getName();
          String htmlDescription = ruleDetails.getHtmlDescription();
          String type = ruleDetails.getType();
          String severity = ruleDetails.getSeverity();
          client.openRuleDescription(RuleDescription.of(ruleKey, ruleName, htmlDescription, type, severity));
        }
        break;
      case SONARLINT_UPDATE_SERVER_STORAGE_COMMAND:
        List<Object> list = args == null ? null : args.stream().map(SonarLintLanguageServer::parseToMap).collect(Collectors.toList());
        handleUpdateServerStorageCommand(list);
        break;
      case SONARLINT_UPDATE_PROJECT_BINDING_COMMAND:
        Map<String, Object> map = args == null || args.isEmpty() ? null : parseToMap(args.get(0));
        updateBinding(map);
        break;
      default:
        warn("Unimplemented command: " + params.getCommand());
    }
    return CompletableFuture.completedFuture(new Object());
  }

  @Override
  public CompletableFuture<List<? extends SymbolInformation>> symbol(WorkspaceSymbolParams params) {
    return null;
  }

  @Override
  public void didChangeConfiguration(DidChangeConfigurationParams params) {
    Map<String, Object> settings = parseToMap(params.getSettings());
    Map<String, Object> entries = (Map<String, Object>) settings.get(SONARLINT_CONFIGURATION_NAMESPACE);
    userSettings = new UserSettings(entries);
    telemetry.optOut(userSettings.disableTelemetry);
  }

  @Override
  public void didChangeWatchedFiles(DidChangeWatchedFilesParams params) {
    // No watched files
  }

  // TODO this method never seems to get triggered...
  // Users must restart language server after adding new workspace folder.
  @Override
  public void didChangeWorkspaceFolders(DidChangeWorkspaceFoldersParams params) {
    WorkspaceFoldersChangeEvent event = params.getEvent();
    workspaceFolders.removeAll(toList(event.getRemoved()));
    workspaceFolders.addAll(toList(event.getAdded()));
    workspaceFolders.sort(Comparator.reverseOrder());
  }

  // See the changelog for any evolutions on how properties are parsed:
  // https://github.com/eclipse/lsp4j/blob/master/CHANGELOG.md
  // (currently JsonElement, used to be Map<String, Object>)
  private static Map<String, Object> parseToMap(Object obj) {
    return new Gson().fromJson((JsonElement) obj, Map.class);
  }

  // See the changelog for any evolutions on how properties are parsed:
  // https://github.com/eclipse/lsp4j/blob/master/CHANGELOG.md
  private static String parseToString(Object obj) {
    return ((JsonPrimitive) obj).getAsString();
  }

  static class ServerProjectBinding {
    final String serverId;
    final String projectKey;

    ServerProjectBinding(String serverId, String projectKey) {
      this.serverId = serverId;
      this.projectKey = projectKey;
    }
  }
}
