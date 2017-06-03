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
import java.io.PrintWriter;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.stream.Collectors;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.sonarlint.daemon.services.StandaloneSonarLintImpl;
import org.sonarsource.sonarlint.daemon.proto.SonarlintDaemon.AnalyzeContentRequest;
import org.sonarsource.sonarlint.daemon.proto.SonarlintDaemon.AnalyzeContentRequest.Builder;
import org.sonarsource.sonarlint.daemon.proto.SonarlintDaemon.Issue;

import io.grpc.stub.StreamObserver;

public class AnalyzerServlet extends HttpServlet {

  @Override
  protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
    String postBody = req.getReader().lines().collect(Collectors.joining(System.lineSeparator()));
    if (postBody == null || postBody.isEmpty()) {
      resp.getWriter().write("No content");
      resp.setStatus(400);
      return;
    }
    System.out.println("content:");
    System.out.println(postBody);

    Path sonarlintHome = Paths.get(".");

    StandaloneSonarLintImpl sonarlint = new StandaloneSonarLintImpl(Utils.getAnalyzers(sonarlintHome));
    Builder build = AnalyzeContentRequest.newBuilder();
    build.setCharset("UTF-8");

    build.setContent(postBody);

    try (PrintWriter writer = resp.getWriter()) {
      writer.write("starting Analysis\n");
      sonarlint.analyzeContent(build.build(), new StreamObserver<Issue>() {

        @Override
        public void onCompleted() {
          writer.write("onCompleted\n");
        }

        @Override
        public void onError(Throwable arg0) {
          arg0.printStackTrace();
          writer.write("onError\n");
        }

        @Override
        public void onNext(Issue arg0) {
          writer.write("onNext " + arg0 + "\n");
        }
      });

      writer.write("Analysis done");
    }
    resp.setStatus(200);
  }
}
