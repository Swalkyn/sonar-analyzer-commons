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

import java.util.Collections;
import org.sonarsource.analyzer.commons.regex.MatchType;
import org.sonarsource.analyzer.commons.regex.RegexIssueReporter;
import org.sonarsource.analyzer.commons.regex.RegexParseResult;
import org.sonarsource.analyzer.commons.regex.ast.FinalState;
import org.sonarsource.analyzer.commons.regex.ast.LookAroundTree;
import org.sonarsource.analyzer.commons.regex.ast.RegexBaseVisitor;
import org.sonarsource.analyzer.commons.regex.ast.RegexTree;
import org.sonarsource.analyzer.commons.regex.helpers.ReconstructionVisitor;
import org.sonarsource.analyzer.commons.regex.helpers.RegexTreeHelper;

public class FailingLookaheadFinder extends RegexBaseVisitor {

  private static final String MESSAGE = "Remove or fix this lookahead assertion that can never be true.";

  private final RegexIssueReporter.ElementIssue regexElementIssueReporter;
  private final FinalState finalState;
  private final MatchType matchType;

  private RegexTree root = null;

  public FailingLookaheadFinder(RegexIssueReporter.ElementIssue regexElementIssueReporter, FinalState finalState) {
    this(regexElementIssueReporter, finalState, MatchType.NOT_SUPPORTED);
  }

  public FailingLookaheadFinder(RegexIssueReporter.ElementIssue regexElementIssueReporter, FinalState finalState, MatchType matchType) {
    this.regexElementIssueReporter = regexElementIssueReporter;
    this.finalState = finalState;
    this.matchType = matchType;
  }

  @Override
  public void visit(RegexParseResult parseResult) {
    root = parseResult.getResult();
    super.visit(parseResult);
  }

  @Override
  public void visitLookAround(LookAroundTree tree) {
    if (tree.getDirection() == LookAroundTree.Direction.AHEAD && doesLookaheadContinuationAlwaysFail(tree)) {
      regexElementIssueReporter.report(tree, MESSAGE, null, Collections.emptyList());
    }
    super.visitLookAround(tree);
  }

  private boolean doesLookaheadContinuationAlwaysFail(LookAroundTree lookAround) {
    ReconstructionVisitor reconstructionVisitor = new ReconstructionVisitor(lookAround.getRange());
    reconstructionVisitor.visit(root);
    String regexA = lookAround.getElement().getText();
    String regexB = reconstructionVisitor.result();

    if (lookAround.getPolarity() == LookAroundTree.Polarity.NEGATIVE) {
      return !RegexTreeHelper.intersects(regexA, regexB, true, false, false, true);
    }
    return !RegexTreeHelper.intersects(regexA, regexB, true, matchType != MatchType.FULL, true, false);
  }
}
