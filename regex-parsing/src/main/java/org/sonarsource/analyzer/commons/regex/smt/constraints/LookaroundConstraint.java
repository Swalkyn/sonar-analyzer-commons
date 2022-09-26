package org.sonarsource.analyzer.commons.regex.smt.constraints;

import org.sonarsource.analyzer.commons.regex.smt.ConstraintVisitor;
import org.sosy_lab.java_smt.api.BooleanFormula;
import org.sosy_lab.java_smt.api.StringFormula;

public class LookaroundConstraint extends StringConstraint {
  public final StringConstraint element;

  public LookaroundConstraint(StringFormula stringVar, BooleanFormula formula, StringConstraint element) {
    super(stringVar, formula);
    this.element = element;
  }

  public StringConstraint getElement() {
    return element;
  }

  @Override
  public void accept(ConstraintVisitor visitor) {
    visitor.visit(this);
  }
}
