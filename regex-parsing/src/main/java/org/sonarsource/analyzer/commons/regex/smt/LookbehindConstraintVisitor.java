package org.sonarsource.analyzer.commons.regex.smt;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import org.sonarsource.analyzer.commons.regex.ast.LookAroundTree;
import org.sonarsource.analyzer.commons.regex.smt.constraints.LookaroundConstraint;
import org.sonarsource.analyzer.commons.regex.smt.constraints.StringConstraint;
import org.sosy_lab.java_smt.api.BooleanFormula;
import org.sosy_lab.java_smt.api.BooleanFormulaManager;
import org.sosy_lab.java_smt.api.StringFormula;
import org.sosy_lab.java_smt.api.StringFormulaManager;

public class LookbehindConstraintVisitor extends BaseConstraintVisitor {
  private final List<BooleanFormula> lookaheadConstraints;

  LookbehindConstraintVisitor(StringFormulaManager smgr, BooleanFormulaManager bmgr, SatisfiabilityChecker checker) {
    super(smgr, bmgr, checker);
    lookaheadConstraints = new ArrayList<>();
  }

  public BooleanFormula getLookbehindConstraints(StringConstraint root) {
    super.visit(root);
    return bmgr.and(lookaheadConstraints);
  }

  @Override
  Iterator<StringConstraint> concatenationElementsIterator(List<StringConstraint> elements) {
    return elements.listIterator();
  }

  @Override
  public void visitLookaround(LookaroundConstraint constraint) {
    Iterator<StringFormula> visited = getVisited();
    if (constraint.tree.getDirection() == LookAroundTree.Direction.BEHIND && visited.hasNext()) {
      StringFormula concatenation = visited.next();
      while (visited.hasNext()) {
        concatenation = smgr.concat(concatenation, visited.next());
      }
      StringFormula variable = checker.newStringVar();
      BooleanFormula prefixFormula = smgr.suffix(variable, concatenation);
      BooleanFormula elementFormula = constraint.tree.getPolarity() == LookAroundTree.Polarity.POSITIVE ?
        smgr.in(variable, constraint.getElement().formula) :
        bmgr.not(smgr.in(concatenation, smgr.concat(smgr.all(), constraint.getElement().formula)));
      BooleanFormula formula = bmgr.and(elementFormula, prefixFormula);
      lookaheadConstraints.add(formula);
    }
  }
}
