package boomerang.scene.sparse.aliasaware;

import boomerang.scene.Method;
import boomerang.scene.Statement;
import boomerang.scene.Val;
import boomerang.scene.sparse.SootAdapter;
import boomerang.scene.sparse.SparseAliasingCFG;
import boomerang.scene.sparse.SparseCFGCache;
import boomerang.scene.sparse.eval.SparseCFGQueryLog;
import java.util.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import soot.SootMethod;
import soot.Value;
import soot.jimple.Stmt;

public class AliasAwareSparseCFGCache implements SparseCFGCache {

  private static final Logger LOGGER = LoggerFactory.getLogger(AliasAwareSparseCFGCache.class);

  List<SparseCFGQueryLog> logList = new ArrayList<>();

  Map<String, Map<String, Set<SparseAliasingCFG>>> cache;
  AliasAwareSparseCFGBuilder sparseCFGBuilder;

  private static AliasAwareSparseCFGCache INSTANCE;
  private static boolean ignore;

  public static AliasAwareSparseCFGCache getInstance(boolean ignoreAfterQuery) {
    if (INSTANCE == null || ignore != ignoreAfterQuery) {
      ignore = ignoreAfterQuery;
      INSTANCE =
          new AliasAwareSparseCFGCache(new AliasAwareSparseCFGBuilder(true, ignoreAfterQuery));
    }
    return INSTANCE;
  }

  private AliasAwareSparseCFGCache(AliasAwareSparseCFGBuilder sparseCFGBuilder) {
    this.cache = new HashMap<>();
    this.sparseCFGBuilder = sparseCFGBuilder;
  }

  /**
   * It is not possible take an aas-scfg in global cache for forward propagation, because the first
   * encountered stmt must be not the stmt which is used for building the aas-scfg. Therefore, we
   * only retrieve aas-scfgs from solver cache.
   */
  public SparseAliasingCFG getSparseCFGForForwardPropagation(
      SootMethod m, String initialQueryVarType) {
    return null;
  }

  public synchronized SparseAliasingCFG getSparseCFGForBackwardPropagation(
      Val initialQueryVal,
      Statement initialQueryStmt,
      Method currentMethod,
      Val currentVal,
      Statement currentStmt) {

    SootMethod sootCurrentMethod = SootAdapter.asSootMethod(currentMethod);
    Stmt sootCurrentStmt = SootAdapter.asStmt(currentStmt);
    Value sootCurrentValue = SootAdapter.asValue(currentVal);
    String methodSig = sootCurrentMethod.getSignature();

    if (cache.containsKey(methodSig)) {
      Map<String, Set<SparseAliasingCFG>> stmtToSCFGs = cache.get(methodSig);
      if (stmtToSCFGs.containsKey(sootCurrentStmt.toString())) {
        Set<SparseAliasingCFG> scfgs = stmtToSCFGs.get(sootCurrentStmt.toString());
        for (SparseAliasingCFG scfg : scfgs) {
          if (scfg.getFallBackAliases().contains(sootCurrentValue)) {
            SparseCFGQueryLog scfgLog =
                new SparseCFGQueryLog(
                    true,
                    sootCurrentMethod,
                    null,
                    null,
                    scfg.toString(),
                    SparsificationStrategy.ALIAS_AWARE);
            // LOGGER.info(scfgLog.toString());
            logList.add(scfgLog);
            return scfg;
          }
        }
        return createNewAASCFG(initialQueryVal, sootCurrentMethod, currentVal, sootCurrentStmt);
      } else {
        return createNewAASCFG(initialQueryVal, sootCurrentMethod, currentVal, sootCurrentStmt);
      }
    } else {
      return createNewAASCFG(initialQueryVal, sootCurrentMethod, currentVal, sootCurrentStmt);
    }
  }

  private SparseAliasingCFG createNewAASCFG(
      Val initialQueryVal, SootMethod sootCurrentMethod, Val currentVal, Stmt sootCurrentStmt) {
    SparseCFGQueryLog scfgLog =
        new SparseCFGQueryLog(
            false,
            sootCurrentMethod,
            null,
            sootCurrentStmt,
            null,
            SparsificationStrategy.ALIAS_AWARE);
    scfgLog.logStart();
    SparseAliasingCFG scfg =
        sparseCFGBuilder.buildSparseCFG(
            initialQueryVal, sootCurrentMethod, currentVal, sootCurrentStmt, scfgLog);
    scfgLog.logEnd();
    scfgLog.setScfg(scfg.toString());
    // LOGGER.info(scfgLog.toString());
    logList.add(scfgLog);
    put(sootCurrentMethod.getSignature(), sootCurrentStmt.toString(), scfg);
    return scfg;
  }

  private void put(String methodSignature, String stmtKey, SparseAliasingCFG scfg) {
    Map<String, Set<SparseAliasingCFG>> scfgsMap;
    if (cache.containsKey(methodSignature)) {
      scfgsMap = cache.get(methodSignature);
    } else {
      scfgsMap = new HashMap<>();
      cache.put(methodSignature, scfgsMap);
    }
    Set<SparseAliasingCFG> scfgs;
    if (scfgsMap.containsKey(stmtKey)) {
      scfgs = scfgsMap.get(stmtKey);
    } else {
      scfgs = new HashSet<>();
      scfgsMap.put(stmtKey, scfgs);
    }
    scfgs.add(scfg);
  }

  @Override
  public List<SparseCFGQueryLog> getSCFGLogs() {
    return logList;
  }

  @Override
  public void resetSCFGLogs() {
    this.logList = new ArrayList<>();
  }

  @Override
  public Map<String, Map<String, Set<SparseAliasingCFG>>> getCache() {
    return this.cache;
  }
}
