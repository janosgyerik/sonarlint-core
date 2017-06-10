/*
 * SonarLint Daemon
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
package org.sonarlint.daemon;

import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import javax.servlet.ServletException;
import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.sonar.api.utils.text.JsonWriter;
import org.sonarlint.daemon.services.StandaloneSonarLintImpl;
import org.sonarsource.sonarlint.daemon.proto.SonarlintDaemon;
import org.sonarsource.sonarlint.daemon.proto.SonarlintDaemon.AnalyzeContentRequest;
import org.sonarsource.sonarlint.daemon.proto.SonarlintDaemon.AnalyzeContentRequest.Builder;
import org.sonarsource.sonarlint.daemon.proto.SonarlintDaemon.Issue;

import io.grpc.stub.StreamObserver;

import static java.nio.charset.StandardCharsets.UTF_8;

public class AnalyzerServlet extends HttpServlet {

  @Override
  protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
    String postBody = req.getReader().lines().collect(Collectors.joining(System.lineSeparator()));
    if (postBody == null || postBody.isEmpty()) {
      resp.getWriter().write("No content");
      resp.setStatus(400);
      return;
    }

    try (ServletOutputStream outputStream = resp.getOutputStream();
         OutputStreamWriter writer = new OutputStreamWriter(outputStream, UTF_8);
         JsonWriter json = JsonWriter.of(writer)) {
      json.beginObject();
      json.name("log");
      json.beginArray();

      String language = req.getParameter("language");
      if (language == null || language.isEmpty()) {
        language = "JavaScript";
        json.value("No language specified, defaulting to " + language);
      }

      Path sonarlintHome = Paths.get(".");
      StandaloneSonarLintImpl sonarlint = new StandaloneSonarLintImpl(Utils.getAnalyzers(sonarlintHome));

      List<Issue> issues = getIssues(json, sonarlint, postBody, language);
      List<SonarlintDaemon.RuleDetails> rules = getRules(json, sonarlint, issues);

      json.endArray();
      writeIssues(rules, issues, json);
      json.endObject();
    }
    resp.setStatus(200);
  }

  private List<SonarlintDaemon.RuleDetails> getRules(final JsonWriter json, StandaloneSonarLintImpl sonarlint, List<Issue> issues) {
    json.value("loading rules");
    List<SonarlintDaemon.RuleDetails> rules = new ArrayList<>();
    Set<String> ruleKeys = issues.stream().map(Issue::getRuleKey).collect(Collectors.toSet());
    for (String ruleKey : ruleKeys) {
      json.value("loading rule " + ruleKey);
      SonarlintDaemon.RuleKey ruleKeyParsed = SonarlintDaemon.RuleKey.newBuilder().setKey(ruleKey).build();
      sonarlint.getRuleDetails(ruleKeyParsed, new StreamObserver<SonarlintDaemon.RuleDetails>() {
        @Override
        public void onNext(SonarlintDaemon.RuleDetails ruleDetails) {
          rules.add(ruleDetails);
          json.value("onNext " + ruleDetails);
        }

        @Override
        public void onError(Throwable throwable) {
          json.value("onError");
        }

        @Override
        public void onCompleted() {
          json.value("onCompleted");
        }
      });
    }
    return rules;
  }

  private List<Issue> getIssues(final JsonWriter json, StandaloneSonarLintImpl sonarlint, String postBody, String language) {
    Builder build = AnalyzeContentRequest.newBuilder();
    build.setCharset("UTF-8");

    build.setContent(postBody);
    build.setLanguage(language);

    List<Issue> issues = new ArrayList<>();
    json.value("starting analysis");
    sonarlint.analyzeContent(build.build(), new StreamObserver<Issue>() {

      @Override
      public void onCompleted() {
        json.value("onCompleted");
      }

      @Override
      public void onError(Throwable arg0) {
        json.value("onError");
      }

      @Override
      public void onNext(Issue arg0) {
        issues.add(arg0);
        json.value("onNext " + arg0);
      }
    });
    json.value("analysis done");
    return issues;
  }

  private void writeIssues(List<SonarlintDaemon.RuleDetails> rules, List<Issue> issues, JsonWriter json) {
    json.prop("total", issues.size());
    json.prop("p", 1);
    json.prop("ps", issues.size());

    json.name("paging")
            .beginObject()
            .prop("pageIndex", 1)
            .prop("pageSize", issues.size())
            .prop("total", issues.size())
            .endObject();

    json.name("issues");
    json.beginArray();
    for (Issue issue : issues) {
      json.beginObject();
      json.prop("rule", issue.getRuleKey());
      json.prop("severity", issue.getSeverity().name());
      json.prop("message", issue.getMessage());

      json.name("textRange")
              .beginObject()
              .prop("startLine", issue.getStartLine())
              .prop("endLine", issue.getEndLine())
              .prop("startOffset", issue.getStartLineOffset())
              .prop("endOffset", issue.getEndLineOffset())
              .endObject();

      json.endObject();
    }
    json.endArray();

    json.name("rules");
    json.beginArray();
    for (SonarlintDaemon.RuleDetails rule : rules) {
      json.beginObject();
      json.prop("key", rule.getKey());
      json.prop("name", rule.getName());
      json.prop("lang", rule.getLanguage());
      json.prop("langName", rule.getLanguage());
      json.endObject();
    }
    json.endArray();
  }
}
