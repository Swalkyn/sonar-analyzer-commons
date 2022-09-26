package org.sonarsource.analyzer.commons.regex.helpers.smt;

public interface ConstraintVisitor {
  void visit(StringConstraint constraint);
  void visitConcatenation(ConcatenationConstraint constraint);
  void visitDisjunction(DisjunctionConstraint constraint);
  void visitLookaround(LookaroundConstraint constraint);
  void visitSimple(SimpleStringConstraint constraint);
}
