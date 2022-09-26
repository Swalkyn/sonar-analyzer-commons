package org.sonarsource.analyzer.commons.regex.smt.constraints;

import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.UnaryOperator;
import org.sosy_lab.java_smt.api.RegexFormula;

public class RegexConstraint implements Constraint {
  public final RegexFormula formula;
  public final Optional<String> simpleChar;

  public RegexConstraint(RegexFormula formula, String simpleChar) {
    this.formula = formula;
    this.simpleChar = Optional.of(simpleChar);
  }
  public RegexConstraint(RegexFormula formula) {
    this.formula = formula;
    this.simpleChar = Optional.empty();
  }

  public RegexConstraint map(UnaryOperator<RegexFormula> fun) {
    return new RegexConstraint(fun.apply(this.formula));
  }

  @Override
  public void consume(Consumer<RegexConstraint> rc, Consumer<StringConstraint> sc) {
    rc.accept(this);
  }

  @Override
  public <T> T process(Function<RegexConstraint, T> rf, Function<StringConstraint, T> sf) {
    return rf.apply(this);
  }
}
