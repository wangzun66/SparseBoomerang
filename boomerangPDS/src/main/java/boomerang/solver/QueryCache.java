package boomerang.solver;

import boomerang.scene.sparse.SparseAliasingCFG;
import java.util.HashMap;
import java.util.Map;
import javax.annotation.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class QueryCache {

  private static final Logger LOGGER = LoggerFactory.getLogger(QueryCache.class);

  Map<String, SparseAliasingCFG> cache;

  private static QueryCache INSTANCE;

  public static QueryCache getInstance() {
    if (INSTANCE == null) {
      INSTANCE = new QueryCache();
    }
    return INSTANCE;
  }

  private QueryCache() {
    cache = new HashMap<>();
  }

  public void reset() {
    cache = new HashMap<>();
  }

  @Nullable
  public SparseAliasingCFG get(String methodSig) {
    if (cache.containsKey(methodSig)) {
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
