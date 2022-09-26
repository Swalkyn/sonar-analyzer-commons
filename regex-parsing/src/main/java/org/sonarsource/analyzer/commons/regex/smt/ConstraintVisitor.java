package org.sonarsource.analyzer.commons.regex.smt;

import org.sonarsource.analyzer.commons.regex.smt.constraints.ConcatenationConstraint;
import org.sonarsource.analyzer.commons.regex.smt.constraints.DisjunctionConstraint;
import org.sonarsource.analyzer.commons.regex.smt.constraints.LookaroundConstraint;
import org.sonarsource.analyzer.commons.regex.smt.constraints.SimpleStringConstraint;
import org.sonarsource.analyzer.commons.regex.smt.constraints.StringConstraint;

public interface ConstraintVisitor {
  void visit(StringConstraint constraint);
  void visitConcatenation(ConcatenationConstraint constraint);
  void visitDisjunction(DisjunctionConstraint constraint);
  void visitLookaround(LookaroundConstraint constraint);
  void visitSimple(SimpleStringConstraint constraint);
}
