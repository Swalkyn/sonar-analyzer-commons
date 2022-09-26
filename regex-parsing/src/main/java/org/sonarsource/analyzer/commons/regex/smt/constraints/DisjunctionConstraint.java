package org.sonarsource.analyzer.commons.regex.smt.constraints;

import java.util.List;
import org.sonarsource.analyzer.commons.regex.smt.ConstraintVisitor;
import org.sosy_lab.java_smt.api.BooleanFormula;
import org.sosy_lab.java_smt.api.StringFormula;

public class DisjunctionConstraint extends StringConstraint {
  private final List<StringConstraint> elements;

  public DisjunctionConstraint(StringFormula stringVar, BooleanFormula formula, List<StringConstraint> elements) {
    super(stringVar, formula);
    this.elements = elements;
  }

  public List<StringConstraint> getElements() {
    return elements;
  }

  @Override
  public void accept(ConstraintVisitor visitor) {
    visitor.visitDisjunction(this);
  }
}
