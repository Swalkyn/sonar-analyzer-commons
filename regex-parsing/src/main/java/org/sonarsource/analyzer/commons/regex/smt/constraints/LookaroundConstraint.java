package org.sonarsource.analyzer.commons.regex.smt.constraints;

import org.sonarsource.analyzer.commons.regex.ast.LookAroundTree;
import org.sonarsource.analyzer.commons.regex.smt.ConstraintVisitor;
import org.sosy_lab.java_smt.api.BooleanFormula;
import org.sosy_lab.java_smt.api.StringFormula;

public class LookaroundConstraint extends StringConstraint {
  public final RegexConstraint element;
  public final LookAroundTree tree;

  public LookaroundConstraint(StringFormula stringVar, BooleanFormula formula, RegexConstraint element, LookAroundTree tree) {
    super(stringVar, formula);
    this.element = element;
    this.tree = tree;
  }

  public RegexConstraint getElement() {
    return element;
  }

  @Override
  public void accept(ConstraintVisitor visitor) {
    visitor.visitLookaround(this);
  }
}
