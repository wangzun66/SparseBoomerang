/**
 * ***************************************************************************** Copyright (c) 2018
 * Fraunhofer IEM, Paderborn, Germany. This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0 which is available at
 * http://www.eclipse.org/legal/epl-2.0.
 *
 * <p>SPDX-License-Identifier: EPL-2.0
 *
 * <p>Contributors: Johannes Spaeth - initial API and implementation
 * *****************************************************************************
 */
package boomerang;

import boomerang.scene.ControlFlowGraph;
import boomerang.scene.Val;

public class ForwardQuery extends Query {

  public ForwardQuery(ControlFlowGraph.Edge edge, Val variable) {
    super(edge, variable);
  }

  @Override
  public String toString() {
    return "ForwardQuery: " + super.toString();
  }

  @Override
  public String getInfo() {
    StringBuilder builder = new StringBuilder();
    builder.append("ForwardQuery: (Var: ");
    builder.append(var().getVariableName());
    builder.append(", Stmt: ");
    builder.append(cfgEdge().getTarget().toString());
    builder.append(", Method: ");
    builder.append(cfgEdge().getMethod().toString());
    builder.append(")");
    return builder.toString();
  }
}
