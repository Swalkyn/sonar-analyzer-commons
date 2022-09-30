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
import java.util.List;
import org.sonarsource.analyzer.commons.regex.ast.DisjunctionTree;
import org.sonarsource.analyzer.commons.regex.smt.constraints.RegexConstraint;
import org.sonarsource.analyzer.commons.regex.smt.constraints.Constraint;
import org.sonarsource.analyzer.commons.regex.smt.constraints.DisjunctionConstraint;
import org.sonarsource.analyzer.commons.regex.smt.constraints.StringConstraint;
import org.sosy_lab.java_smt.api.BooleanFormula;
import org.sosy_lab.java_smt.api.BooleanFormulaManager;
import org.sosy_lab.java_smt.api.RegexFormula;
import org.sosy_lab.java_smt.api.StringFormula;
import org.sosy_lab.java_smt.api.StringFormulaManager;

public class ConstraintDisjunction {
  private final StringFormulaManager smgr;
  private final BooleanFormulaManager bmgr;
  private final SatisfiabilityChecker checker;

  private RegexFormula regexUnion;
  private List<StringConstraint> stringConstraints;

  public ConstraintDisjunction(StringFormulaManager smgr, BooleanFormulaManager bmgr, SatisfiabilityChecker checker) {
    this.smgr = smgr;
    this.bmgr = bmgr;
    this.checker = checker;
  }

  public Constraint of(DisjunctionTree tree) {
    regexUnion = null;
    stringConstraints = new ArrayList<>();

    tree.getAlternatives().forEach(alternative ->
      checker.visit(alternative).consume(
        rc -> regexUnion = regexUnion == null ? rc.formula : smgr.union(rc.formula, regexUnion),
        stringConstraints::add
      )
    );
    if (stringConstraints.size() > 1) {
      if (regexUnion != null) {
        stringConstraints.add(checker.convert(regexUnion));
      }
      StringFormula unionVar = checker.newStringVar();
      BooleanFormula unionConstraint = stringConstraints.stream().map(sc -> bmgr.and(smgr.equal(unionVar, sc.stringVar), sc.formula)).collect(bmgr.toDisjunction());
      return new DisjunctionConstraint(unionVar, unionConstraint, stringConstraints);
    } else {
      return new RegexConstraint(regexUnion);
    }
  }
}
