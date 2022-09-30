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
