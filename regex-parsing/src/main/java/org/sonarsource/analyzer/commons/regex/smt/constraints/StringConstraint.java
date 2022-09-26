package org.sonarsource.analyzer.commons.regex.smt.constraints;

import java.util.function.Consumer;
import java.util.function.Function;
import org.sonarsource.analyzer.commons.regex.smt.ConstraintVisitor;
import org.sosy_lab.java_smt.api.BooleanFormula;
import org.sosy_lab.java_smt.api.StringFormula;

public abstract class StringConstraint implements Constraint {
  public final StringFormula stringVar;
  public final BooleanFormula formula;

  public StringConstraint(StringFormula stringVar, BooleanFormula formula) {
    this.stringVar = stringVar;
    this.formula = formula;
  }

  public abstract void accept(ConstraintVisitor visitor);

  @Override
  public void consume(Consumer<RegexConstraint> rc, Consumer<StringConstraint> sc) {
    sc.accept(this);
  }

  @Override
  public <T> T process(Function<RegexConstraint, T> rf, Function<StringConstraint, T> sf) {
    return sf.apply(this);
  }
}
