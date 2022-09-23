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

import java.util.List;
import java.util.ListIterator;
import java.util.Optional;
import java.util.stream.Collectors;
import org.sonarsource.analyzer.commons.regex.RegexParseResult;
import org.sonarsource.analyzer.commons.regex.ast.AtomicGroupTree;
import org.sonarsource.analyzer.commons.regex.ast.BackReferenceTree;
import org.sonarsource.analyzer.commons.regex.ast.BoundaryTree;
import org.sonarsource.analyzer.commons.regex.ast.CapturingGroupTree;
import org.sonarsource.analyzer.commons.regex.ast.CharacterClassTree;
import org.sonarsource.analyzer.commons.regex.ast.CharacterClassUnionTree;
import org.sonarsource.analyzer.commons.regex.ast.CharacterRangeTree;
import org.sonarsource.analyzer.commons.regex.ast.CharacterTree;
import org.sonarsource.analyzer.commons.regex.ast.ConditionalSubpatternTree;
import org.sonarsource.analyzer.commons.regex.ast.DisjunctionTree;
import org.sonarsource.analyzer.commons.regex.ast.DotTree;
import org.sonarsource.analyzer.commons.regex.ast.LookAroundTree;
import org.sonarsource.analyzer.commons.regex.ast.NonCapturingGroupTree;
import org.sonarsource.analyzer.commons.regex.ast.Quantifier;
import org.sonarsource.analyzer.commons.regex.ast.RegexTree;
import org.sonarsource.analyzer.commons.regex.ast.RepetitionTree;
import org.sonarsource.analyzer.commons.regex.ast.SequenceTree;
import org.sosy_lab.java_smt.api.BooleanFormula;
import org.sosy_lab.java_smt.api.BooleanFormulaManager;
import org.sosy_lab.java_smt.api.Formula;
import org.sosy_lab.java_smt.api.ProverEnvironment;
import org.sosy_lab.java_smt.api.RegexFormula;
import org.sosy_lab.java_smt.api.SolverContext;
import org.sosy_lab.java_smt.api.SolverException;
import org.sosy_lab.java_smt.api.StringFormula;
import org.sosy_lab.java_smt.api.StringFormulaManager;

public class SatisfiabilityChecker extends BranchTrackingVisitor {
  private interface Constraint {
    boolean isStringConstraint();
    default boolean isRegexConstraint() {
      return !isStringConstraint();
    }
    StringConstraint asStringConstraint();
  }

  private class StringConstraint implements Constraint {
    private final StringFormula stringVar;
    private final BooleanFormula constraint;

    public StringConstraint(StringFormula stringVar, BooleanFormula constraint) {
      this.stringVar = stringVar;
      this.constraint = constraint;
    }

    @Override
    public boolean isStringConstraint() {
      return true;
    }

    @Override
    public StringConstraint asStringConstraint() {
      return this;
    }
  }

  private class RegexConstraint implements Constraint {
    private final RegexFormula constraint;

    public RegexConstraint(RegexFormula constraint) {
      this.constraint = constraint;
    }

    public RegexFormula getConstraint() {
      return constraint;
    }

    @Override
    public boolean isStringConstraint() {
      return false;
    }

    @Override
    public StringConstraint asStringConstraint() {
      StringFormula var = newStringVar();
      return new StringConstraint(var, smgr.in(var, constraint));
    }
  }

  private class ConstraintUnion {
    public Constraint of(List<RegexConstraint> regexConstraints) {
      return new RegexConstraint(regexConstraints.stream().map(RegexConstraint::getConstraint).reduce(smgr.makeRegex(""), smgr::union));
    }
  }

  private final SolverContext context;
  private final StringFormulaManager smgr;
  private final BooleanFormulaManager bmgr;
  private Constraint returnFormula;
  private Optional<String> returnCharacter = Optional.empty();
  private StringFormula suffix;
  private StringFormula current;

  private StringFormula newStringVar() {
    return smgr.makeVariable(VarNameGenerator.getFreshName());
  }

  private void returnFormula(Formula formula) {
    returnFormula = formula;
    returnCharacter = Optional.empty();
  }

  private void returnCharacter(String character) {
    returnFormula = smgr.makeRegex(character);
    returnCharacter = Optional.of(character);
  }

  private String getSolverChar(CharacterTree tree) {
    return (Character.isAlphabetic(tree.codePointOrUnit())) ?
      tree.characterAsString() : String.format("\\u{%d}", tree.codePointOrUnit());
  }

  private BooleanFormula split(RegexFormula currentConstraint, RegexFormula suffixConstraint) {
    BooleanFormula lookAroundConstraints = bmgr.and(smgr.in(current, currentConstraint), smgr.in(suffix, suffixConstraint));
    suffix = smgr.concat(current, suffix);
    current = smgr.makeString(VarNameGenerator.getFreshName());
    return lookAroundConstraints;
  }

  public SatisfiabilityChecker(SolverContext solverContext) {
    context = solverContext;
    smgr = solverContext.getFormulaManager().getStringFormulaManager();
    bmgr = solverContext.getFormulaManager().getBooleanFormulaManager();
  }

