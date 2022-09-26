package org.sonarsource.analyzer.commons.regex.helpers.smt;

import org.sosy_lab.java_smt.api.BooleanFormula;
import org.sosy_lab.java_smt.api.StringFormula;

public class SimpleStringConstraint extends StringConstraint {
  public SimpleStringConstraint(StringFormula stringVar, BooleanFormula formula) {
    super(stringVar, formula);
  }

  @Override
  public void accept(ConstraintVisitor visitor) {
    visitor.visitSimple(this);
  }
}
