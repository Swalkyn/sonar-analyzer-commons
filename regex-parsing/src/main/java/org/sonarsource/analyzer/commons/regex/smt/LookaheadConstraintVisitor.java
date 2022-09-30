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

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;
import org.sonarsource.analyzer.commons.regex.ast.LookAroundTree;
import org.sonarsource.analyzer.commons.regex.smt.constraints.LookaroundConstraint;
import org.sonarsource.analyzer.commons.regex.smt.constraints.StringConstraint;
import org.sosy_lab.java_smt.api.BooleanFormula;
import org.sosy_lab.java_smt.api.BooleanFormulaManager;
import org.sosy_lab.java_smt.api.StringFormula;
import org.sosy_lab.java_smt.api.StringFormulaManager;

public class LookaheadConstraintVisitor extends BaseConstraintVisitor {
  private final List<BooleanFormula> lookaheadConstraints;

  public LookaheadConstraintVisitor(StringFormulaManager smgr, BooleanFormulaManager bmgr, SatisfiabilityChecker checker) {
    super(smgr, bmgr, checker);
    lookaheadConstraints = new ArrayList<>();
  }

  public BooleanFormula getLookaheadConstraints(StringConstraint root) {
    super.visit(root);
    return bmgr.and(lookaheadConstraints);
  }

  @Override
  protected Iterator<StringConstraint> concatenationElementsIterator(List<StringConstraint> elements) {
    final ListIterator<StringConstraint> listIterator = elements.listIterator(elements.size());
    return new Iterator<StringConstraint>() {
      @Override
      public boolean hasNext() {
        return listIterator.hasPrevious();
      }

      @Override
      public StringConstraint next() {
        return listIterator.previous();
      }
    };
  }

  @Override
  public void visitLookaround(LookaroundConstraint constraint) {
    Iterator<StringFormula> visited = getVisited();
    if (constraint.direction == LookAroundTree.Direction.AHEAD && visited.hasNext()) {
      StringFormula concatenation = visited.next();
      while (visited.hasNext()) {
        concatenation = smgr.concat(visited.next(), concatenation);
      }
      lookaheadConstraints.add(smgr.equal(concatenation, constraint.continuationVariable));
    }
  }
}
