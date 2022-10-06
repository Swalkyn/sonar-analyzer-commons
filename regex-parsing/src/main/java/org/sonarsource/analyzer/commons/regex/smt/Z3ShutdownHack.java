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

import org.sosy_lab.common.ShutdownNotifier.ShutdownRequestListener;
import org.sosy_lab.java_smt.SolverContextFactory;
import org.sosy_lab.java_smt.api.SolverContext;

import java.lang.reflect.Field;

public class Z3ShutdownHack {
    public static void shutdownHack(SolverContext context, String reason) {
        if (context.getSolverName() != SolverContextFactory.Solvers.Z3) {
            throw new RuntimeException("This hack is intended for Z3-Contexts only!");
        }

        try {
            Class z3SolverContextClass = context.getClass().getClassLoader().loadClass("org.sosy_lab.java_smt.solvers.z3.Z3SolverContext");
            Field interruptListenerField = z3SolverContextClass.getDeclaredField("interruptListener");
            interruptListenerField.setAccessible(true);
            ShutdownRequestListener interruptListener = (ShutdownRequestListener) interruptListenerField.get(context);
            interruptListener.shutdownRequested(reason);
        } catch (NoSuchFieldException | IllegalAccessException | ClassNotFoundException e) {
            System.out.println("Hacking the Z3SmtContext failed!");
            e.printStackTrace();
        }
    }
}
