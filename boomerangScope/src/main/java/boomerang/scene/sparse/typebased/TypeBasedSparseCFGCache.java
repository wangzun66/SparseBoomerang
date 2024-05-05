package boomerang.scene.sparse.typebased;

import boomerang.scene.Method;
import boomerang.scene.Statement;
import boomerang.scene.Val;
import boomerang.scene.sparse.SootAdapter;
import boomerang.scene.sparse.SparseAliasingCFG;
import boomerang.scene.sparse.SparseCFGCache;
import boomerang.scene.sparse.eval.MainQueryInfo;
import boomerang.scene.sparse.eval.SparseCFGQueryLog;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import soot.SootMethod;
import soot.jimple.Stmt;

public class TypeBasedSparseCFGCache implements SparseCFGCache {
  private static final Logger LOGGER = LoggerFactory.getLogger(TypeBasedSparseCFGCache.class);

  List<SparseCFGQueryLog> logList = new ArrayList<>();

  Map<String, Map<String, SparseAliasingCFG>> cache;
  TypeBasedSparseCFGBuilder sparseCFGBuilder;

  private static TypeBasedSparseCFGCache INSTANCE;

  private TypeBasedSparseCFGCache() {}

  public static TypeBasedSparseCFGCache getInstance() {
    if (INSTANCE == null) {
      INSTANCE = new TypeBasedSparseCFGCache(new TypeBasedSparseCFGBuilder(true));
    }
    return INSTANCE;
  }

  private TypeBasedSparseCFGCache(TypeBasedSparseCFGBuilder sparseCFGBuilder) {
    this.cache = new HashMap<>();
    this.sparseCFGBuilder = sparseCFGBuilder;
  }

  public SparseAliasingCFG getSparseCFGForForwardPropagation(SootMethod m, Stmt stmt, Val val) {
    String methodSign = m.getSignature();
    Map<String, SparseAliasingCFG> scfgMap = new HashMap<>();
    if (cache.containsKey(methodSign)) {
      scfgMap = cache.get(methodSign);
      String type = MainQueryInfo.getInstance().getVal().getType().toString();
      if (scfgMap.containsKey(type)) {
        SparseAliasingCFG scfg = scfgMap.get(type);
        if (scfg.getGraph().nodes().contains(stmt)) {
          LOGGER.info("FW Retrieve SCFG for {}", m);
          SparseCFGQueryLog queryLog =
              new SparseCFGQueryLog(true, SparseCFGQueryLog.QueryDirection.FWD);
          logList.add(queryLog);
          return scfg;
        }
      }
    }
    for (SparseAliasingCFG scfg : scfgMap.values()) {
      if (scfg.getGraph().nodes().contains(stmt)) {
        LOGGER.info("FW Retrieve SCFG for {}", m);
        SparseCFGQueryLog queryLog =
            new SparseCFGQueryLog(true, SparseCFGQueryLog.QueryDirection.FWD);
        logList.add(queryLog);
        return scfg;
      }
    }
    LOGGER.info("Original CFG for {}", m);
    SparseCFGQueryLog queryLog = new SparseCFGQueryLog(false, SparseCFGQueryLog.QueryDirection.FWD);
    logList.add(queryLog);
    return null;
  }

  public synchronized SparseAliasingCFG getSparseCFGForBackwardPropagation(
      Val initialQueryVal,
      Statement initialQueryStmt,
      Method currentMethod,
      Val currentVal,
      Statement currentStmt) {

    SootMethod sootSurrentMethod = SootAdapter.asSootMethod(currentMethod);
    Stmt sootCurrentStmt = SootAdapter.asStmt(currentStmt);

    String key = sootSurrentMethod.getSignature();
    String type = initialQueryVal.getType().toString();

    // if not we'll built another sparseCFG for the currentStmt
    if (cache.containsKey(key)) {
      Map<String, SparseAliasingCFG> scfgMap = cache.get(key);
      if (scfgMap.containsKey(type)) {
        SparseAliasingCFG scfg = scfgMap.get(type);
        if (scfg.getGraph().nodes().contains(sootCurrentStmt)) {
          LOGGER.info("BW Retrieve SCFG for {}", sootSurrentMethod.toString());
          SparseCFGQueryLog queryLog =
              new SparseCFGQueryLog(true, SparseCFGQueryLog.QueryDirection.BWD);
          logList.add(queryLog);
          return scfg;
        } else {
          return createNewTASCFG(initialQueryVal, sootSurrentMethod, sootCurrentStmt, type);
        }
      } else {
        return createNewTASCFG(initialQueryVal, sootSurrentMethod, sootCurrentStmt, type);
      }
    } else {
      return createNewTASCFG(initialQueryVal, sootSurrentMethod, sootCurrentStmt, type);
    }
  }

  public SparseAliasingCFG createNewTASCFG(
      Val initialQueryVal, SootMethod sootSurrentMethod, Stmt sootCurrentStmt, String type) {
    SparseCFGQueryLog queryLog = new SparseCFGQueryLog(false, SparseCFGQueryLog.QueryDirection.BWD);
    LOGGER.info("BW Build SCFG for {}", sootSurrentMethod.toString());
    queryLog.logStart();
    SparseAliasingCFG scfg =
        sparseCFGBuilder.buildSparseCFG(
            initialQueryVal, sootSurrentMethod, sootCurrentStmt, queryLog);
    queryLog.logEnd();
    logList.add(queryLog);
    put(sootSurrentMethod.getSignature(), type, scfg);
    return scfg;
  }

  public void put(String methodSignature, String type, SparseAliasingCFG scfg) {
    Map<String, SparseAliasingCFG> scfgMap;
    if (cache.containsKey(methodSignature)) {
      scfgMap = cache.get(methodSignature);
    } else {
      scfgMap = new HashMap<>();
      cache.put(methodSignature, scfgMap);
    }
    scfgMap.put(type, scfg);
  }

  @Override
  public List<SparseCFGQueryLog> getQueryLogs() {
    return logList;
  }
}
