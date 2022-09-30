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
package org.sonarsource.analyzer.commons.regex.smt;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Iterator;
import java.util.List;
import org.sonarsource.analyzer.commons.regex.smt.constraints.ConcatenationConstraint;
import org.sonarsource.analyzer.commons.regex.smt.constraints.DisjunctionConstraint;
import org.sonarsource.analyzer.commons.regex.smt.constraints.LookaroundConstraint;
import org.sonarsource.analyzer.commons.regex.smt.constraints.SimpleStringConstraint;
import org.sonarsource.analyzer.commons.regex.smt.constraints.StringConstraint;
import org.sosy_lab.java_smt.api.BooleanFormulaManager;
import org.sosy_lab.java_smt.api.StringFormula;
import org.sosy_lab.java_smt.api.StringFormulaManager;

public abstract class BaseConstraintVisitor implements ConstraintVisitor {
  protected final StringFormulaManager smgr;
  protected final BooleanFormulaManager bmgr;
  protected final SatisfiabilityChecker checker;
  private final Deque<StringFormula> visited;

  BaseConstraintVisitor(StringFormulaManager smgr, BooleanFormulaManager bmgr, SatisfiabilityChecker checker) {
    visited = new ArrayDeque<>();
    this.smgr = smgr;
    this.bmgr = bmgr;
    this.checker = checker;
  }

  public Iterator<StringFormula> getVisited() {
    return visited.descendingIterator();
  }

  abstract Iterator<StringConstraint> concatenationElementsIterator(List<StringConstraint> elements);

  @Override
  public void visit(StringConstraint constraint) {
    constraint.accept(this);
  }

  @Override
  public void visitConcatenation(ConcatenationConstraint constraint) {
    concatenationElementsIterator(constraint.getElements()).forEachRemaining(element -> {
      visit(element);
      visited.push(element.stringVar);
    });
    constraint.getElements().forEach(x -> visited.pop());
  }

  @Override
  public void visitDisjunction(DisjunctionConstraint constraint) {
    for (StringConstraint element: constraint.getElements()) {
      visit(element);
    }
  }

  @Override
  public void visitLookaround(LookaroundConstraint constraint) {}

  @Override
  public void visitSimple(SimpleStringConstraint constraint) {}
}
