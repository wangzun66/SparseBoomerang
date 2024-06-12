package boomerang.solver;

import boomerang.scene.sparse.EmptySparseCFG;
import boomerang.scene.sparse.SparseCFG;
import java.util.HashMap;
import java.util.Map;
import javax.annotation.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SCFGSolverCache {

  private static final Logger LOGGER = LoggerFactory.getLogger(SCFGSolverCache.class);

  Map<String, SparseCFG> cache;

  private static SCFGSolverCache INSTANCE;

  public static SCFGSolverCache getInstance() {
    if (INSTANCE == null) {
      INSTANCE = new SCFGSolverCache();
    }
    return INSTANCE;
  }

  private SCFGSolverCache() {
    cache = new HashMap<>();
  }

  public void reset() {
    cache = new HashMap<>();
  }

  @Nullable
  public SparseCFG get(String methodSig) {
    if (cache.containsKey(methodSig)) {
      SparseCFG scfg = cache.get(methodSig);
      if(scfg instanceof EmptySparseCFG){
        LOGGER.info("Retrieve EmptySparseCFG from SCFGSolverCache for method: {}", methodSig);
      }else {
        LOGGER.info("Retrieve Sparse CFG from SCFGSolverCache for method: {}", methodSig);
      }
      return cache.get(methodSig);
    }
    return null;
  }

  public void put(String methodSig, SparseCFG scfg) {
    if (!cache.containsKey(methodSig)) {
      cache.put(methodSig, scfg);
    }
  }
}
