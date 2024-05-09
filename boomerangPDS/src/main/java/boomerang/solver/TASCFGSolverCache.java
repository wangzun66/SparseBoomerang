package boomerang.solver;

import boomerang.scene.sparse.SparseAliasingCFG;
import java.util.HashMap;
import java.util.Map;
import javax.annotation.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TASCFGSolverCache {

  private static final Logger LOGGER = LoggerFactory.getLogger(TASCFGSolverCache.class);

  Map<String, Map<String, SparseAliasingCFG>> cache;

  private static TASCFGSolverCache INSTANCE;

  public static TASCFGSolverCache getInstance() {
    if (INSTANCE == null) {
      INSTANCE = new TASCFGSolverCache();
    }
    return INSTANCE;
  }

  private TASCFGSolverCache() {
    cache = new HashMap<>();
  }

  public void reset() {
    cache = new HashMap<>();
  }

  @Nullable
  public SparseAliasingCFG get(String methodSig, String type) {
    if (cache.containsKey(methodSig)) {
      Map<String, SparseAliasingCFG> scfgMap = cache.get(methodSig);
      if (scfgMap.containsKey(type)) {
        LOGGER.info("Retrieved in TASCFGSolverCache");
        return scfgMap.get(type);
      }
    }
    return null;
  }

  public void put(String methodSig, String type, SparseAliasingCFG scfg) {
    Map<String, SparseAliasingCFG> scfgMap;
    if (cache.containsKey(methodSig)) {
      scfgMap = cache.get(methodSig);
    } else {
      scfgMap = new HashMap<>();
      cache.put(methodSig, scfgMap);
    }
    scfgMap.put(type, scfg);
  }
}
