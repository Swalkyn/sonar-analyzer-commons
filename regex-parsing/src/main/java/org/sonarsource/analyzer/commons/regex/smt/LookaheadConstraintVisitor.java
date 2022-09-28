package org.sonarsource.analyzer.commons.regex.smt;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;
import org.sonarsource.analyzer.commons.regex.ast.LookAroundTree;
import org.sonarsource.analyzer.commons.regex.smt.constraints.LookaroundConstraint;
import org.sonarsource.analyzer.commons.regex.smt.constraints.StringConstraint;
import org.sosy_lab.java_smt.api.BooleanFormula;
import org.sosy_lab.java_smt.api.BooleanFormulaManager;
import org.sosy_lab.java_smt.api.StringFormula;
import org.sosy_lab.java_smt.api.StringFormulaManager;

public class LookaheadConstraintVisitor extends BaseConstraintVisitor {
  private final List<BooleanFormula> lookaheadConstraints;

  public LookaheadConstraintVisitor(StringFormulaManager smgr, BooleanFormulaManager bmgr, SatisfiabilityChecker checker) {
    super(smgr, bmgr, checker);
    lookaheadConstraints = new ArrayList<>();
  }

  public BooleanFormula getLookaheadConstraints(StringConstraint root) {
    super.visit(root);
    return bmgr.and(lookaheadConstraints);
  }

  @Override
  protected Iterator<StringConstraint> concatenationElementsIterator(List<StringConstraint> elements) {
    final ListIterator<StringConstraint> listIterator = elements.listIterator(elements.size());
    return new Iterator<StringConstraint>() {
      @Override
      public boolean hasNext() {
        return listIterator.hasPrevious();
      }

      @Override
      public StringConstraint next() {
        return listIterator.previous();
      }
    };
  }

  @Override
  public void visitLookaround(LookaroundConstraint constraint) {
    Iterator<StringFormula> visited = getVisited();
    if (constraint.tree.getDirection() == LookAroundTree.Direction.AHEAD && visited.hasNext()) {
      StringFormula concatenation = visited.next();
      while (visited.hasNext()) {
        concatenation = smgr.concat(visited.next(), concatenation);
      }
      StringFormula variable = checker.newStringVar();
      BooleanFormula prefixFormula = smgr.prefix(variable, concatenation);
      BooleanFormula elementFormula = constraint.tree.getPolarity() == LookAroundTree.Polarity.POSITIVE ?
        smgr.in(variable, constraint.getElement().formula) :
        bmgr.not(smgr.in(concatenation, smgr.concat(constraint.getElement().formula, smgr.all())));
      BooleanFormula formula = bmgr.and(elementFormula, prefixFormula);
      lookaheadConstraints.add(formula);
    }
  }
}
