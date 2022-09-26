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
package org.sonarsource.analyzer.commons.regex.smt;

import java.util.function.Function;
import org.sonarsource.analyzer.commons.regex.RegexParseResult;
import org.sonarsource.analyzer.commons.regex.ast.AtomicGroupTree;
import org.sonarsource.analyzer.commons.regex.ast.BackReferenceTree;
import org.sonarsource.analyzer.commons.regex.ast.BoundaryTree;
import org.sonarsource.analyzer.commons.regex.ast.CapturingGroupTree;
import org.sonarsource.analyzer.commons.regex.ast.CharacterClassIntersectionTree;
import org.sonarsource.analyzer.commons.regex.ast.CharacterClassTree;
import org.sonarsource.analyzer.commons.regex.ast.CharacterClassUnionTree;
import org.sonarsource.analyzer.commons.regex.ast.CharacterRangeTree;
import org.sonarsource.analyzer.commons.regex.ast.CharacterTree;
import org.sonarsource.analyzer.commons.regex.ast.ConditionalSubpatternTree;
import org.sonarsource.analyzer.commons.regex.ast.DisjunctionTree;
import org.sonarsource.analyzer.commons.regex.ast.DotTree;
import org.sonarsource.analyzer.commons.regex.ast.EscapedCharacterClassTree;
import org.sonarsource.analyzer.commons.regex.ast.GroupTree;
import org.sonarsource.analyzer.commons.regex.ast.LookAroundTree;
import org.sonarsource.analyzer.commons.regex.ast.MiscEscapeSequenceTree;
import org.sonarsource.analyzer.commons.regex.ast.NonCapturingGroupTree;
import org.sonarsource.analyzer.commons.regex.ast.Quantifier;
import org.sonarsource.analyzer.commons.regex.ast.RepetitionTree;
import org.sonarsource.analyzer.commons.regex.ast.ReturningRegexVisitor;
import org.sonarsource.analyzer.commons.regex.ast.SequenceTree;
import org.sonarsource.analyzer.commons.regex.smt.constraints.Constraint;
import org.sonarsource.analyzer.commons.regex.smt.constraints.LookaroundConstraint;
import org.sonarsource.analyzer.commons.regex.smt.constraints.RegexConstraint;
import org.sonarsource.analyzer.commons.regex.smt.constraints.SimpleStringConstraint;
import org.sonarsource.analyzer.commons.regex.smt.constraints.StringConstraint;
import org.sosy_lab.java_smt.api.BooleanFormulaManager;
import org.sosy_lab.java_smt.api.ProverEnvironment;
import org.sosy_lab.java_smt.api.RegexFormula;
import org.sosy_lab.java_smt.api.SolverContext;
import org.sosy_lab.java_smt.api.SolverException;
import org.sosy_lab.java_smt.api.StringFormula;
import org.sosy_lab.java_smt.api.StringFormulaManager;

public class SatisfiabilityChecker implements ReturningRegexVisitor<Constraint> {

  private final SolverContext context;
  private final StringFormulaManager smgr;
  private final BooleanFormulaManager bmgr;
  private final ConstraintConcatenation constraintConcatenation;
  private final ConstraintDisjunction constraintDisjunction;
  private final VarNameGenerator varNameGenerator;

  public StringFormula newStringVar() {
    return smgr.makeVariable(varNameGenerator.getFreshName());
  }

  public StringConstraint convert(RegexFormula regexFormula) {
    StringFormula variable = newStringVar();
    return new SimpleStringConstraint(variable, smgr.in(variable, regexFormula));
  }

  private String getSolverChar(CharacterTree tree) {
    return (Character.isAlphabetic(tree.codePointOrUnit())) ?
      tree.characterAsString() : String.format("\\u{%d}", tree.codePointOrUnit());
  }

  public SatisfiabilityChecker(SolverContext solverContext) {
    context = solverContext;
    smgr = solverContext.getFormulaManager().getStringFormulaManager();
    bmgr = solverContext.getFormulaManager().getBooleanFormulaManager();
    constraintConcatenation = new ConstraintConcatenation(smgr, bmgr, this);
    constraintDisjunction = new ConstraintDisjunction(smgr, bmgr, this);
    varNameGenerator = new VarNameGenerator();
  }

  public boolean check(RegexParseResult parseResult, boolean defaultAnswer) {
    if (parseResult.getResult() == null) {
      return defaultAnswer;
    }
    StringConstraint constraint = visit(parseResult.getResult()).process(rc -> convert(rc.formula), Function.identity());
    try (ProverEnvironment prover = context.newProverEnvironment()) {
      prover.addConstraint(constraint.formula);
      System.out.println(context.getFormulaManager().dumpFormula(constraint.formula));
      return !prover.isUnsat();
    } catch (SolverException | InterruptedException e) {
      return true;
    }
  }

  @Override
  public Constraint visit(RegexParseResult regexParseResult) {
    return null;
  }

  @Override
  public Constraint visitBackReference(BackReferenceTree tree) {
    throw new UnsupportedOperationException("Backreferences are not supported");
  }

