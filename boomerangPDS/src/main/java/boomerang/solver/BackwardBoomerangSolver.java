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
package boomerang.solver;

import boomerang.BackwardQuery;
import boomerang.BoomerangOptions;
import boomerang.callgraph.CalleeListener;
import boomerang.callgraph.ObservableICFG;
import boomerang.controlflowgraph.ObservableControlFlowGraph;
import boomerang.controlflowgraph.PredecessorListener;
import boomerang.controlflowgraph.SuccessorListener;
import boomerang.datacollection.DataCollection;
import boomerang.datacollection.DecisionLog;
import boomerang.datacollection.MethodLog;
import boomerang.datacollection.QueryLog;
import boomerang.flowfunction.IBackwardFlowFunction;
import boomerang.scene.AllocVal;
import boomerang.scene.ControlFlowGraph;
import boomerang.scene.ControlFlowGraph.Edge;
import boomerang.scene.DataFlowScope;
import boomerang.scene.Field;
import boomerang.scene.InvokeExpr;
import boomerang.scene.Method;
import boomerang.scene.Statement;
import boomerang.scene.Type;
import boomerang.scene.Val;
import boomerang.scene.sparse.*;
import boomerang.scene.sparse.eval.PropagationCounter;
import boomerang.strategydecider.FeatureExtractor;
import com.google.common.collect.Multimap;
import com.google.common.collect.Sets;
import java.util.AbstractMap.SimpleEntry;
import java.util.Collection;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.stream.Collectors;
import org.jpmml.evaluator.Evaluator;
import org.jpmml.evaluator.EvaluatorUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import soot.SootMethod;
import soot.Unit;
import soot.jimple.Stmt;
import sync.pds.solver.nodes.GeneratedState;
import sync.pds.solver.nodes.INode;
import sync.pds.solver.nodes.Node;
import sync.pds.solver.nodes.PopNode;
import sync.pds.solver.nodes.PushNode;
import sync.pds.solver.nodes.SingleNode;
import wpds.impl.NestedWeightedPAutomatons;
import wpds.impl.Transition;
import wpds.impl.Weight;
import wpds.interfaces.Location;
import wpds.interfaces.State;

public abstract class BackwardBoomerangSolver<W extends Weight> extends AbstractBoomerangSolver<W> {
  private static final Logger LOGGER = LoggerFactory.getLogger(BackwardBoomerangSolver.class);
  private final BackwardQuery query;
  private final IBackwardFlowFunction flowFunction;
  private SparseCFG currentSCFG = null;
  private String currMethodSig = "";
  QueryLog queryLog;

  public BackwardBoomerangSolver(
      ObservableICFG<Statement, Method> icfg,
      ObservableControlFlowGraph cfg,
      Map<
              Entry<INode<Node<ControlFlowGraph.Edge, Val>>, Field>,
              INode<Node<ControlFlowGraph.Edge, Val>>>
          genField,
      BackwardQuery query,
      BoomerangOptions options,
      NestedWeightedPAutomatons<ControlFlowGraph.Edge, INode<Val>, W> callSummaries,
      NestedWeightedPAutomatons<Field, INode<Node<ControlFlowGraph.Edge, Val>>, W> fieldSummaries,
      DataFlowScope scope,
      IBackwardFlowFunction backwardFlowFunction,
      Multimap<Field, Statement> fieldLoadStatements,
      Multimap<Field, Statement> fieldStoreStatements,
      Type propagationType) {
    super(icfg, cfg, genField, options, callSummaries, fieldSummaries, scope, propagationType);
    this.query = query;
    this.flowFunction = backwardFlowFunction;
    this.flowFunction.setSolver(this, fieldLoadStatements, fieldStoreStatements);
    SCFGSolverCache.getInstance().reset();
    queryLog = DataCollection.getInstance().getQueryLog(query.getId());
  }

  private boolean notUsedInMethod(Method m, Statement curr, Val value) {
    if (value.isStatic()) return false;
    if (!m.getLocals().stream()
        .filter(e -> e.toString().equals(value.toString()))
        .findAny()
        .isPresent()) return true;
    return false;
  }

