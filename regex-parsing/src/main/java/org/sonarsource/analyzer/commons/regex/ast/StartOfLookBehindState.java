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
package org.sonarsource.analyzer.commons.regex.ast;

import javax.annotation.Nonnull;

public class StartOfLookBehindState extends ActiveFlagsState {

  private final AutomatonState content;

  public StartOfLookBehindState(AutomatonState content, FlagSet activeFlags) {
    super(activeFlags);
    this.content = content;
  }

  @Nonnull
  @Override
  public AutomatonState continuation() {
    return content;
  }

  @Nonnull
  @Override
  public TransitionType incomingTransitionType() {
    return TransitionType.LOOKAROUND_BACKTRACKING;
  }

}
