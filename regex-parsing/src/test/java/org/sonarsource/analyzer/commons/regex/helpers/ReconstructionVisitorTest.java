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

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;
import org.sonarsource.analyzer.commons.regex.RegexParser;
import org.sonarsource.analyzer.commons.regex.RegexSource;
import org.sonarsource.analyzer.commons.regex.ast.FlagSet;
import org.sonarsource.analyzer.commons.regex.ast.IndexRange;
import org.sonarsource.analyzer.commons.regex.java.JavaRegexSource;

public class ReconstructionVisitorTest {
  private String parseAndReconstruct(String regex, IndexRange excludeRange) {
    RegexSource source = new JavaRegexSource(regex);
    RegexParser parser = new RegexParser(source, new FlagSet());
    ReconstructionVisitor reconstructionVisitor = new ReconstructionVisitor(excludeRange);
    reconstructionVisitor.visit(parser.parse());
    return reconstructionVisitor.result();
  }

  private void testFullReconstruction(String regex) {
    String reconstructed = parseAndReconstruct(regex, new IndexRange(-1, -1));
    assertEquals(regex, reconstructed);
  }

  private void testPartialReconstruction(String regex, IndexRange excludeRange, String expected) {
    String reconstructed = parseAndReconstruct(regex, excludeRange);
    assertEquals(expected, reconstructed);
  }

  @Test
  void testFullReconstruction() {
    testFullReconstruction("abcd");
    testFullReconstruction("a(?:bc)d");
    testFullReconstruction("a(bc)|d");
    testFullReconstruction("abc{2,10}d");
    testFullReconstruction("a?bc++");
    testFullReconstruction("(?=ac)ab");
    testFullReconstruction("(\\W|^)[\\w.\\-]{0,25}@(yahoo|hotmail|gmail)\\.com(\\W|$)");
  }

  @Test
  void testPartialReconstruction() {
    testPartialReconstruction("abcd", new IndexRange(1, 2), "cd");
    testPartialReconstruction("a(?:b|c)d", new IndexRange(4, 5), "d");
    testPartialReconstruction("ab|cd", new IndexRange(1, 2), "");
    testPartialReconstruction("ab|cd+", new IndexRange(3, 4), "d+");
    testPartialReconstruction("(?=ac)ab", new IndexRange(0, 6), "ab");
    testPartialReconstruction("(\\W|^)[\\w.\\-]{0,25}@(yahoo|hotmail|gmail)\\.com(\\W|$)", new IndexRange(25, 26), "\\.com(\\W|$)");
  }
}
