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
package org.sonarsource.analyzer.commons.regex.helpers;

import org.assertj.core.api.AbstractBooleanAssert;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.sonarsource.analyzer.commons.regex.RegexParseResult;
import org.sonarsource.analyzer.commons.regex.ast.FlagSet;
import org.sonarsource.analyzer.commons.regex.helpers.smt.SatisfiabilityChecker;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.java_smt.SolverContextFactory;
import org.sosy_lab.java_smt.api.SolverContext;

import static org.assertj.core.api.Assertions.assertThat;
import static org.sonarsource.analyzer.commons.regex.RegexParserTestUtils.parseRegex;

class SatisfiabilityCheckerTest {
  private static SolverContext context;

  @BeforeAll
  public static void init() throws InvalidConfigurationException {
    context = SolverContextFactory.createSolverContext(SolverContextFactory.Solvers.Z3);
  }

  @AfterAll
  public static void cleanUp() {
    context.close();
  }

  private static AbstractBooleanAssert<?> assertSatisfiable(String regex, FlagSet flagSet) {
    SatisfiabilityChecker satisfiabilityChecker = new SatisfiabilityChecker(context);
    RegexParseResult result = parseRegex(regex);
    return assertThat(satisfiabilityChecker.check(result, true));
  }

  private static AbstractBooleanAssert<?> assertSatisfiable(String regex) {
    return assertSatisfiable(regex, new FlagSet());
  }

  @Test
  void testSimpleSat() {
    assertSatisfiable("a").isTrue();
    assertSatisfiable("[a-z]").isTrue();
    assertSatisfiable("a|b").isTrue();
    assertSatisfiable("^a$").isTrue();
    assertSatisfiable("a*").isTrue();
    assertSatisfiable("a+").isTrue();
    assertSatisfiable("a++").isTrue();
    assertSatisfiable("abc").isTrue();
    assertSatisfiable("abc(d|e)").isTrue();
    assertSatisfiable("(?:a(?!bc)|d)+bc").isTrue();
    assertSatisfiable("(?:a((?!bc)|d)*)+bc").isTrue();
    assertSatisfiable("a(?!:abc):ab").isTrue();
    assertSatisfiable("(?:-(?:one|[0-9]+([a-z](?=[^a-z]|$)|st|nd|rd|th)?))*").isTrue();
    assertSatisfiable("(..(?=ab))*").isTrue();
    assertSatisfiable("(?=a)a").isTrue();
    assertSatisfiable("(?=a)..").isTrue();
    assertSatisfiable("(?=a)ab").isTrue();
    assertSatisfiable("(?!ab)..").isTrue();
    assertSatisfiable("(?<=a)b").isTrue();
    assertSatisfiable("a(?=b)").isTrue();
    assertSatisfiable("(?=abc)ab").isTrue();
    assertSatisfiable("(?!abc)ab").isTrue();
  }

  @Test
  void testSimpleUnsat() {
    assertSatisfiable("(?=a)b").isFalse();
    assertSatisfiable("(?=ac)ab").isFalse();
    assertSatisfiable("(?=a)bc").isFalse();
    assertSatisfiable("(?!a)a").isFalse();
    assertSatisfiable("(?!ab)ab").isFalse();
    assertSatisfiable("(?=a)[^ba]").isFalse();
    assertSatisfiable("(?!.)ab").isFalse();
    assertSatisfiable("(?:a(?!bc))+bc").isFalse();
    assertSatisfiable("(?:(x|y)(?!bc))+bc").isFalse();
  }
}
