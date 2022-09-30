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
package org.sonarsource.analyzer.commons.regex.ast;

import org.sonarsource.analyzer.commons.regex.RegexParseResult;

public interface ReturningRegexVisitor<T> {

  default T visit(RegexTree tree) {
    return tree.accept(this);
  }
  T visit(RegexParseResult regexParseResult);

  T visitBackReference(BackReferenceTree tree);

  T visitCharacter(CharacterTree tree);

  T visitSequence(SequenceTree tree);

  T visitDisjunction(DisjunctionTree tree);

  /** Generic for all 4 different kinds of GroupTree(s) */
  T visitGroup(GroupTree tree);

  T visitCapturingGroup(CapturingGroupTree tree);

  T visitNonCapturingGroup(NonCapturingGroupTree tree);

  T visitAtomicGroup(AtomicGroupTree tree);

  T visitLookAround(LookAroundTree tree);

  T visitRepetition(RepetitionTree tree);

  T visitCharacterClass(CharacterClassTree tree);

  T visitCharacterRange(CharacterRangeTree tree);

  T visitCharacterClassUnion(CharacterClassUnionTree tree);

  T visitCharacterClassIntersection(CharacterClassIntersectionTree tree);

  T visitDot(DotTree tree);

  T visitEscapedCharacterClass(EscapedCharacterClassTree tree);

  T visitBoundary(BoundaryTree boundaryTree);

  T visitMiscEscapeSequence(MiscEscapeSequenceTree tree);

  T visitConditionalSubpattern(ConditionalSubpatternTree tree);
}
