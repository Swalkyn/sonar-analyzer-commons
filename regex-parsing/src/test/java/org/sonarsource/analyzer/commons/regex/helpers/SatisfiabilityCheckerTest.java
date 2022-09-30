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

import java.util.Set;
import org.assertj.core.api.AbstractBooleanAssert;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.sonarsource.analyzer.commons.regex.MatchType;
import org.sonarsource.analyzer.commons.regex.RegexFeature;
import org.sonarsource.analyzer.commons.regex.RegexParseResult;
import org.sonarsource.analyzer.commons.regex.smt.SatisfiabilityChecker;
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

  private static AbstractBooleanAssert<?> assertSatisfiable(String regex, MatchType matchType, int flags) {
    SatisfiabilityChecker satisfiabilityChecker = new SatisfiabilityChecker(context, matchType);
    RegexParseResult result = parseRegex(regex, flags, RegexFeature.POSSESSIVE_QUANTIFIER);
    return assertThat(satisfiabilityChecker.check(result, true));
  }

  private static AbstractBooleanAssert<?> assertSatisfiable(String regex, MatchType matchType) {
    return assertSatisfiable(regex, matchType, 0);
  }

  private static AbstractBooleanAssert<?> assertSatisfiable(String regex) {
    return assertSatisfiable(regex, MatchType.UNKNOWN, 0);
  }

  @Test
  void testSimpleSat() {
    assertSatisfiable("a").isTrue();
    assertSatisfiable("[a-z]").isTrue();
    assertSatisfiable("[0-9]").isTrue();
    assertSatisfiable("a|b").isTrue();
    assertSatisfiable("^a$").isTrue();
    assertSatisfiable("a*").isTrue();
    assertSatisfiable("a+").isTrue();
    assertSatisfiable("a++").isTrue();
    assertSatisfiable("abc").isTrue();
    assertSatisfiable("abc(d|e)").isTrue();
  }

  @Test
  void testPositiveLookaheadSat() {
    assertSatisfiable("(?=a)a").isTrue();
    assertSatisfiable("(?=a)..").isTrue();
    assertSatisfiable("(?=a)ab").isTrue();
    assertSatisfiable("(?=a)(?=.b)ab").isTrue();
    assertSatisfiable("(?=a)a(?=b)b").isTrue();
    assertSatisfiable("(?=abc)ab(?=cd)cde").isTrue();
    assertSatisfiable("(?:a(?=c)|b(?=d))c").isTrue();
    assertSatisfiable("a(?=b)").isTrue();
    assertSatisfiable("(?:-(?:one|[0-9]+([a-z](?=[^a-z]|$)|st|nd|rd|th)?))*").isTrue();
//    assertSatisfiable("(..(?=ab))*").isTrue();  -> Need support for loop unrolling
    assertSatisfiable("(?=abc)ab", MatchType.PARTIAL).isTrue();
  }

  @Test
  void testPositiveLookaheadUnsat() {
    assertSatisfiable("(?=a)b").isFalse();
    assertSatisfiable("(?=ac)ab").isFalse();
    assertSatisfiable("(?=a)bc").isFalse();
    assertSatisfiable("(?=a)(?=b).").isFalse();
    assertSatisfiable("(?=abcd)ab(?=.e)..").isFalse();
    assertSatisfiable("(?=ab).(?:a(?=c)|b(?=d))c").isFalse();
    assertSatisfiable("(?=[ab])(?=[bc])[ac]").isFalse();
    assertSatisfiable("(?=a)[^ba]").isFalse();
    assertSatisfiable("(?=abc)ab", MatchType.FULL).isFalse();
  }

  @Test
  void testNegativeLookaheadSat() {
    assertSatisfiable("(?!ab)..").isTrue();
    assertSatisfiable("(?!ab)..c").isTrue();
    assertSatisfiable("a(?!:abc):ab", MatchType.PARTIAL).isTrue();
    assertSatisfiable("(?!abc)ab", MatchType.PARTIAL).isTrue();

  }

  @Test
  void testNegativeLookaheadUnsat() {
    assertSatisfiable("(?!a)a").isFalse();
    assertSatisfiable("(?!a)ab").isFalse();
    assertSatisfiable("(?!a|b)a").isFalse();
    assertSatisfiable("(?!ab)ab").isFalse();
    assertSatisfiable("(?!ab)abcd").isFalse();
    assertSatisfiable("(?!.).").isFalse();
    assertSatisfiable("(?!.)ab").isFalse();
//    assertSatisfiable("(?:a(?!bc))+bc").isFalse();  -> Need support for loop unrolling
//    assertSatisfiable("(?:(x|y)(?!bc))+bc").isFalse();  -> Need support for loop unrolling
  }

  @Test
  void testPositiveLookbehindSat() {
    assertSatisfiable("(?<=a)b").isTrue();
    assertSatisfiable("a(?<=a)").isTrue();
    assertSatisfiable("abc(?<=bc)").isTrue();
    assertSatisfiable("abc(?<=bc)d(?<=cd)e").isTrue();
    assertSatisfiable("a(bc)*(?<=b|c)(?<=.)").isTrue();
  }

  @Test
  void testPositiveLookbehindUnsat() {
    assertSatisfiable("b(?<=a)").isFalse();
    assertSatisfiable("abc(?<=c.)").isFalse();
    assertSatisfiable("ab(?<=b)(?<=d.)").isFalse();
    assertSatisfiable("abc(?<=bc)d(?<=bd)e").isFalse();
    assertSatisfiable("[12](?<=[23])(?<=[13])").isFalse();
  }

  @Test
  void testRepetitionSat() {
//    assertSatisfiable("[12]*(?<=[23])(?<=[13])").isFalse();  -> Timeout
//    assertSatisfiable("a(bc)*(?<!b|c)").isFalse(); -> Timeout
//    assertSatisfiable("(?:a(?!bc)|d)+bc").isTrue();
//    assertSatisfiable("(?:a((?!bc)|d)*)+bc").isTrue();
  }

  @Test
  void testRepetitionUnsat() {
    assertSatisfiable("(?:a(?!bc))+bc").isFalse();
  }

  @Test
  void testNegativeLookbehindSat() {
    assertSatisfiable("b(?<!a)").isTrue();
    assertSatisfiable("abc(?<!c.)").isTrue();
    assertSatisfiable("abc(?<!b)(?<!a\\d)").isTrue();
    assertSatisfiable("ab.(?<!bc)d(?<!cd)e").isTrue();
    assertSatisfiable("\\d(?<![025-9])(?<![13])").isTrue();
  }

  @Test
  void testNegativeLookbehindUnsat() {
    assertSatisfiable("a(?<!a)").isFalse();
    assertSatisfiable("a(?<!.)").isFalse();
    assertSatisfiable("abc(?<!bc)").isFalse();
    assertSatisfiable("abc(?<!bc)d(?<!cd)e").isFalse();
    assertSatisfiable("abc(?<!b|c)").isFalse();
    assertSatisfiable("2(?<![^3])").isFalse();
    assertSatisfiable("[12](?<![^23])(?<![^13])").isFalse();
    assertSatisfiable("[0-9](?<![0245-9])(?<![13])").isFalse();
  }

  @Test
  void testPossessiveQuantifierSat() {
    assertSatisfiable("x*+y").isTrue();
    assertSatisfiable("x*+y").isTrue();
    assertSatisfiable("x++y").isTrue();
    assertSatisfiable("(xy)++z").isTrue();
    assertSatisfiable("(xy)++x").isTrue();
    assertSatisfiable("([a-y])++z").isTrue();
    assertSatisfiable("([0-8])++[1-9]").isTrue();
  }

  @Test
  void testPossessiveQuantifierUnsat() {
    assertSatisfiable("x++x").isFalse();
    assertSatisfiable("(xy)++xyz").isFalse();
    assertSatisfiable("[a-z]++z").isFalse();
    assertSatisfiable("[0-9]++[13579]").isFalse();
    assertSatisfiable(".++.").isFalse();
  }

  @Test
  void testCombinedSat() {
    assertSatisfiable("x++(?<=x)").isTrue();
    assertSatisfiable("x++..(?<=x)").isTrue();
  }

  @Test
  void testCombinedUnsat() {
    assertSatisfiable("x++.(?<=x)").isFalse();
  }

  @Test
  void testFullmatchSat() {
  }

  @Test
  void testFullmatchUnsat() {

  }
}