  public INode<Node<ControlFlowGraph.Edge, Val>> generateFieldState(
      final INode<Node<ControlFlowGraph.Edge, Val>> d, final Field loc) {
    Entry<INode<Node<Edge, Val>>, Field> e = new SimpleEntry<>(d, loc);
    if (!generatedFieldState.containsKey(e)) {
      generatedFieldState.put(
          e, new GeneratedState<>(new SingleNode<>(new Node<>(epsilonStmt(), Val.zero())), loc));
    }
    return generatedFieldState.get(e);
  }

  /*
  @Override
  public INode<Val> generateCallState(INode<Val> d, Statement loc) {
    Entry<INode<Val>, Statement> e = new AbstractMap.SimpleEntry<>(d, loc);
    if (!generatedCallState.containsKey(e)) {
      generatedCallState.put(
          e, new GeneratedState<Val, Statement>(new SingleNode<Val>(Val.zero()), loc));
    }
    return generatedCallState.get(e);
  }
  */

  @Override
  protected Collection<? extends State> computeReturnFlow(
      Method method, Statement callerReturnStatement, Val value) {
    return flowFunction.returnFlow(method, callerReturnStatement, value).stream()
        .map(x -> new PopNode<>(x, PDSSystem.CALLS))
        .collect(Collectors.toSet());
  }

  protected void callFlow(Method caller, Node<Edge, Val> curr, Statement callSite) {
    icfg.addCalleeListener(new CallSiteCalleeListener(curr, caller));
    InvokeExpr invokeExpr = callSite.getInvokeExpr();
    if (dataFlowScope.isExcluded(invokeExpr.getMethod())) {
      byPassFlowAtCallsite(caller, curr);
    }
  }

  private void byPassFlowAtCallsite(Method caller, Node<Edge, Val> curr) {
    for (Statement returnSite :
        curr.stmt()
            .getStart()
            .getMethod()
            .getControlFlowGraph()
            .getPredsOf(curr.stmt().getStart())) {

      Set<State> res =
          flowFunction.callToReturnFlow(new Edge(returnSite, curr.stmt().getStart()), curr.fact())
              .stream()
              .collect(Collectors.toSet());
      for (State s : res) {
        propagate(curr, s);
      }
    }
  }

  @Override
  public void computeSuccessor(Node<Edge, Val> node) {
    LOGGER.trace("BW: Computing successor of {} for {}", node, this);
    Edge edge = node.stmt();
    Val value = node.fact();
    assert !(value instanceof AllocVal);
    Method method = edge.getStart().getMethod();
    if (method == null) return;
    if (dataFlowScope.isExcluded(method)) return;
    if (notUsedInMethod(method, edge.getStart(), value)) {
      return;
    }
    // collect data
    SootMethod sootMethod = SootAdapter.asSootMethod(method);
    if (!sootMethod.getSignature().equals(queryLog.getCurrentMethodSig())) {
      MethodLog methodLog = queryLog.getCurrentMethodLog();
      methodLog.logEnd();
      LOGGER.info(methodLog.toString());
      queryLog.setCurrentMethodSig(sootMethod.getSignature());
      MethodLog ml = new MethodLog(sootMethod);
      queryLog.setCurrentMethodLog(ml);
      queryLog.addMethodLog(ml);
      LOGGER.info(ml.toString());
      ml.logStart();
    }
    if (edge.getStart().containsInvokeExpr()
        && edge.getStart().uses(value)
        && INTERPROCEDURAL
        && checkSpecialInvoke(edge)) {
      callFlow(method, node, edge.getStart());
    } else if (icfg.isExitStmt(edge.getStart())) {
      returnFlow(method, node);
    } else {
      normalFlow(method, node);
    }
  }

  private boolean checkSpecialInvoke(Edge edge) {
    if (!options.handleSpecialInvokeAsNormalPropagation()) {
      return true;
    } else {
      return !edge.getStart().getInvokeExpr().isSpecialInvokeExpr();
    }
  }

  protected void normalFlow(Method method, Node<ControlFlowGraph.Edge, Val> currNode) {
    Edge curr = currNode.stmt();
    Val value = currNode.fact();
    if (options.getSparsificationStrategy() == SparseCFGCache.SparsificationStrategy.DYNAMIC) {
      propagateDynamic(method, currNode, curr, value);
    } else if (options.getSparsificationStrategy() != SparseCFGCache.SparsificationStrategy.NONE) {
      propagateSparse(method, currNode, curr, value);
    } else {
      for (Statement pred :
          curr.getStart().getMethod().getControlFlowGraph().getPredsOf(curr.getStart())) {
        Collection<State> flow = computeNormalFlow(method, new Edge(pred, curr.getStart()), value);
        for (State s : flow) {
          PropagationCounter.getInstance(options.getSparsificationStrategy()).countBackward();
          propagate(currNode, s);
        }
      }
    }
  }

