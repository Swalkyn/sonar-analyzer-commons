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

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import org.sonarsource.analyzer.commons.regex.MatchType;
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
import org.sonarsource.analyzer.commons.regex.ast.FlagSet;
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
import org.sosy_lab.java_smt.api.BooleanFormula;
import org.sosy_lab.java_smt.api.BooleanFormulaManager;
import org.sosy_lab.java_smt.api.ProverEnvironment;
import org.sosy_lab.java_smt.api.RegexFormula;
import org.sosy_lab.java_smt.api.SolverContext;
import org.sosy_lab.java_smt.api.SolverException;
import org.sosy_lab.java_smt.api.StringFormula;
import org.sosy_lab.java_smt.api.StringFormulaManager;

public class SatisfiabilityChecker implements ReturningRegexVisitor<Constraint> {

  public final SolverContext context;
  private final StringFormulaManager smgr;
  private final BooleanFormulaManager bmgr;
  private final VarNameGenerator varNameGenerator;
  private final MatchType matchType;
  private final FlagSet flags;

  public StringFormula newStringVar() {
    return smgr.makeVariable(varNameGenerator.getFreshName());
  }

  public StringConstraint convert(RegexFormula regexFormula) {
    StringFormula variable = newStringVar();
    return new SimpleStringConstraint(variable, smgr.in(variable, regexFormula));
  }

  private String getSolverChar(CharacterTree tree) {
    return (Character.isAlphabetic(tree.codePointOrUnit())) ?
      tree.characterAsString() : String.format("\\u{%x}", tree.codePointOrUnit());
  }

  public SatisfiabilityChecker(SolverContext solverContext, MatchType matchType, FlagSet flags) {
    this.context = solverContext;
    this.matchType = matchType;
    this.flags = flags;
    smgr = solverContext.getFormulaManager().getStringFormulaManager();
    bmgr = solverContext.getFormulaManager().getBooleanFormulaManager();
    varNameGenerator = new VarNameGenerator();
  }

  public SatisfiabilityChecker(SolverContext solverContext, MatchType matchType) {
    this(solverContext, matchType, new FlagSet());
  }

  public SatisfiabilityChecker(SolverContext solverContext) {
    this(solverContext, MatchType.UNKNOWN);
  }

