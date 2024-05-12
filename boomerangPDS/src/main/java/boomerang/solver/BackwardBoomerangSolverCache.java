package boomerang.solver;

import boomerang.scene.sparse.SparseAliasingCFG;
import java.util.HashMap;
import java.util.Map;
import javax.annotation.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class BackwardBoomerangSolverCache {

  private static final Logger LOGGER = LoggerFactory.getLogger(BackwardBoomerangSolverCache.class);

  Map<String, SparseAliasingCFG> cache;

  private static BackwardBoomerangSolverCache INSTANCE;

  public static BackwardBoomerangSolverCache getInstance() {
    if (INSTANCE == null) {
      INSTANCE = new BackwardBoomerangSolverCache();
    }
    return INSTANCE;
  }

  private BackwardBoomerangSolverCache() {
    cache = new HashMap<>();
  }

  public void reset() {
    cache = new HashMap<>();
  }

  @Nullable
  public SparseAliasingCFG get(String methodSig) {
    if (cache.containsKey(methodSig)) {
      LOGGER.info("Retrieved in SolverCache");
      return cache.get(methodSig);
    }
    return null;
  }

  public void put(String methodSig, SparseAliasingCFG scfg) {
    if (!cache.containsKey(methodSig)) {
      cache.put(methodSig, scfg);
    }
  }
}
