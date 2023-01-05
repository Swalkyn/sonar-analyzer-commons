/*
 * SonarSource Analyzers Regex Parsing Commons
 * Copyright (C) 2009-2022 SonarSource SA
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
package org.sonarsource.analyzer.commons.regex.finders;

import java.util.ArrayList;
import org.sonarsource.analyzer.commons.regex.MatchType;
import org.sonarsource.analyzer.commons.regex.RegexIssueReporter;
import org.sonarsource.analyzer.commons.regex.RegexParseResult;
import org.sonarsource.analyzer.commons.regex.ast.RegexBaseVisitor;
import org.sonarsource.analyzer.commons.regex.smt.SatisfiabilityChecker;
import org.sosy_lab.java_smt.api.SolverContext;

public class UnsatisfiableRegexFinder extends RegexBaseVisitor {
  private final RegexIssueReporter.ElementIssue reporter;
  private final SolverContext context;

  public UnsatisfiableRegexFinder(RegexIssueReporter.ElementIssue reporter, SolverContext context) {
    this.reporter = reporter;
    this.context = context;
  }

  @Override
  public void visit(RegexParseResult regexParseResult) {
    boolean satisfiable = new SatisfiabilityChecker(context, MatchType.FULL).check(regexParseResult, true);
    if (!satisfiable) {
      reporter.report(regexParseResult.getResult(), "UNSAT", null, new ArrayList<>());
    }
  }
}
