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
package org.sonarsource.analyzer.commons.regex.java;

import javax.annotation.CheckForNull;
import org.sonarsource.analyzer.commons.regex.RegexSource;
import org.sonarsource.analyzer.commons.regex.ast.IndexRange;
import org.sonarsource.analyzer.commons.regex.ast.SourceCharacter;

/**
 * Parses unicode escape sequences in Java code. Given an Unicode escape sequence, it will give you the character
 * represented by it. Given any other character it will give you that character as-is.
 */
public class JavaUnicodeEscapeParser {

  private final RegexSource source;

  private final String sourceText;
  private final int textLength;

  private int index;

  private SourceCharacter current;
  private boolean isEscaping = false;

  public JavaUnicodeEscapeParser(RegexSource source) {
    this.source = source;
    this.sourceText = source.getSourceText();
    this.textLength = sourceText.length();
    this.index = 0;
    moveNext();
  }

  public void resetTo(int index) {
    this.index = index;
    moveNext();
  }

  @CheckForNull
  public SourceCharacter getCurrent() {
    return current;
  }

  public void moveNext() {
    if (index >= textLength) {
      current = null;
      return;
    }
    int startIndex = index;
    char ch;

    boolean isBackslash = sourceText.charAt(index) == '\\';
    boolean isEscapedUnicode = isBackslash && index < (textLength - 1) && sourceText.charAt(index + 1) == 'u';

    if (isEscapedUnicode && !isEscaping) {
      index += 2;
      while (sourceText.charAt(index) == 'u') {
        index++;
      }
      StringBuilder codePoint = new StringBuilder(4);
      for (int i = 0; i < 4 && index < textLength; i++, index++) {
        codePoint.append(sourceText.charAt(index));
      }
      ch = (char) Integer.parseInt(codePoint.toString(), 16);
    } else {
      ch = sourceText.charAt(index);
      index++;
      isEscaping = isBackslash && !isEscaping;
    }
    current = new SourceCharacter(source, new IndexRange(startIndex, index), ch, isEscapedUnicode);
  }

}
