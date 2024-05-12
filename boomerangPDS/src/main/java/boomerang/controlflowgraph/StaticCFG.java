package boomerang.controlflowgraph;

import boomerang.BoomerangOptions;
import boomerang.scene.Method;
import boomerang.scene.Statement;
import boomerang.scene.Val;
import boomerang.scene.jimple.JimpleMethod;
import boomerang.scene.jimple.JimpleStatement;
import boomerang.scene.sparse.SootAdapter;
import boomerang.scene.sparse.SparseAliasingCFG;
import boomerang.scene.sparse.SparseCFGCache;
import boomerang.scene.sparse.eval.PropagationCounter;
import boomerang.solver.BackwardBoomerangSolverCache;
import java.util.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import soot.SootMethod;
import soot.Unit;
import soot.jimple.Stmt;

public class StaticCFG implements ObservableControlFlowGraph {

  private static final Logger LOGGER = LoggerFactory.getLogger(StaticCFG.class);

  private SparseCFGCache.SparsificationStrategy sparsificationStrategy;

  private BoomerangOptions options;

  private Val currentVal;

  private SparseAliasingCFG currentSCFG = null;
  private String currMethodSig = "";
  private String initialQueryVarType;

  public void setCurrentVal(Val val) {
    this.currentVal = val;
  }

  public void setInitialQueryVarType(String type) {
    this.initialQueryVarType = type;
  }

  public StaticCFG(BoomerangOptions options) {
    this.options = options;
    this.sparsificationStrategy = options.getSparsificationStrategy();
  }

  @Override
  public void addPredsOfListener(PredecessorListener l) {
    for (Statement s : l.getCurr().getMethod().getControlFlowGraph().getPredsOf(l.getCurr())) {
      l.getPredecessor(s);
    }
  }

  @Override
  public void addSuccsOfListener(SuccessorListener l) {
    Method method = l.getCurr().getMethod();
    Statement curr = l.getCurr();
    if (sparsificationStrategy != SparseCFGCache.SparsificationStrategy.NONE) {
      String methodSig = SootAdapter.asSootMethod(method).getSignature();
      Stmt currStmt = SootAdapter.asStmt(curr);
      if (methodSig.equals(currMethodSig)) {
      } else {
        SparseAliasingCFG sparseCFG = BackwardBoomerangSolverCache.getInstance().get(methodSig);
        currentSCFG = sparseCFG;
      }
      if (currentSCFG != null && currentSCFG.getGraph().nodes().contains(currStmt)) {
        propagateSparse(l, method, curr, currentSCFG);
      } else {
        propagateDefault(l);
      }
    } else {
      propagateDefault(l);
    }
  }

  private void propagateSparse(
      SuccessorListener l, Method method, Statement curr, SparseAliasingCFG sparseCFG) {
    Set<Unit> successors = sparseCFG.getGraph().successors(SootAdapter.asStmt(curr));
    for (Unit succ : successors) {
      PropagationCounter.getInstance(sparsificationStrategy).countForward();
      l.getSuccessor(SootAdapter.asStatement(succ, method));
    }
  }

  private void propagateDefault(SuccessorListener l) {
    for (Statement s : l.getCurr().getMethod().getControlFlowGraph().getSuccsOf(l.getCurr())) {
      PropagationCounter.getInstance(sparsificationStrategy).countForward();
      l.getSuccessor(s);
    }
  }

  /**
   * It is not possible take an aas-scfg in global cache for forward propagation, because the first
   * encountered stmt must be not the stmt which is used for building the aas-scfg for the current
   * resolving query. Therefore, we only retrieve aas-scfgs from solver cache
   */
  @Deprecated
  private SparseAliasingCFG getSparseCFG(
      Method method, Statement stmt, Val currentVal, String initialQueryVarType) {
    SootMethod sootMethod = ((JimpleMethod) method).getDelegate();
    Stmt sootStmt = ((JimpleStatement) stmt).getDelegate();
    SparseCFGCache sparseCFGCache =
        SparseCFGCache.getInstance(
            sparsificationStrategy, options.ignoreSparsificationAfterQuery());
    SparseAliasingCFG sparseCFG =
        sparseCFGCache.getSparseCFGForForwardPropagation(
            sootMethod, sootStmt, currentVal, initialQueryVarType);
    return sparseCFG;
  }

  @Override
  public void step(Statement curr, Statement succ) {}

  @Override
  public void unregisterAllListeners() {}
}
