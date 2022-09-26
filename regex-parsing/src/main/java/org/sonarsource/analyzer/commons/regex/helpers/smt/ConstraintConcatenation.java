package org.sonarsource.analyzer.commons.regex.helpers.smt;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import org.sonarsource.analyzer.commons.regex.ast.SequenceTree;
import org.sosy_lab.java_smt.api.BooleanFormula;
import org.sosy_lab.java_smt.api.BooleanFormulaManager;
import org.sosy_lab.java_smt.api.RegexFormula;
import org.sosy_lab.java_smt.api.StringFormula;
import org.sosy_lab.java_smt.api.StringFormulaManager;

public class ConstraintConcatenation {
  private final StringFormulaManager smgr;
  private final BooleanFormulaManager bmgr;
  private final SatisfiabilityChecker checker;
  private StringBuilder sb = new StringBuilder();
  private RegexFormula concatFormula = null;
  private List<StringConstraint> stringConstraints = new ArrayList<>();

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
    }
    stringConstraints.add(constraint);
  }

  public Constraint of(SequenceTree tree) {
    sb = new StringBuilder();
    concatFormula = null;
    stringConstraints = new ArrayList<>();

    tree.getItems().forEach(item -> {
      Constraint constraint = checker.visit(item);
      constraint.consume(this::concatenateRegexConstraint, this::concatenateStringConstraint);
    });

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
