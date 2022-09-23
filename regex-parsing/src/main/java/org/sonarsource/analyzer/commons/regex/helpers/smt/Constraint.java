package org.sonarsource.analyzer.commons.regex.helpers.smt;

import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Function;

public interface Constraint {
  void consume(Consumer<RegexConstraint> rc, Consumer<StringConstraint> sc);
  <T> T process(Function<RegexConstraint, T> rf, Function<StringConstraint, T> sf);

  default Optional<RegexConstraint> getRegexConstraint() {
    return this instanceof RegexConstraint ? Optional.of((RegexConstraint) this) : Optional.empty();
  }
}
