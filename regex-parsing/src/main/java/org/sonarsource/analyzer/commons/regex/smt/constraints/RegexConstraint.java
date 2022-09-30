/*
 * SonarSource Analyzers Regex Parsing Commons
 * Copyright (C) 2009-2022 SonarSource SA
 * mailto:info AT sonarsource DOT com
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 */
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
