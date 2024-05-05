package boomerang.solver;

import boomerang.scene.sparse.SparseAliasingCFG;
import java.util.HashMap;
import java.util.Map;
import javax.annotation.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TASCFGSolverCache {

  private static final Logger LOGGER = LoggerFactory.getLogger(TASCFGSolverCache.class);
  private static TASCFGSolverCache INSTANCE;
  private Map<String, SparseAliasingCFG> methodToSCFG = new HashMap<>();

  public static TASCFGSolverCache getInstance() {
    if (INSTANCE == null) {
      INSTANCE = new TASCFGSolverCache();
    }
    return INSTANCE;
  }

  public void reset() {
    methodToSCFG = new HashMap<>();
  }

  @Nullable
  public SparseAliasingCFG get(String methodSig) {
    if (methodToSCFG.containsKey(methodSig)) {
      LOGGER.info("Retrieved in TASCFGSolverCache");
      return methodToSCFG.get(methodSig);
    } else {
      return null;
    }
  }

  public void put(String methodSig, SparseAliasingCFG scfg) {
    methodToSCFG.put(methodSig, scfg);
  }
}