  private void propagateDynamic(Method method, Node<Edge, Val> currNode, Edge curr, Val value) {
    Statement propStmt = curr.getStart();
    Stmt stmt = SootAdapter.asStmt(propStmt);
    SootMethod sootMethod = SootAdapter.asSootMethod(method);
    String methodSig = sootMethod.getSignature();
    if (!methodSig.equals(currMethodSig)) {
      this.currMethodSig = methodSig;
      SparseCFG sparseCFG = SCFGSolverCache.getInstance().get(methodSig);
      if (sparseCFG == null) {
        DecisionLog dl = new DecisionLog(methodSig, 0);
        queryLog.addDecisionLog(dl);
        Map<String, Float> features =
            FeatureExtractor.getInstance()
                .extract(sootMethod, SootAdapter.asValue(query.var()), stmt);
        dl.logStart();
        Evaluator evaluator = options.getEvaluator();
        Map<String, ?> results = EvaluatorUtil.decodeAll(evaluator.evaluate(features));
        int y = Integer.valueOf(results.get("y").toString());
        dl.logEnd();
        if (y == 0) {
          sparseCFG = new EmptySparseCFG(methodSig);
        } else if (y == 1) {
          sparseCFG =
              getSparseCFG(
                  query, method, value, propStmt, SparseCFGCache.SparsificationStrategy.TYPE_BASED);
          dl.setDecision(1);
        } else if (y == 2) {
          sparseCFG =
              getSparseCFG(
                  query,
                  method,
                  value,
                  propStmt,
                  SparseCFGCache.SparsificationStrategy.ALIAS_AWARE);
          dl.setDecision(2);
        } else {
          throw new RuntimeException("Evaluator evaluates wrong class!!");
        }
        LOGGER.info(dl.toString());
        SCFGSolverCache.getInstance().put(methodSig, sparseCFG);
      }
      currentSCFG = sparseCFG;
    }
    if (currentSCFG instanceof SparseAliasingCFG) {
      if (((SparseAliasingCFG) currentSCFG).getGraph().nodes().contains(stmt)) {
        propagateOnSCFG((SparseAliasingCFG) currentSCFG, method, currNode, propStmt, value);
      } else {
        propagateOnCFG(method, currNode, propStmt, value);
      }
    } else {
      propagateOnCFG(method, currNode, propStmt, value);
    }
  }

  private void propagateSparse(Method method, Node<Edge, Val> currNode, Edge curr, Val value) {
    Statement propStmt = curr.getStart();
    Stmt stmt = SootAdapter.asStmt(propStmt);
    String methodSig = SootAdapter.asSootMethod(method).getSignature();
    // todo: insert Strategy Decider
    /**
     * take sparseCFG for the method in local, if there's no corresponding scfg take scfg for the
     * method from SFCGSolverCache , if there's no corresponding scfg in solver-cache take or create
     * scfg for the method from global cache for whole apk and update in solver-cache don't forget
     * update scfg and methodsig in local
     */
    if (!methodSig.equals(currMethodSig)) {
      this.currMethodSig = methodSig;
      SparseCFG sparseCFG = SCFGSolverCache.getInstance().get(methodSig);
      if (sparseCFG == null) {
        sparseCFG = getSparseCFG(query, method, value, propStmt);
        SCFGSolverCache.getInstance().put(methodSig, sparseCFG);
      }
      currentSCFG = sparseCFG;
    }
    if (((SparseAliasingCFG) currentSCFG).getGraph().nodes().contains(stmt)) {
      propagateOnSCFG((SparseAliasingCFG) currentSCFG, method, currNode, propStmt, value);
    } else {
      propagateOnCFG(method, currNode, propStmt, value);
    }
  }

