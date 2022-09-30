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
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.sonarsource.analyzer.commons.regex.ast.SequenceTree;
import org.sonarsource.analyzer.commons.regex.smt.constraints.ConcatenationConstraint;
import org.sonarsource.analyzer.commons.regex.smt.constraints.Constraint;
import org.sonarsource.analyzer.commons.regex.smt.constraints.RegexConstraint;
import org.sonarsource.analyzer.commons.regex.smt.constraints.StringConstraint;
import org.sosy_lab.java_smt.api.BooleanFormula;
import org.sosy_lab.java_smt.api.BooleanFormulaManager;
import org.sosy_lab.java_smt.api.RegexFormula;
import org.sosy_lab.java_smt.api.StringFormula;
import org.sosy_lab.java_smt.api.StringFormulaManager;

public class ConstraintConcatenation {
  private final StringFormulaManager smgr;
  private final BooleanFormulaManager bmgr;
  private final SatisfiabilityChecker checker;
  private final List<StringConstraint> stringConstraints = new ArrayList<>();
  private final StringBuilder sb = new StringBuilder();
  private RegexFormula concatFormula = null;

  public ConstraintConcatenation(StringFormulaManager smgr, BooleanFormulaManager bmgr, SatisfiabilityChecker checker) {
    this.smgr = smgr;
    this.bmgr = bmgr;
    this.checker = checker;
  }

  private RegexFormula combineCurrentChars(StringBuilder sb, RegexFormula concatFormula) {
    if (sb.length() != 0) {
      RegexFormula characters = smgr.makeRegex(sb.toString());
      sb.setLength(0);
      return concatFormula == null ? characters : smgr.concat(concatFormula, characters);
    }
    return concatFormula;
  }

  private void concatenateRegexConstraint(RegexConstraint constraint) {
    if (constraint.simpleChar.isPresent()) {
      sb.append(constraint.simpleChar.get());
    } else {
      concatFormula = combineCurrentChars(sb, concatFormula);
      concatFormula = concatFormula == null ? constraint.formula : smgr.concat(concatFormula, constraint.formula);
    }
  }

  private void concatenateStringConstraint(StringConstraint constraint) {
    concatFormula = combineCurrentChars(sb, concatFormula);
    if (concatFormula != null) {
      stringConstraints.add(checker.convert(concatFormula));
      concatFormula = null;
    }
    stringConstraints.add(constraint);
  }

  public Constraint of(SequenceTree tree) {
    return this.of(tree.getItems().stream().map(checker::visit));
  }

  public Constraint of(Constraint... constraints) {
    return this.of(Arrays.stream(constraints));
  }

  private Constraint of(Stream<Constraint> constraints) {
    constraints.forEach(constraint -> constraint.consume(this::concatenateRegexConstraint, this::concatenateStringConstraint));

    concatFormula = combineCurrentChars(sb, concatFormula);
    if (!stringConstraints.isEmpty()) {
      if (concatFormula != null) {
        stringConstraints.add(checker.convert(concatFormula));
      }
      StringFormula concatVar = smgr.concat(stringConstraints.stream().map(sc -> sc.stringVar).collect(Collectors.toList()));
      BooleanFormula concatConstraint = stringConstraints.stream().map(sc -> sc.formula).collect(bmgr.toConjunction());
      return new ConcatenationConstraint(concatVar, concatConstraint, stringConstraints);
    } else {
      return new RegexConstraint(concatFormula);
    }
  }
}
