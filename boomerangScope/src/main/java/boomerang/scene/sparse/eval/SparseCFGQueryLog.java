package boomerang.scene.sparse.eval;

import boomerang.scene.sparse.SparseCFGCache;
import com.google.common.base.Stopwatch;
import java.time.Duration;
import javax.annotation.Nullable;
import soot.SootMethod;
import soot.Value;
import soot.jimple.Stmt;

/**
 * For logging if a SparseCFG was retrieved from cache, or it was built for the first time. And how
 * long did it take to build it.
 */
public class SparseCFGQueryLog {

  private boolean retrievedFromCache;
  private SparseCFGCache.SparsificationStrategy strategy;
  private int initialStmtCount = 0;
  private int finalStmtCount = 0;

  private Stmt stmt = null;
  private Value value = null;
  private SootMethod method;
  private String scfg;
  private Stopwatch watch;
  private Duration duration = Duration.ZERO;

  public SparseCFGQueryLog(
      boolean retrievedFromCache,
      SootMethod method,
      Value value,
      Stmt stmt,
      String scfg,
      SparseCFGCache.SparsificationStrategy strategy) {
    this.retrievedFromCache = retrievedFromCache;
    this.method = method;
    this.stmt = stmt;
    this.value = value;
    this.scfg = scfg;
    this.strategy = strategy;
    this.watch = Stopwatch.createUnstarted();
  }

  @Nullable
  public Value getValue() {
    return this.value;
  }

  @Nullable
  public Stmt getStmt() {
    return this.stmt;
  }

  public SootMethod getMethod() {
    return this.method;
  }

  public String getScfg() {
    return this.scfg;
  }

  public void setScfg(String scfg) {
    this.scfg = scfg;
  }

  public SparseCFGCache.SparsificationStrategy getStrategy() {
    return this.strategy;
  }

  public boolean isRetrievedFromCache() {
    return retrievedFromCache;
  }

  public int getInitialStmtCount() {
    return initialStmtCount;
  }

  public void setInitialStmtCount(int initialStmtCount) {
    this.initialStmtCount = initialStmtCount;
  }

  public int getFinalStmtCount() {
    return finalStmtCount;
  }

  public void setFinalStmtCount(int finalStmtCount) {
    this.finalStmtCount = finalStmtCount;
  }

  public void logStart() {
    if (!retrievedFromCache) {
      this.watch.start();
    }
  }

  public void logEnd() {
    if (!retrievedFromCache) {
      this.watch.stop();
      duration = watch.elapsed();
    }
  }

  public Duration getDuration() {
    return this.duration;
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    if (this.strategy == SparseCFGCache.SparsificationStrategy.TYPE_BASED) {
      sb.append("TAS-");
    } else if (this.strategy == SparseCFGCache.SparsificationStrategy.ALIAS_AWARE) {
      sb.append("AAS-");
    }
    sb.append("CFG for method: ");
    sb.append(method.getSignature());
    sb.append(" is ");
    if (retrievedFromCache) {
      sb.append("retrieved ");
    } else {
      sb.append("built ");
    }
    if (stmt != null) {
      sb.append("at stmt: ");
      sb.append(stmt);
      sb.append(", ");
    }
    if (value != null) {
      sb.append("with value: ");
      sb.append(value);
    }
    return sb.toString();
  }
}
