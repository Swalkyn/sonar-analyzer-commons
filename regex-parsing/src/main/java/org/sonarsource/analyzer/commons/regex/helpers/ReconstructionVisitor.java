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

import org.sonarsource.analyzer.commons.regex.ast.*;

import java.util.Iterator;

public class ReconstructionVisitor extends RegexBaseVisitor {

  private final StringBuilder sb = new StringBuilder();
  private final IndexRange excludeRange;

  public ReconstructionVisitor(IndexRange excludeRange) {
    this.excludeRange = excludeRange;
  }

  public String result() {
    return sb.toString();
  }

  private void visitLeaf(RegexTree tree) {
    if (!tree.getRange().equals(excludeRange)) {
      sb.append(tree.getText());
    }
  }

  @Override
  public void visit(RegexTree tree) {
    if (tree.getRange().higherThan(excludeRange)) {
      sb.append(tree.getText());
    } else if (!tree.getRange().equals(excludeRange)) {
      super.visit(tree);
    }
  }

  @Override
  public void visitBackReference(BackReferenceTree tree) {
    visitLeaf(tree);
  }

  @Override
  public void visitCharacter(CharacterTree tree) {
    visitLeaf(tree);
  }

  @Override
  public void visitSequence(SequenceTree tree) {
    Iterator<RegexTree> iterator = tree
      .getItems()
      .stream()
      .filter(subTree -> !subTree.getRange().lowerThan(excludeRange))
      .iterator();
    visit(iterator.next());
    iterator.forEachRemaining(regexTree -> sb.append(regexTree.getText()));
  }

  @Override
  public void visitDisjunction(DisjunctionTree tree) {
    tree.getAlternatives().stream()
      .filter(subTree -> subTree.getRange().contains(excludeRange))
      .findFirst()
      .ifPresent(this::visit);
  }

  @Override
  public void visitGroup(GroupTree tree) {
    RegexTree element = tree.getElement();
    if (element != null && !tree.getRange().equals(excludeRange)) {
      int currentLength = sb.length();
      visit(element);
      if (sb.length() != currentLength) {
        sb.insert(currentLength, tree.getGroupHeader().getText());
        sb.append(")");
      }
    }
  }

  @Override
  public void visitRepetition(RepetitionTree tree) {
    int currentLength = sb.length();
    visit(tree.getElement());
    if (sb.length() != currentLength) {
      sb.append("*");
    }
  }

  @Override
  public void visitCharacterClass(CharacterClassTree tree) {
    visitLeaf(tree);
  }

//  @Override
//  public void visitCharacterRange(CharacterRangeTree tree) {
//  }
//
//  @Override
//  public void visitCharacterClassUnion(CharacterClassUnionTree tree) {
//
//  }
//
//  @Override
//  public void visitCharacterClassIntersection(CharacterClassIntersectionTree tree) {
//
//  }

  @Override
  public void visitDot(DotTree tree) {
    visitLeaf(tree);
  }

//  @Override
//  public void visitEscapedCharacterClass(EscapedCharacterClassTree tree) {
//
//  }
//
//  @Override
//  public void visitBoundary(BoundaryTree boundaryTree) {
//  }

  @Override
  public void visitMiscEscapeSequence(MiscEscapeSequenceTree tree) {
    visitLeaf(tree);
  }

  @Override
  public void visitConditionalSubpattern(ConditionalSubpatternTree tree) {
    visitLeaf(tree);
  }
}
