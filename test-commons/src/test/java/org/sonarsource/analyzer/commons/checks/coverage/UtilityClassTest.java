/*
 * SonarSource Analyzers Test Commons
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
package org.sonarsource.analyzer.commons.checks.coverage;

import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

public class UtilityClassTest {

  @Test
  public void self_check() throws Exception {
    UtilityClass.assertGoodPractice(UtilityClass.class);
  }

  @Test
  public void error_if_not_final() throws Exception {
    class Utils { }

    assertThatThrownBy(() -> UtilityClass.assertGoodPractice(Utils.class))
      .isInstanceOf(IllegalStateException.class)
      .hasMessage("Utility class Utils should be 'final'");
  }

  @Test
  public void error_if_several_constructors() throws Exception {
    final class Utils { private Utils() {} private Utils(int a) {} }

    assertThatThrownBy(() -> UtilityClass.assertGoodPractice(Utils.class))
      .isInstanceOf(IllegalStateException.class)
      .hasMessage("Utility class Utils should only have one constructor.");
  }

  @Test
  public void error_if_constructor_not_private() throws Exception {
    final class Utils { public Utils() {} }

    assertThatThrownBy(() -> UtilityClass.assertGoodPractice(Utils.class))
      .isInstanceOf(IllegalStateException.class)
      .hasMessage("Utils constructor should be private.");
  }

}
