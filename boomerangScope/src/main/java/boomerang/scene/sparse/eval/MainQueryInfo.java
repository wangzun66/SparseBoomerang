package boomerang.scene.sparse.eval;

import boomerang.scene.Method;
import boomerang.scene.Statement;
import boomerang.scene.Val;

/**
 * This class is used to transfer main query info from boomerangPDS to boomerangScope The instance
 * is created in FlowDroid Manager
 */
public class MainQueryInfo {

  private static MainQueryInfo INSTANCE;

  private Method method = null;
  private Statement stmt = null;
  private Val val = null;

  public static MainQueryInfo getInstance() {
    if (INSTANCE == null) {
      INSTANCE = new MainQueryInfo();
    }
    return INSTANCE;
  }

  public void setInfo(Method method, Statement stmt, Val val) {
    this.method = method;
    this.stmt = stmt;
    this.val = val;
  }

  public Method getMethod() {
    return method;
  }

  public Val getVal() {
    return val;
  }

  public Statement getStmt() {
    return stmt;
  }
}
