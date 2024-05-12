package boomerang.scene.sparse.typebased;

import boomerang.scene.Method;
import boomerang.scene.Statement;
import boomerang.scene.Val;
import boomerang.scene.sparse.SootAdapter;
import boomerang.scene.sparse.SparseAliasingCFG;
import boomerang.scene.sparse.SparseCFGCache;
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

  public synchronized SparseAliasingCFG getSparseCFGForForwardPropagation(
      SootMethod m, Stmt stmt, Val val, String initialQueryType) {
    String key = m.getSignature();
    if (cache.containsKey(key)) {
      Map<String, SparseAliasingCFG> scfgMap = cache.get(key);
      if (scfgMap.containsKey(initialQueryType)) {
        SparseAliasingCFG scfg = scfgMap.get(initialQueryType);
        LOGGER.info("Forward Retrieved SCFG for {} from TypeBasedSparseCFGCache", m);
        SparseCFGQueryLog queryLog =
            new SparseCFGQueryLog(true, SparseCFGQueryLog.QueryDirection.FWD);
        logList.add(queryLog);
        return scfg;
      }
    }
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

    SootMethod sootCurrentMethod = SootAdapter.asSootMethod(currentMethod);
    Stmt sootCurrentStmt = SootAdapter.asStmt(currentStmt);

    String methodSignature = sootCurrentMethod.getSignature();
    String typeKey = initialQueryVal.getType().toString();

    if (cache.containsKey(methodSignature)) {
      Map<String, SparseAliasingCFG> scfgMap = cache.get(methodSignature);
      if (scfgMap.containsKey(typeKey)) {
        SparseAliasingCFG scfg = scfgMap.get(typeKey);
        LOGGER.info(
            "Backward Retrieved SCFG for {} from TypeBasedSparseCFGCache", sootCurrentMethod);
        SparseCFGQueryLog queryLog =
            new SparseCFGQueryLog(true, SparseCFGQueryLog.QueryDirection.BWD);
        logList.add(queryLog);
        return scfg;
      } else {
        return createNewTASCFG(initialQueryVal, sootCurrentMethod, sootCurrentStmt, typeKey);
      }
    } else {
      return createNewTASCFG(initialQueryVal, sootCurrentMethod, sootCurrentStmt, typeKey);
    }
  }

  private SparseAliasingCFG createNewTASCFG(
      Val initialQueryVal,
      SootMethod sootCurrentMethod,
      Stmt sootCurrentStmt,
      String initialQueryVarType) {
    SparseCFGQueryLog queryLog = new SparseCFGQueryLog(false, SparseCFGQueryLog.QueryDirection.BWD);
    LOGGER.info("Build SCFG for {} from TypeBasedSparseCFGCache", sootCurrentMethod);
    queryLog.logStart();
    SparseAliasingCFG scfg =
        sparseCFGBuilder.buildSparseCFG(
            initialQueryVal, sootCurrentMethod, sootCurrentStmt, queryLog);
    queryLog.logEnd();
    logList.add(queryLog);
    put(sootCurrentMethod.getSignature(), initialQueryVarType, scfg);
    return scfg;
  }

  private void put(String methodSignature, String type, SparseAliasingCFG scfg) {
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
