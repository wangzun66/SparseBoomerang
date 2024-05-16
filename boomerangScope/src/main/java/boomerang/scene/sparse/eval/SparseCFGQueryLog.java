package boomerang.scene.sparse.eval;

import com.google.common.base.Stopwatch;
import soot.SootMethod;
import soot.Value;
import soot.jimple.Stmt;

import javax.annotation.Nullable;
import java.time.Duration;

/**
 * For logging if a SparseCFG was retrieved from cache, or it was built for the first time. And how
 * long did it take to build it.
 */
public class SparseCFGQueryLog {

  private boolean retrievedFromCache;

  private QueryDirection direction;

  public enum QueryDirection {
    FWD,
    BWD;
  }

  /*private final Stopwatch watch;
  private final Stopwatch cfgNumberWatch;
  private final Stopwatch findStmtsWatch;
  private final Stopwatch sparsifysWatch;*/
  private int containerTypeCount = 0;
  private int initialStmtCount = 0;
  private int finalStmtCount = 0;


  private Stmt stmt = null;
  private Value value = null;
  private SootMethod method;

  /*public SparseCFGQueryLog(boolean retrievedFromCache, QueryDirection direction) {
    this.retrievedFromCache = retrievedFromCache;
    this.direction = direction;
    if (!retrievedFromCache) {
      this.watch = Stopwatch.createUnstarted();
      this.cfgNumberWatch = Stopwatch.createUnstarted();
      this.findStmtsWatch = Stopwatch.createUnstarted();
      this.sparsifysWatch = Stopwatch.createUnstarted();
    } else {
      this.watch = null;
      this.cfgNumberWatch = null;
      this.findStmtsWatch = null;
      this.sparsifysWatch = null;
    }
  }*/

  public SparseCFGQueryLog(boolean retrievedFromCache, SootMethod method, Value value, Stmt stmt) {
    this.retrievedFromCache = retrievedFromCache;
    this.method = method;
    if(!retrievedFromCache){
      this.stmt = stmt;
      this.value = value;
    }
  }

  @Nullable
  public Value getValue(){
    return this.value;
  }

  @Nullable
  public Stmt getStmt(){
    return this.stmt;
  }

  public SootMethod getMethod(){
    return this.method;
  }

  /*public void logStart() {
    if (!retrievedFromCache) {
      this.watch.start();
    }
  }*/

  /*public void logEnd() {
    if (!retrievedFromCache) {
      this.watch.stop();
    }
  }*/

  /**
   * SparseCFG built time in microseconds, 0 if it was retrieved from the cache
   *
   * @return
   */
  /*public Duration getDuration() {
    if (!retrievedFromCache) {
      return this.watch.elapsed();
    } else {
      return Duration.ZERO;
    }
  }*/

  public boolean isRetrievedFromCache() {
    return retrievedFromCache;
  }

  public QueryDirection getDirection() {
    return direction;
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

  @Override
  public String toString(){
    StringBuilder sb = new StringBuilder();
    sb.append("SCFG method: ");
    sb.append(method.getSubSignature());
    sb.append(" is ");
    if(retrievedFromCache){
      sb.append("retrieved ");
    }else {
      sb.append("built ");
    }
    if(stmt != null){
      sb.append("at stmt: ");
      sb.append(stmt);
      sb.append(", ");
    }
    if(value != null){
      sb.append("with value: ");
      sb.append(value);
    }
    return sb.toString();
  }
}
