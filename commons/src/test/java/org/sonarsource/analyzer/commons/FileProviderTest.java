/*
 * SonarSource Analyzers Commons
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
package org.sonarsource.analyzer.commons;

import java.io.File;
import java.io.FileFilter;
import java.util.List;

import org.apache.commons.io.filefilter.IOFileFilter;
import org.junit.Test;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class FileProviderTest {

  private static final File baseDir = new File("src/test/resources/org/sonarsource/analyzer/commons/scanner").getAbsoluteFile();
  private static final File file = new File(baseDir, "dir/f1.txt");

  @Test
  public void noMatchedFile() {
    assertThat(getMatchingFiles("dir/xxx")).isEmpty();
  }

  @Test
  public void simpleFile() {
    assertThat(getMatchingFiles("dir/f1.txt")).containsOnly(file);
  }

  @Test
  public void wildCard() {
    assertThat(getMatchingFiles("*/f1.txt")).containsOnly(file);
    assertThat(getMatchingFiles("**/f1.txt")).containsOnly(file, new File(baseDir, "dir/subdir/f1.txt"));
  }

  @Test
  public void shouldNotFailWhenChildPathIsUnexpectedlyShorterThanBaseDirPath() {
    File dir = mock(File.class);
    final File matchingFile = new File("/matching/file");
    when(dir.getAbsolutePath()).thenReturn("/a/somewhat/long/path");
    when(dir.isDirectory()).thenReturn(true);
    when(dir.listFiles(any(FileFilter.class))).thenAnswer(new Answer<File[]>() {
      @Override
      public File[] answer(InvocationOnMock invocation) throws Throwable {
        IOFileFilter filter = (IOFileFilter) invocation.getArguments()[0];
        filter.accept(new File("/short"), "path");
        return new File[] {matchingFile};
      }
    });
    assertThat(scan("xxx", dir)).containsOnly(matchingFile);
  }

  private List<File> getMatchingFiles(String pattern) {
    return scan(pattern, baseDir);
  }

  private static List<File> scan(String pattern, File dir) {
    FileProvider scanner = new FileProvider(dir, pattern);
    return scanner.getMatchingFiles();
  }

}
