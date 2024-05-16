package boomerang.datacollection;

import boomerang.BackwardQuery;
import java.util.HashMap;
import java.util.Map;
import javax.annotation.Nullable;

/** Collect necessary data for each apk */
public class DataCollection {

  private static DataCollection INSTANCE;

  private Map<BackwardQuery, QueryLog> query2Logs;

  public static DataCollection getInstance() {
    if (INSTANCE == null) {
      INSTANCE = new DataCollection();
    }
    return INSTANCE;
  }

  private DataCollection() {
    query2Logs = new HashMap<>();
  }

  public QueryLog registerQuery(BackwardQuery query) {
    QueryLog queryLog = new QueryLog(query);
    query2Logs.put(query, queryLog);
    return queryLog;
  }

  @Nullable
  public QueryLog getQueryLog(BackwardQuery query) {
    if (query2Logs.containsKey(query)) {
      return this.query2Logs.get(query);
    }
    return null;
  }
}
