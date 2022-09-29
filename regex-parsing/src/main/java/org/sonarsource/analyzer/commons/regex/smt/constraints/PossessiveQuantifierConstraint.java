package org.sonarsource.analyzer.commons.regex.smt.constraints;

import org.sonarsource.analyzer.commons.regex.smt.ConstraintVisitor;
import org.sosy_lab.java_smt.api.BooleanFormula;
import org.sosy_lab.java_smt.api.StringFormula;

public class PossessiveQuantifierConstraint extends StringConstraint {
  public PossessiveQuantifierConstraint(StringFormula stringVar, BooleanFormula formula) {
    super(stringVar, formula);
  }

  @Override
  public void accept(ConstraintVisitor visitor) {

  }
}