  private void propagateOnSCFG(
      SparseAliasingCFG sparseCFG,
      Method method,
      Node<Edge, Val> currNode,
      Statement propStmt,
      Val value) {
    Stmt stmt = SootAdapter.asStmt(propStmt);
    Set<Unit> predecessors = sparseCFG.getGraph().predecessors(stmt);
    for (Unit pred : predecessors) {
      Collection<State> flow =
          computeNormalFlow(
              method, new Edge(SootAdapter.asStatement(pred, method), propStmt), value);
      for (State s : flow) {
        PropagationCounter.getInstance(options.getSparsificationStrategy()).countBackward();
        propagate(currNode, s);
      }
    }
  }

  private void propagateOnCFG(
      Method method, Node<Edge, Val> currNode, Statement propStmt, Val value) {
    for (Statement pred : method.getControlFlowGraph().getPredsOf(propStmt)) {
      Collection<State> flow = computeNormalFlow(method, new Edge(pred, propStmt), value);
      for (State s : flow) {
        PropagationCounter.getInstance(options.getSparsificationStrategy()).countBackward();
        propagate(currNode, s);
      }
    }
  }

  /**
   * BackwardQuery: (b2 (target.aliasing.Aliasing1.<target.aliasing.Aliasing1: void
   * main(java.lang.String[])>),b2.secret = $stack9 -> return)
   *
   * @param method
   * @param val
   * @param stmt
   * @return
   */
  private SparseAliasingCFG getSparseCFG(
      BackwardQuery query, Method method, Val val, Statement stmt) {
    SparseAliasingCFG sparseCFG;
    SparseCFGCache sparseCFGCache =
        SparseCFGCache.getInstance(
            options.getSparsificationStrategy(), options.ignoreSparsificationAfterQuery());
    sparseCFG =
        sparseCFGCache.getSparseCFGForBackwardPropagation(
            query.var(), query.asNode().stmt().getStart(), method, val, stmt);
    return sparseCFG;
  }

  private SparseAliasingCFG getSparseCFG(
      BackwardQuery query,
      Method method,
      Val val,
      Statement stmt,
      SparseCFGCache.SparsificationStrategy strategy) {
    SparseAliasingCFG sparseCFG;
    SparseCFGCache sparseCFGCache =
        SparseCFGCache.getInstance(strategy, options.ignoreSparsificationAfterQuery());
    sparseCFG =
        sparseCFGCache.getSparseCFGForBackwardPropagation(
            query.var(), query.asNode().stmt().getStart(), method, val, stmt);
    return sparseCFG;
  }

  protected Collection<? extends State> computeCallFlow(
      Edge callSiteEdge, Val fact, Method callee, Edge calleeStartEdge) {
    Statement calleeSp = calleeStartEdge.getTarget();
    return flowFunction.callFlow(callSiteEdge.getTarget(), fact, callee, calleeSp).stream()
        .map(x -> new PushNode<>(calleeStartEdge, x, callSiteEdge, PDSSystem.CALLS))
        .collect(Collectors.toSet());
  }

  @Override
  public void processPush(
      Node<Edge, Val> curr, Location location, PushNode<Edge, Val, ?> succ, PDSSystem system) {
    if (PDSSystem.CALLS == system) {
      if (!((PushNode<Edge, Val, Edge>) succ).location().getTarget().equals(curr.stmt().getStart())
          || !(curr.stmt().getStart().containsInvokeExpr())) {
        throw new RuntimeException("Invalid push rule");
      }
    }
    super.processPush(curr, location, succ, system);
  }

  @Override
  protected Collection<State> computeNormalFlow(Method method, Edge currEdge, Val fact) {
    return flowFunction.normalFlow(currEdge, fact).stream().collect(Collectors.toSet());
  }

  @Override
  public void applyCallSummary(
      Edge callSiteEdge, Val factAtSpInCallee, Edge spInCallee, Edge exitStmt, Val exitingFact) {
    Set<Node<Edge, Val>> out = Sets.newHashSet();
    Statement callSite = callSiteEdge.getTarget();
    if (callSite.containsInvokeExpr()) {
      if (exitingFact.isThisLocal()) {
        if (callSite.getInvokeExpr().isInstanceInvokeExpr()) {
          out.add(new Node<>(callSiteEdge, callSite.getInvokeExpr().getBase()));
        }
      }
      if (exitingFact.isReturnLocal()) {
        if (callSite.isAssign()) {
          out.add(new Node<>(callSiteEdge, callSite.getLeftOp()));
        }
      }
      for (int i = 0; i < callSite.getInvokeExpr().getArgs().size(); i++) {
        if (exitingFact.isParameterLocal(i)) {
          out.add(new Node<>(callSiteEdge, callSite.getInvokeExpr().getArg(i)));
        }
      }
    }
    for (Node<Edge, Val> xs : out) {
      addNormalCallFlow(new Node<>(callSiteEdge, exitingFact), xs);
      addNormalFieldFlow(new Node<>(exitStmt, exitingFact), xs);
    }
  }

