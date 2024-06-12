package boomerang.scene.sparse.typebased;

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

  public SparseAliasingCFG getSparseCFGForForwardPropagation(
      SootMethod m, String initialQueryType) {
    String key = m.getSignature();
    if (cache.containsKey(key)) {
      Map<String, SparseAliasingCFG> scfgMap = cache.get(key);
      if (scfgMap.containsKey(initialQueryType)) {
        SparseAliasingCFG scfg = scfgMap.get(initialQueryType);
        SparseCFGQueryLog scfgLog =
            new SparseCFGQueryLog(
                true, m, null, null, scfg.toString(), SparsificationStrategy.TYPE_BASED);
        // LOGGER.info(scfgLog.toString());
        logList.add(scfgLog);
        return scfg;
      }
    }
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
        SparseCFGQueryLog scfgLog =
            new SparseCFGQueryLog(
                true,
                sootCurrentMethod,
                null,
                null,
                scfg.toString(),
                SparsificationStrategy.TYPE_BASED);
        // LOGGER.info(scfgLog.toString());
        logList.add(scfgLog);
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
    SparseCFGQueryLog scfgLog =
        new SparseCFGQueryLog(
            false,
            sootCurrentMethod,
            SootAdapter.asValue(initialQueryVal),
            null,
            null,
            SparsificationStrategy.TYPE_BASED);
    SparseAliasingCFG scfg =
        sparseCFGBuilder.buildSparseCFG(
            initialQueryVal, sootCurrentMethod, sootCurrentStmt, scfgLog);
    // queryLog.logEnd();
    scfgLog.setScfg(scfg.toString());
    // LOGGER.info(scfgLog.toString());
    logList.add(scfgLog);
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
  public List<SparseCFGQueryLog> getSCFGLogs() {
    return logList;
  }

  @Override
  public void resetSCFGLogs() {
    this.logList = new ArrayList<>();
  }

  @Override
  public Map<String, Map<String, Set<SparseAliasingCFG>>> getCache() {
    Map<String, Map<String, Set<SparseAliasingCFG>>> result = new HashMap<>();
    for (String ms : cache.keySet()) {
      result.put(ms, new HashMap<>());
      Map<String, Set<SparseAliasingCFG>> type2Scfgs = result.get(ms);
      for (String type : cache.get(ms).keySet()) {
        Set<SparseAliasingCFG> scfg = new HashSet<>();
        scfg.add(cache.get(ms).get(type));
        type2Scfgs.put(type, scfg);
      }
    }
    return result;
  }
}
