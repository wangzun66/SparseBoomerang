package boomerang.datacollection;

import boomerang.BackwardQuery;
import boomerang.scene.sparse.eval.SparseCFGQueryLog;
import java.util.ArrayList;
import java.util.List;

/** Collect data for each BackwardQuery */
public class QueryLog {

  private BackwardQuery query;

  private List<MethodLog> logs = new ArrayList<>();

  private List<SparseCFGQueryLog> scfgLogs = new ArrayList<>();

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

  public void addLog(MethodLog log) {
    this.logs.add(log);
  }

  public BackwardQuery getQuery() {
    return this.query;
  }

  public List<MethodLog> getLogList() {
    return this.logs;
  }

  public List<SparseCFGQueryLog> getSCFGLogList() {
    return scfgLogs;
  }

  public void storeSCFGLogList(List<SparseCFGQueryLog> list) {
    this.scfgLogs = list;
  }
}
