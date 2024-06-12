package boomerang.datacollection;

import boomerang.BackwardQuery;
import boomerang.scene.sparse.eval.SparseCFGQueryLog;
import java.util.ArrayList;
import java.util.List;

/** Collect data for each BackwardQuery */
public class QueryLog {

  private BackwardQuery query;

  private List<MethodLog> methodLogs = new ArrayList<>();

  private List<SparseCFGQueryLog> scfgLogs = new ArrayList<>();

  private List<DecisionLog> decisionLogs = new ArrayList<>();

  private String currentMethodSig = "";

  private MethodLog currentMethodLog = null;

  public String getCurrentMethodSig() {
    return this.currentMethodSig;
  }

  public MethodLog getCurrentMethodLog() {
    return currentMethodLog;
  }

  public void setCurrentMethodSig(String currentMethodSig) {
    this.currentMethodSig = currentMethodSig;
  }

  public void setCurrentMethodLog(MethodLog currentMethodLog) {
    this.currentMethodLog = currentMethodLog;
  }

  public QueryLog(BackwardQuery query) {
    this.query = query;
  }

  public void addMethodLog(MethodLog log) {
    this.methodLogs.add(log);
  }

  public void addDecisionLog(DecisionLog log) {
    this.decisionLogs.add(log);
  }

  public BackwardQuery getQuery() {
    return this.query;
  }

  public List<MethodLog> getMethodLogs() {
    return this.methodLogs;
  }

  public List<SparseCFGQueryLog> getSCFGLogs() {
    return scfgLogs;
  }

  public List<DecisionLog> getDecisionLogs() {
    return this.decisionLogs;
  }

  public void storeSCFGLogs(List<SparseCFGQueryLog> list) {
    this.scfgLogs.addAll(list);
  }
}
