package org.sonarsource.analyzer.commons.regex.smt.constraints;

import org.sonarsource.analyzer.commons.regex.ast.LookAroundTree;
import org.sonarsource.analyzer.commons.regex.smt.ConstraintVisitor;
import org.sosy_lab.java_smt.api.BooleanFormula;
import org.sosy_lab.java_smt.api.StringFormula;

public class LookaroundConstraint extends StringConstraint {
  public final StringFormula continuationVariable;
  public final LookAroundTree tree;

  public LookaroundConstraint(StringFormula stringVar, BooleanFormula formula, StringFormula continuationVariable, LookAroundTree tree) {
    super(stringVar, formula);
    this.continuationVariable = continuationVariable;
    this.tree = tree;
  }

  @Override
  public void accept(ConstraintVisitor visitor) {
    visitor.visitLookaround(this);
  }
}