  @Override
  protected void propagateUnbalancedToCallSite(
      Statement callSite, Transition<Edge, INode<Val>> transInCallee) {
    GeneratedState<Val, Edge> target = (GeneratedState<Val, Edge>) transInCallee.getTarget();

    if (!callSite.containsInvokeExpr()) {
      throw new RuntimeException("Invalid propagate Unbalanced return");
    }
    if (!isMatchingCallSiteCalleePair(callSite, transInCallee.getLabel().getMethod())) {
      return;
    }
    cfg.addSuccsOfListener(
        new SuccessorListener(callSite) {
          @Override
          public void getSuccessor(Statement succ) {
            cfg.addPredsOfListener(
                new PredecessorListener(callSite) {
                  @Override
                  public void getPredecessor(Statement pred) {
                    Node<ControlFlowGraph.Edge, Val> curr =
                        new Node<>(new Edge(callSite, succ), query.var());

                    Transition<ControlFlowGraph.Edge, INode<Val>> callTrans =
                        new Transition<>(
                            wrap(curr.fact()),
                            curr.stmt(),
                            generateCallState(wrap(curr.fact()), curr.stmt()));
                    callAutomaton.addTransition(callTrans);
                    callAutomaton.addUnbalancedState(
                        generateCallState(wrap(curr.fact()), curr.stmt()), target);

                    State s =
                        new PushNode<>(
                            target.location(),
                            target.node().fact(),
                            new Edge(pred, callSite),
                            PDSSystem.CALLS);
                    propagate(curr, s);
                  }
                });
          }
        });
  }

  private final class CallSiteCalleeListener implements CalleeListener<Statement, Method> {
    private final Statement callSite;
    private final Node<Edge, Val> curr;
    private final Method caller;

    private CallSiteCalleeListener(Node<Edge, Val> curr, Method caller) {
      this.curr = curr;
      this.callSite = curr.stmt().getStart();
      this.caller = caller;
    }

    @Override
    public Statement getObservedCaller() {
      return callSite;
    }

    @Override
    public void onCalleeAdded(Statement callSite, Method callee) {
      if (callee.isStaticInitializer()) {
        return;
      }
      for (Statement calleeSp : icfg.getStartPointsOf(callee)) {
        for (Statement predOfCall :
            callSite.getMethod().getControlFlowGraph().getPredsOf(callSite)) {
          Collection<? extends State> res =
              computeCallFlow(
                  new Edge(predOfCall, callSite),
                  curr.fact(),
                  callee,
                  new Edge(calleeSp, calleeSp));
          for (State o : res) {
            BackwardBoomerangSolver.this.propagate(curr, o);
          }
        }
      }
    }

    @Override
    public void onNoCalleeFound() {
      byPassFlowAtCallsite(caller, curr);
    }

    @Override
    public int hashCode() {
      final int prime = 31;
      int result = 1;
      result = prime * result + getOuterType().hashCode();
      result = prime * result + ((caller == null) ? 0 : caller.hashCode());
      result = prime * result + ((curr == null) ? 0 : curr.hashCode());
      return result;
    }

    @Override
    public boolean equals(Object obj) {
      if (this == obj) return true;
      if (obj == null) return false;
      if (getClass() != obj.getClass()) return false;
      CallSiteCalleeListener other = (CallSiteCalleeListener) obj;
      if (!getOuterType().equals(other.getOuterType())) return false;
      if (caller == null) {
        if (other.caller != null) return false;
      } else if (!caller.equals(other.caller)) return false;
      if (curr == null) {
        if (other.curr != null) return false;
      } else if (!curr.equals(other.curr)) return false;
      return true;
    }

    private BackwardBoomerangSolver getOuterType() {
      return BackwardBoomerangSolver.this;
    }
  }

  @Override
  public String toString() {
    return "BackwardBoomerangSolver{" + "query=" + query + '}';
  }
}
