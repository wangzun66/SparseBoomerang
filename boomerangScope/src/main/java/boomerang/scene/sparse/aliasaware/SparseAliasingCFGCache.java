package boomerang.scene.sparse.aliasaware;

import boomerang.scene.Method;
import boomerang.scene.Statement;
import boomerang.scene.Val;
import boomerang.scene.sparse.SootAdapter;
import java.util.HashMap;
import java.util.Map;
import soot.SootMethod;
import soot.Value;
import soot.jimple.Stmt;

public class SparseAliasingCFGCache {

  Map<String, SparseAliasingCFG> cache;
  SparseAliasingCFGBuilder sparseCFGBuilder;

  private static SparseAliasingCFGCache INSTANCE;

  private SparseAliasingCFGCache() {}

  public static SparseAliasingCFGCache getInstance() {
    if (INSTANCE == null) {
      INSTANCE = new SparseAliasingCFGCache(new SparseAliasingCFGBuilder(true));
    }
    return INSTANCE;
  }

  private SparseAliasingCFGCache(SparseAliasingCFGBuilder sparseCFGBuilder) {
    this.cache = new HashMap<>();
    this.sparseCFGBuilder = sparseCFGBuilder;
  }

  public SparseAliasingCFG getSparseCFG(SootMethod m, Stmt stmt) {
    for (String s : cache.keySet()) {
      if (s.startsWith(m.getSignature())) {
        SparseAliasingCFG sparseAliasingCFG = cache.get(s);
        if (sparseAliasingCFG.getGraph().nodes().contains(stmt)) {
          return sparseAliasingCFG;
        }
      }
    }
    // throw new RuntimeException("CFG not found for:" + m + " s:" + stmt);
    return null;
  }

  public synchronized SparseAliasingCFG getSparseCFG(
      Val initialQueryVal,
      Statement initialQueryStmt,
      Method currentMethod,
      Val currentVal,
      Statement currentStmt) {

    SootMethod sootSurrentMethod = SootAdapter.asSootMethod(currentMethod);
    Stmt sootInitialQueryStmt = SootAdapter.asStmt(initialQueryStmt);
    Stmt sootCurrentStmt = SootAdapter.asStmt(currentStmt);
    Value sootInitialQueryVal = SootAdapter.asValue(initialQueryVal);
    Value sootCurrentQueryVal = SootAdapter.asValue(currentVal);

    String key =
        new StringBuilder(sootSurrentMethod.getSignature())
            .append("-")
            .append(initialQueryVal)
            .append("-")
            .append(sootInitialQueryStmt)
            .toString();

    if (cache.containsKey(key)) {
      if (cache.get(key).getGraph().nodes().contains(sootCurrentStmt)) {
        return cache.get(key);
      } else {
        SparseAliasingCFG cfg =
            sparseCFGBuilder.buildSparseCFG(
                sootInitialQueryVal, sootSurrentMethod, sootCurrentQueryVal, sootCurrentStmt);
        cache.put(key + currentStmt, cfg);
        return cfg;
      }
    } else if (cache.containsKey(key + currentStmt)) {
      return cache.get(key + currentStmt);
    } else {
      SparseAliasingCFG cfg =
          sparseCFGBuilder.buildSparseCFG(
              sootInitialQueryVal, sootSurrentMethod, sootCurrentQueryVal, sootCurrentStmt);
      cache.put(key, cfg);
      return cfg;
    }
  }
}
