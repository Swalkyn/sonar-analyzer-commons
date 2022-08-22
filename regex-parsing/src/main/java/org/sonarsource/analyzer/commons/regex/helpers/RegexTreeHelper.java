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


import java.util.HashSet;
import java.util.Set;
import dk.brics.automaton.Automaton;
import dk.brics.automaton.RegExp;
import org.sonarsource.analyzer.commons.regex.ast.AutomatonState;
import org.sonarsource.analyzer.commons.regex.ast.BoundaryTree;
import org.sonarsource.analyzer.commons.regex.ast.FinalState;
import org.sonarsource.analyzer.commons.regex.ast.LookAroundTree;

import static org.sonarsource.analyzer.commons.regex.ast.AutomatonState.TransitionType.EPSILON;

public class RegexTreeHelper {

  private RegexTreeHelper() {
    // Utils class
  }

  /**
   * Translates java.util.re syntax into brics syntax
   */
  private static Automaton regexToAutomaton(String regex) {
    String processed = regex
      .replace("\\\\", "\\")
      .replace("\\d", "[0-9]");
    return new RegExp(processed).toAutomaton();
  }

  /**
   * If both sub-automata have allowPrefix set to true, this method will check whether auto1 intersects
   * the prefix of auto2 or auto2 intersects the prefix of auto1. This is different than checking whether
   * the prefix of auto1 intersects the prefix of auto2 (which would always be true because both prefix
   * always contain the empty string).
   * defaultAnswer will be returned in case of unsupported features or the state limit is exceeded.
   * It should be whichever answer does not lead to an issue being reported to avoid false positives.
   */
  public static boolean intersects(String regexA, String regexB, boolean defaultAnswer, boolean prefixA, boolean prefixB, boolean negate) {
    try {
      Automaton dfaA = regexToAutomaton(regexA);
      Automaton dfaB = regexToAutomaton(regexB);
      if (negate) {
        dfaA = dfaA.concatenate(new RegExp(".*").toAutomaton()).complement();
      }
      if (prefixA && prefixB) {
        Automaton dfaAPrefix = dfaA.clone();
        dfaAPrefix.prefixClose();
        Automaton dfaBPrefix = dfaB.clone();
        dfaBPrefix.prefixClose();
        return !(dfaA.intersection(dfaBPrefix).isEmpty() && dfaAPrefix.intersection(dfaB).isEmpty());
      }
      if (prefixA) {
        dfaA.prefixClose();
      }
      if (prefixB) {
        dfaB.prefixClose();
      }
      Automaton intersection = dfaA.intersection(dfaB);
      return !(intersection.isEmpty() || intersection.isEmptyString());
    } catch (IllegalArgumentException e) {
      return defaultAnswer;
    }
  }

  /**
   * Here auto2.allowPrefix means that if supersetOf(auto1, auto2), then for every string matched by auto2, auto1 can match a prefix of it
   * auto1.allowPrefix means that if supersetOf(auto1, auto2), then for every string matched by auto2, auto1 can match a continuation of it
   * If both are set, it means either one can be the case.
   */
  public static boolean supersetOf(String regexA, String regexB, boolean defaultAnswer, boolean prefixA, boolean prefixB) {
    // FIXME: approximation, can return incorrect results.
    try {
      Automaton dfaA = regexToAutomaton(regexA);
      Automaton dfaB = regexToAutomaton(regexB);
      if (prefixB) {
        String commonPrefix = dfaB.getCommonPrefix();
        if (!commonPrefix.isEmpty()) {
          Automaton commonPrefixes = new RegExp(commonPrefix).toAutomaton();
          commonPrefixes.prefixClose();
          return !commonPrefixes.intersection(dfaA).isEmpty();
        }
      } else if (prefixA) {
        dfaA.prefixClose();
        return dfaB.subsetOf(dfaA);
      }
      return dfaB.subsetOf(dfaA);
    } catch (IllegalArgumentException e) {
      return defaultAnswer;
    }
  }

  public static boolean isAnchoredAtEnd(AutomatonState start) {
    return isAnchoredAtEnd(start, new HashSet<>());
  }

  private static boolean isAnchoredAtEnd(AutomatonState start, Set<AutomatonState> visited) {
    if (isEndBoundary(start)) {
      return true;
    }
    if (start instanceof FinalState) {
      return false;
    }
    visited.add(start);
    for (AutomatonState successor : start.successors()) {
      if (!visited.contains(successor) && !isAnchoredAtEnd(successor, visited)) {
        return false;
      }
    }
    return true;
  }

  public static boolean isEndBoundary(AutomatonState state) {
    if (!(state instanceof BoundaryTree)) {
      return false;
    }
    switch (((BoundaryTree) state).type()) {
      case LINE_END:
      case INPUT_END:
      case INPUT_END_FINAL_TERMINATOR:
        return true;
      default:
        return false;
    }
  }

  public static boolean onlyMatchesEmptySuffix(AutomatonState start) {
    return onlyMatchesEmptySuffix(start, new HashSet<>());
  }

  private static boolean onlyMatchesEmptySuffix(AutomatonState start, Set<AutomatonState> visited) {
    if (start instanceof FinalState || visited.contains(start)) {
      return true;
    }
    visited.add(start);
    if (start instanceof LookAroundTree) {
      return onlyMatchesEmptySuffix(start.continuation(), visited);
    }
    if (start.incomingTransitionType() != EPSILON) {
      return false;
    }

    for (AutomatonState successor : start.successors()) {
      if (!onlyMatchesEmptySuffix(successor, visited)) {
        return false;
      }
    }
    return true;
  }
}