  public boolean check(RegexParseResult parseResult, boolean defaultAnswer) {
    if (parseResult.getResult() == null) {
      return defaultAnswer;
    }
    super.visit(parseResult.getResult());
    StringFormula s = smgr.makeVariable("S");
    try (ProverEnvironment prover = context.newProverEnvironment()) {
      prover.addConstraint(smgr.in(s, returnFormula));
      return !prover.isUnsat();
    } catch (SolverException | InterruptedException e) {
      return true;
    }
  }

  @Override
  public void visitBackReference(BackReferenceTree tree) {
    throw new UnsupportedOperationException("Backreferences are not supported");
  }

  @Override
  public void visitCharacter(CharacterTree tree) {
    returnCharacter(getSolverChar(tree));
  }

  @Override
  public void visitSequence(SequenceTree tree) {
    StringBuilder sb = new StringBuilder();
    parentFormula = smgr.makeRegex("");

    ListIterator<RegexTree> iterator = tree.getItems().listIterator(tree.getItems().size());
    // Traversing sequences backwards is needed to create suffix strings for lookaheads
    while (iterator.hasPrevious()) {
      super.visit(iterator.previous());
      if (returnCharacter.isPresent()) {
        // Combine groups of characters
        while (returnCharacter.isPresent()) {
          sb.append(returnCharacter.get());
          if (iterator.hasPrevious()) {
            super.visit(iterator.previous());
          } else {
            break;
          }
        }
        if (sb.length() != 0) {
          parentFormula = smgr.concat(smgr.makeRegex(sb.reverse().toString()), parentFormula);
          sb.setLength(0);
        }
      } else {
        parentFormula = smgr.concat(returnFormula, parentFormula);
      }
    }
    returnFormula(parentFormula);
  }

  @Override
  public void visitDisjunction(DisjunctionTree tree) {
    List<Constraint> subFormulas = tree.getAlternatives().stream().map(alternative -> {
      super.visit(alternative);
      return returnFormula;
    }).collect(Collectors.toList());
    if (subFormulas.stream().allMatch(Constraint::isRegexConstraint)) {
      RegexFormula union = subFormulas.stream()
        .map(Constraint::getRegexFormula)
        .reduce(smgr.makeRegex(""), smgr::union);
      returnFormula(new Constraint(union));
    } else {

      subFormulas.stream().map(Constraint::asBooleanFormula).
    }
  }

  @Override
  public void visitCapturingGroup(CapturingGroupTree tree) {
    super.visit(tree.getElement());
    returnFormula(returnFormula);
  }

  @Override
  public void visitNonCapturingGroup(NonCapturingGroupTree tree) {
    if (tree.getElement() != null) {
      super.visit(tree.getElement());
      returnFormula(returnFormula);
    }
  }

  @Override
  public void visitAtomicGroup(AtomicGroupTree tree) {
    super.visit(tree.getElement());
    returnFormula(returnFormula);
  }

  @Override
  public void visitLookAround(LookAroundTree tree) {
    if (tree.getDirection() == LookAroundTree.Direction.BEHIND) {
      // Ignore lookbehinds
      returnFormula(smgr.makeRegex(""));
    } else {
      // This does not work for nested lookahead
      super.visit(tree.getElement());
      returnFormula(split(parentFormula, returnFormula));
    }
    // Ignore lookarounds for now
  }

  @Override
  public void visitBoundary(BoundaryTree tree) {
    returnFormula(smgr.makeRegex(""));
  }

  @Override
  public void visitRepetition(RepetitionTree tree) {
    Quantifier quantifier = tree.getQuantifier();
    if (quantifier.getMinimumRepetitions() == 0 && quantifier.getMaximumRepetitions() == null && tree.getElement() instanceof DotTree) {
      returnFormula(smgr.all());
    } else {
      super.visit(tree.getElement());
      RegexFormula repetition = smgr.cross(returnFormula);
      if (quantifier.getMinimumRepetitions() > 0) {
        repetition = smgr.concat(smgr.times(returnFormula, quantifier.getMinimumRepetitions()), repetition);
      }
      returnFormula(repetition);
    }
  }

  @Override
  public void visitCharacterClass(CharacterClassTree tree) {
    tree.getContents().accept(this);
  }

  @Override
  public void visitCharacterRange(CharacterRangeTree tree) {
    RegexFormula characterRange = smgr.range(
      smgr.makeString(getSolverChar(tree.getLowerBound())),
      smgr.makeString(getSolverChar(tree.getUpperBound())));
    returnFormula(characterRange);
  }

  @Override
  public void visitCharacterClassUnion(CharacterClassUnionTree tree) {
    RegexFormula union = tree.getCharacterClasses().stream().map(charClass -> {
      charClass.accept(this);
      return returnFormula;
    }).reduce(smgr.makeRegex(""), smgr::union);
    returnFormula(union);
  }

  @Override
  public void visitDot(DotTree tree) {
    returnFormula(smgr.allChar());
  }

  @Override
  public void visitConditionalSubpattern(ConditionalSubpatternTree tree) {
    throw new UnsupportedOperationException("Conditionals not supported");
  }

  private static class VarNameGenerator {
    private static int count = 0;

    private static String getFreshName() {
      String freshName = String.format("v%d", count);
      count += 1;
      return freshName;
    }
  }
}