  public boolean check(RegexParseResult parseResult, boolean defaultAnswer) {
    if (parseResult.getResult() == null) {
      return defaultAnswer;
    }
    try {
      Optional<StringConstraint> mainConstraints = visit(parseResult.getResult()).getStringConstraint();
      return mainConstraints.map(constraint -> {
        StringConstraint extended = constraint;
        if (matchType != MatchType.FULL) {
          extended = new ConstraintConcatenation(smgr, bmgr, this).of(
            new RegexConstraint(smgr.all()),
            constraint,
            new RegexConstraint(smgr.all())
          ).getStringConstraint().get();
        }
        LookaheadConstraintVisitor laVisitor = new LookaheadConstraintVisitor(smgr, bmgr, this);
        LookbehindConstraintVisitor lbVisitor = new LookbehindConstraintVisitor(smgr, bmgr, this);
        BooleanFormula lookaheadFormulas = laVisitor.getLookaheadConstraints(extended);
        BooleanFormula lookbehindFormulas = lbVisitor.getLookbehindConstraints(extended);
        BooleanFormula fullFormula = bmgr.and(extended.formula, lookaheadFormulas, lookbehindFormulas);
        try (ProverEnvironment prover = context.newProverEnvironment()) {
          prover.addConstraint(fullFormula);
          return !prover.isUnsat();
        } catch (SolverException | InterruptedException e) {
          return true;
        }
      }).orElse(true);
    } catch (UnsupportedOperationException e) {
      return defaultAnswer;
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
    return new ConstraintConcatenation(smgr, bmgr, this).of(tree);
  }

  @Override
  public Constraint visitDisjunction(DisjunctionTree tree) {
    return new ConstraintDisjunction(smgr, bmgr, this).of(tree);
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
    Constraint constraint = visit(tree.getElement());
    StringFormula dummyVariable = newStringVar();
    StringFormula lookaroundVariable = newStringVar();
    StringFormula continuationVariable = newStringVar();
    RegexConstraint regexConstraint = constraint.getRegexConstraint().orElseThrow(UnsupportedOperationException::new);
    BooleanFormula formula;
    if (tree.getDirection() == LookAroundTree.Direction.AHEAD && tree.getPolarity() == LookAroundTree.Polarity.POSITIVE) {
      formula = bmgr.and(
        smgr.in(lookaroundVariable, regexConstraint.formula),
        smgr.prefix(lookaroundVariable, continuationVariable));
    } else if (tree.getDirection() == LookAroundTree.Direction.AHEAD && tree.getPolarity() == LookAroundTree.Polarity.NEGATIVE) {
      formula = context.getFormulaManager().getQuantifiedFormulaManager().forall(lookaroundVariable,
        bmgr.implication(smgr.in(lookaroundVariable, regexConstraint.formula), bmgr.not(smgr.prefix(lookaroundVariable, continuationVariable))));
    }
    else if (tree.getDirection() == LookAroundTree.Direction.BEHIND && tree.getPolarity() == LookAroundTree.Polarity.POSITIVE) {
      formula = bmgr.and(
        smgr.in(lookaroundVariable, regexConstraint.formula),
        smgr.suffix(lookaroundVariable, continuationVariable));
    }
    else {
      formula = context.getFormulaManager().getQuantifiedFormulaManager().forall(lookaroundVariable,
        bmgr.implication(smgr.in(lookaroundVariable, regexConstraint.formula), bmgr.not(smgr.suffix(lookaroundVariable, continuationVariable))));
    }
    return new LookaroundConstraint(
      dummyVariable, bmgr.and(smgr.in(dummyVariable, smgr.makeRegex("")), formula),
      continuationVariable, tree.getDirection());
  }

  @Override
  public Constraint visitBoundary(BoundaryTree tree) {
    return new RegexConstraint(smgr.makeRegex(""));
  }

  @Override
  public Constraint visitMiscEscapeSequence(MiscEscapeSequenceTree tree) {
    throw new UnsupportedOperationException("Misc. escape trees are not supported");
  }

  @Override
  public Constraint visitRepetition(RepetitionTree tree) {
    Quantifier quantifier = tree.getQuantifier();
    if (quantifier.getMinimumRepetitions() == 0 && quantifier.getMaximumRepetitions() == null && tree.getElement() instanceof DotTree) {
      return new RegexConstraint(smgr.all());
    } else {
      Constraint constraint = visit(tree.getElement());
      RegexFormula elementFormula = constraint.getRegexConstraint()
        .orElseThrow(UnsupportedOperationException::new).formula;
      int min = quantifier.getMinimumRepetitions();
      Optional<Integer> optMax = Optional.ofNullable(quantifier.getMaximumRepetitions());
      RegexFormula repetitionFormula =  optMax.map(max -> (min == 0 && max == 1) ?
          smgr.optional(elementFormula) :
          smgr.concat(smgr.times(elementFormula, min), smgr.concatRegex(Collections.nCopies(max - min, smgr.optional(elementFormula))))
      ).orElseGet(() -> {
        if (min == 0) {
          return smgr.closure(elementFormula);
        } else if (min == 1) {
          return smgr.cross(elementFormula);
        } else {
          return smgr.concat(smgr.times(elementFormula, min), smgr.closure(elementFormula));
        }
      });
      if (quantifier.getModifier() == Quantifier.Modifier.POSSESSIVE) {
        StringFormula repetitionVariable = newStringVar();
        StringFormula lookaroundVariable = newStringVar();
        StringFormula continuationVariable = newStringVar();
        BooleanFormula formula = context.getFormulaManager().getQuantifiedFormulaManager().forall(lookaroundVariable,
          bmgr.implication(smgr.in(lookaroundVariable, elementFormula), bmgr.not(smgr.prefix(lookaroundVariable, continuationVariable))));
        return new LookaroundConstraint(
          repetitionVariable, bmgr.and(smgr.in(repetitionVariable, repetitionFormula), formula),
          continuationVariable, LookAroundTree.Direction.AHEAD);
      }
      return new RegexConstraint(repetitionFormula);
    }
  }

  @Override
  public RegexConstraint visitCharacterClass(CharacterClassTree tree) {
    RegexConstraint constraint = tree.getContents().accept(this).getRegexConstraint()
        .orElseThrow(UnsupportedOperationException::new);
    return tree.isNegated() ?
      constraint.map(formula -> smgr.difference(smgr.allChar(), formula)) : constraint;
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
      .reduce(smgr.none(), smgr::union);
    return new RegexConstraint(unionFormula);
  }

  @Override
  public RegexConstraint visitCharacterClassIntersection(CharacterClassIntersectionTree tree) {
    RegexFormula intersectionFormula = tree.getCharacterClasses().stream()
      .map(charClass -> charClass.accept(this).getRegexConstraint().orElseThrow(UnsupportedOperationException::new).formula)
      .reduce(smgr.all(), smgr::intersection);
    return new RegexConstraint(intersectionFormula);
  }

  @Override
  public RegexConstraint visitDot(DotTree tree) {
    return new RegexConstraint(smgr.allChar());
  }

  @Override
  public RegexConstraint visitEscapedCharacterClass(EscapedCharacterClassTree tree) {
    RegexFormula regexFormula;
    switch (tree.getType()) {
      case 'd':
      case 'D':
        regexFormula = smgr.range('0', '9');
        break;
      case 'w':
      case 'W':
        regexFormula = smgr.complement(smgr.union(smgr.range('a', 'z'),
          smgr.union(smgr.range('A', 'Z'),
            smgr.union(smgr.range('0', '9'),
              smgr.makeRegex("_")))));
        break;
      case 's':
      case 'S':
        regexFormula = smgr.union(smgr.range('\t', '\r'),
          smgr.union(smgr.makeRegex(" "),
            smgr.union(smgr.makeRegex("\\u0085"),
              smgr.union(smgr.makeRegex("\\u00A0"),
                smgr.union(smgr.makeRegex("\\u1680"),
                  smgr.union(smgr.range((char) 0x2000, (char) 0x200A),
                    smgr.union(smgr.range((char) 0x2028, (char) 0x2029),
                      smgr.union(smgr.makeRegex("\\u202F"),
                        smgr.union(smgr.makeRegex("\\u205F"), smgr.makeRegex("\\u3000"))))))))));
        break;
      default:
        throw new UnsupportedOperationException("Unsupported escape sequence");
    }
    if (tree.isNegation()) {
      regexFormula = smgr.complement(regexFormula);
    }
    return new RegexConstraint(regexFormula);
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
