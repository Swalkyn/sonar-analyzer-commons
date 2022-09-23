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
