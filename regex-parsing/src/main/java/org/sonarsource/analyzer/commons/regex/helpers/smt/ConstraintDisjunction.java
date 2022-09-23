package org.sonarsource.analyzer.commons.regex.helpers.smt;

import java.util.ArrayList;
import java.util.List;
import org.sonarsource.analyzer.commons.regex.ast.DisjunctionTree;
import org.sonarsource.analyzer.commons.regex.ast.ReturningRegexVisitor;
import org.sosy_lab.java_smt.api.BooleanFormula;
import org.sosy_lab.java_smt.api.BooleanFormulaManager;
import org.sosy_lab.java_smt.api.RegexFormula;
import org.sosy_lab.java_smt.api.StringFormula;
import org.sosy_lab.java_smt.api.StringFormulaManager;

public class ConstraintDisjunction {
  private final StringFormulaManager smgr;
  private final BooleanFormulaManager bmgr;
  private final SatisfiabilityChecker checker;

  private RegexFormula regexUnion;
  private List<StringConstraint> stringConstraints;

  public ConstraintDisjunction(StringFormulaManager smgr, BooleanFormulaManager bmgr, SatisfiabilityChecker checker) {
    this.smgr = smgr;
    this.bmgr = bmgr;
    this.checker = checker;
  }

  public Constraint of(DisjunctionTree tree) {
    regexUnion = null;
    stringConstraints = new ArrayList<>();

    tree.getAlternatives().forEach(alternative ->
      checker.visit(alternative).consume(
        rc -> regexUnion = regexUnion == null ? rc.formula : smgr.union(rc.formula, regexUnion),
        stringConstraints::add
      )
    );
    if (stringConstraints.size() > 1) {
      if (regexUnion != null) {
        stringConstraints.add(checker.convert(regexUnion));
      }
      StringFormula unionVar = checker.newStringVar();
      BooleanFormula unionConstraint = stringConstraints.stream().map(sc -> bmgr.and(smgr.equal(unionVar, sc.stringVar), sc.formula)).collect(bmgr.toDisjunction());
      return new StringConstraint(unionVar, unionConstraint);
    } else {
      return new RegexConstraint(regexUnion);
    }
  }
}
