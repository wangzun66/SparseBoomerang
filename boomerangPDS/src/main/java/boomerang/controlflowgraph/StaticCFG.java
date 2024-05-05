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
import boomerang.solver.TASCFGSolverCache;
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

  public void setCurrentVal(Val val) {
    this.currentVal = val;
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
      LOGGER.info("Take SCFG for {}", method.toString());
      SparseAliasingCFG sparseCFG = null;
      String methodSig = SootAdapter.asSootMethod(method).getSignature();
      Stmt currStmt = SootAdapter.asStmt(curr);
      if (methodSig.equals(currMethodSig) && currentSCFG != null) {
        sparseCFG = currentSCFG;
        LOGGER.info("Retrieved in ForwardBoomerangSolver");
      } else {
        if (sparsificationStrategy == SparseCFGCache.SparsificationStrategy.TYPE_BASED) {
          sparseCFG = TASCFGSolverCache.getInstance().get(methodSig);
        } else if (sparsificationStrategy == SparseCFGCache.SparsificationStrategy.ALIAS_AWARE) {
          // todo: add for AAS
        }
        if (sparseCFG == null) {
          sparseCFG = getSparseCFG(method, curr, currentVal);
        }
      }
      currentSCFG = null;
      currMethodSig = "";
      if (sparseCFG != null && sparseCFG.getGraph().nodes().contains(currStmt)) {
        currentSCFG = sparseCFG;
        currMethodSig = methodSig;
        propagateSparse(l, method, curr, sparseCFG);
      } else if (options.handleSpecialInvokeAsNormalPropagation()) {
        propagateDefault(l);
      } else {
        propagateDefault(l); // back up when not found
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

  private SparseAliasingCFG getSparseCFG(Method method, Statement stmt, Val currentVal) {
    SootMethod sootMethod = ((JimpleMethod) method).getDelegate();
    Stmt sootStmt = ((JimpleStatement) stmt).getDelegate();
    SparseCFGCache sparseCFGCache =
        SparseCFGCache.getInstance(
            sparsificationStrategy, options.ignoreSparsificationAfterQuery());
    SparseAliasingCFG sparseCFG =
        sparseCFGCache.getSparseCFGForForwardPropagation(sootMethod, sootStmt, currentVal);
    return sparseCFG;
  }

  @Override
  public void step(Statement curr, Statement succ) {}

  @Override
  public void unregisterAllListeners() {}
}