  @Override
  public RegexConstraint visitCharacter(CharacterTree tree) {
    return new RegexConstraint(smgr.makeRegex(getSolverChar(tree)), getSolverChar(tree));
  }

  @Override
  public Constraint visitSequence(SequenceTree tree) {
    return constraintConcatenation.of(tree);
  }

  @Override
  public Constraint visitDisjunction(DisjunctionTree tree) {
    return constraintDisjunction.of(tree);
  }

  @Override
  public Constraint visitGroup(GroupTree tree) {
    return null;
  }

  @Override
  public Constraint visitCapturingGroup(CapturingGroupTree tree) {
    return visit(tree.getElement());
  }

  @Override
  public Constraint visitNonCapturingGroup(NonCapturingGroupTree tree) {
    if (tree.getElement() != null) {
      return visit(tree.getElement());
    }
    return new RegexConstraint(smgr.none());
  }

  @Override
  public Constraint visitAtomicGroup(AtomicGroupTree tree) {
    return visit(tree.getElement());
  }

  @Override
  public Constraint visitLookAround(LookAroundTree tree) {
    if (tree.getDirection() == LookAroundTree.Direction.BEHIND) {
      Constraint constraint = visit(tree.getElement());
      StringConstraint stringConstraint = constraint.process(rc -> convert(rc.formula), Function.identity());
      StringFormula prefixVariable = newStringVar();
      return new LookaroundConstraint(stringConstraint.stringVar, bmgr.and(stringConstraint.formula, smgr.suffix(stringConstraint.stringVar, prefixVariable)), stringConstraint);
    }
    Constraint constraint = visit(tree.getElement());
    StringConstraint stringConstraint = constraint.process(rc -> convert(rc.formula), Function.identity());
    StringFormula suffixVariable = newStringVar();
    return new LookaroundConstraint(stringConstraint.stringVar, bmgr.and(stringConstraint.formula, smgr.prefix(stringConstraint.stringVar, suffixVariable)), stringConstraint);
  }

  @Override
  public Constraint visitBoundary(BoundaryTree tree) {
    return new RegexConstraint(smgr.makeRegex(""));
  }

  @Override
  public Constraint visitMiscEscapeSequence(MiscEscapeSequenceTree tree) {
    return null;
  }

  @Override
  public Constraint visitRepetition(RepetitionTree tree) {
    Quantifier quantifier = tree.getQuantifier();
    if (quantifier.getMinimumRepetitions() == 0 && quantifier.getMaximumRepetitions() == null && tree.getElement() instanceof DotTree) {
      return new RegexConstraint(smgr.all());
    } else {
      Constraint constraint = visit(tree.getElement());
      return constraint.getRegexConstraint()
        .orElseThrow(UnsupportedOperationException::new)
        .map(formula -> {
          RegexFormula repetition = smgr.cross(formula);
          if (quantifier.getMinimumRepetitions() > 0) {
            repetition = smgr.concat(smgr.times(formula, quantifier.getMinimumRepetitions()), repetition);
          }
          return repetition;
      });
    }
  }

  @Override
  public RegexConstraint visitCharacterClass(CharacterClassTree tree) {
    RegexConstraint constraint = tree.getContents().accept(this).getRegexConstraint()
        .orElseThrow(UnsupportedOperationException::new);
    return tree.isNegated() ? constraint.map(formula -> smgr.complement(smgr.concat(formula, smgr.all()))) : constraint;
  }

  @Override
  public RegexConstraint visitCharacterRange(CharacterRangeTree tree) {
    RegexFormula characterRange = smgr.range(
      smgr.makeString(getSolverChar(tree.getLowerBound())),
      smgr.makeString(getSolverChar(tree.getUpperBound())));
    return new RegexConstraint(characterRange);
  }

  @Override
  public RegexConstraint visitCharacterClassUnion(CharacterClassUnionTree tree) {
    RegexFormula unionFormula = tree.getCharacterClasses().stream()
      .map(charClass -> charClass.accept(this).getRegexConstraint().orElseThrow(UnsupportedOperationException::new).formula)
      .reduce(smgr.makeRegex(""), smgr::union);
    return new RegexConstraint(unionFormula);
  }

  @Override
  public RegexConstraint visitCharacterClassIntersection(CharacterClassIntersectionTree tree) {
    return null;
  }

  @Override
  public RegexConstraint visitDot(DotTree tree) {
    return new RegexConstraint(smgr.allChar());
  }

  @Override
  public RegexConstraint visitEscapedCharacterClass(EscapedCharacterClassTree tree) {
    return null;
  }

  @Override
  public Constraint visitConditionalSubpattern(ConditionalSubpatternTree tree) {
    throw new UnsupportedOperationException("Conditionals not supported");
  }

  private class VarNameGenerator {
    private int count = 0;

    private String getFreshName(String prefix) {
      String freshName = String.format("%s%d", prefix, count);
      count += 1;
      return freshName;
    }

    private String getFreshName() {
      return getFreshName("s");
    }

  }
}
