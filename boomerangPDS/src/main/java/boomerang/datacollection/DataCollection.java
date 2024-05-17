package boomerang.datacollection;

import boomerang.BackwardQuery;
import java.util.HashMap;
import java.util.Map;
import javax.annotation.Nullable;

/** Collect necessary data for each apk */
public class DataCollection {

  private static DataCollection INSTANCE;

  // private Map<BackwardQuery, QueryLog> query2Logs;
  private Map<Integer, QueryLog> queryId2Logs;

  public static DataCollection getInstance() {
    if (INSTANCE == null) {
      INSTANCE = new DataCollection();
    }
    return INSTANCE;
  }

  private DataCollection() {
    queryId2Logs = new HashMap<>();
  }

  public QueryLog registerQuery(int id, BackwardQuery query) {
    QueryLog queryLog = new QueryLog(query);
    query.setId(id);
    queryId2Logs.put(id, queryLog);
    return queryLog;
  }

  @Nullable
  public QueryLog getQueryLog(int id) {
    if (queryId2Logs.containsKey(id)) {
      return this.queryId2Logs.get(id);
    }
    return null;
  }
}
