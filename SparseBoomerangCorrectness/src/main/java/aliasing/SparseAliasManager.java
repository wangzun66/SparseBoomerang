package aliasing;

import boomerang.BackwardQuery;
import boomerang.Boomerang;
import boomerang.DefaultBoomerangOptions;
import boomerang.datacollection.DataCollection;
import boomerang.datacollection.MethodLog;
import boomerang.datacollection.QueryLog;
import boomerang.results.BackwardBoomerangResults;
import boomerang.scene.*;
import boomerang.scene.jimple.*;
import boomerang.scene.sparse.SparseCFGCache;
import boomerang.util.AccessPath;
import com.google.common.base.Stopwatch;
import java.time.Duration;
import java.util.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import soot.*;
import soot.jimple.Stmt;
import wpds.impl.Weight;

public class SparseAliasManager {

  private static Logger LOGGER = LoggerFactory.getLogger(SparseAliasManager.class);

  private static SparseAliasManager INSTANCE;

  private Boomerang boomerangSolver;

  private SootCallGraph sootCallGraph;
  private DataFlowScope dataFlowScope;

  private SparseCFGCache.SparsificationStrategy sparsificationStrategy;

  private boolean ignoreAfterQuery;

  private int queryCount = 0;
  private DataCollection dataCollection;
  private Map<Integer, BackwardQuery> id2Query = new HashMap<>();
  private Map<Integer, Long> id2PDSBuildingTime = new HashMap();
  private Map<Integer, Long> id2AliasSearchingTime = new HashMap();
  private Map<Integer, Long> id2QueryTime = new HashMap();
  private static Duration totalAliasingDuration;

  static class BoomerangOptions extends DefaultBoomerangOptions {

    private SparseCFGCache.SparsificationStrategy sparsificationStrategy;
    private boolean ignoreAfterQuery;

    public BoomerangOptions(
        SparseCFGCache.SparsificationStrategy sparsificationStrategy, boolean ignoreAfterQuery) {
      this.sparsificationStrategy = sparsificationStrategy;
      this.ignoreAfterQuery = ignoreAfterQuery;
    }

    @Override
    public SparseCFGCache.SparsificationStrategy getSparsificationStrategy() {
      if (this.sparsificationStrategy == null) {
        return SparseCFGCache.SparsificationStrategy.NONE;
      }
      return this.sparsificationStrategy;
    }

    @Override
    public boolean ignoreSparsificationAfterQuery() {
      return this.ignoreAfterQuery;
    }

    @Override
    public int analysisTimeoutMS() {
      return Integer.MAX_VALUE;
    }

    @Override
    public boolean onTheFlyCallGraph() {
      return false;
    }

    @Override
    public StaticFieldStrategy getStaticFieldStrategy() {
      return StaticFieldStrategy.FLOW_SENSITIVE;
    }

    @Override
    public boolean allowMultipleQueries() {
      return true;
    }

    @Override
    public boolean throwFlows() {
      return true;
    }

    @Override
    public boolean trackAnySubclassOfThrowable() {
      return true;
    }
  }

  private SparseAliasManager(
      SparseCFGCache.SparsificationStrategy sparsificationStrategy, boolean ignoreAfterQuery) {
    this.sparsificationStrategy = sparsificationStrategy;
    this.ignoreAfterQuery = ignoreAfterQuery;
    totalAliasingDuration = Duration.ZERO;
    sootCallGraph = new SootCallGraph();
    dataFlowScope = SootDataFlowScope.make(Scene.v());
    dataCollection = DataCollection.getInstance();
  }

  public static Duration getTotalDuration() {
    return totalAliasingDuration;
  }

  public static synchronized SparseAliasManager getInstance(
      SparseCFGCache.SparsificationStrategy sparsificationStrategy, boolean ignoreAfterQuery) {
    if (INSTANCE == null
        || INSTANCE.sparsificationStrategy != sparsificationStrategy
        || INSTANCE.ignoreAfterQuery != ignoreAfterQuery) {
      INSTANCE = new SparseAliasManager(sparsificationStrategy, ignoreAfterQuery);
    }
    return INSTANCE;
  }

  /**
   * @param stmt Statement that contains the value. E.g. Value can be the leftOp
   * @param method Method that contains the Stmt
   * @param value We actually want to find this local's aliases
   * @return
   */
  public synchronized Set<AccessPath> getAliases(Stmt stmt, SootMethod method, Value value) {
    Stopwatch stopwatch = Stopwatch.createStarted();
    BackwardQuery query = createQuery(stmt, method, value);
    Set<AccessPath> aliases = getAliases(query);
    Duration elapsed = stopwatch.elapsed();
    this.id2QueryTime.put(queryCount - 1, elapsed.toNanos());
    totalAliasingDuration = totalAliasingDuration.plus(elapsed);
    //resultsPrinter();
    return aliases;
  }

  private BackwardQuery createQuery(Stmt stmt, SootMethod method, Value value) {
    JimpleMethod jimpleMethod = JimpleMethod.of(method);
    Statement statement = JimpleStatement.create(stmt, jimpleMethod);
    JimpleVal val = new JimpleVal(value, jimpleMethod);
    Optional<Statement> first =
        statement.getMethod().getControlFlowGraph().getSuccsOf(statement).stream().findFirst();
    if (first.isPresent()) {
      return BackwardQuery.make(new ControlFlowGraph.Edge(statement, first.get()), val);
    }
    throw new RuntimeException("No successors for: " + statement);
  }

  private Set<AccessPath> getAliases(BackwardQuery query) {
    QueryLog queryLog = countQuery(query);
    boomerangSolver =
        new Boomerang(
            sootCallGraph,
            dataFlowScope,
            new BoomerangOptions(INSTANCE.sparsificationStrategy, INSTANCE.ignoreAfterQuery));

    // record PDS building time
    Stopwatch stopwatch = Stopwatch.createUnstarted();
    stopwatch.start();
    BackwardBoomerangResults<Weight.NoWeight> results = boomerangSolver.solve(query);
    stopwatch.stop();
    Duration elapsed = stopwatch.elapsed();
    this.id2PDSBuildingTime.put(queryCount - 1, elapsed.toNanos());

    // record aliases searching time on PDS
    stopwatch.start();
    Set<AccessPath> aliases = results.getAllAliases();
    stopwatch.stop();
    elapsed = stopwatch.elapsed();
    this.id2AliasSearchingTime.put(queryCount - 1, elapsed.toNanos());
    if (sparsificationStrategy != SparseCFGCache.SparsificationStrategy.NONE) {
      queryLog.storeSCFGLogList(
          SparseCFGCache.getInstance(sparsificationStrategy, ignoreAfterQuery).getSCFGLogs());
      SparseCFGCache.getInstance(sparsificationStrategy, ignoreAfterQuery).resetSCFGLogs();
    }
    return aliases;
  }

  private QueryLog countQuery(BackwardQuery query) {
    this.id2Query.put(queryCount, query);
    QueryLog queryLog = dataCollection.registerQuery(queryCount, query);
    queryCount++;
    return queryLog;
  }

  private void resultsPrinter() {
    for (int i = 0; i < queryCount; i++) {
      BackwardQuery query = this.id2Query.get(i);
      LOGGER.info(query.getInfo());
      LOGGER.info(
          "QueryTime: {}  PDSBuildingTime: {}  AliasSearchingTime: {}",
          this.id2QueryTime.get(i),
          this.id2PDSBuildingTime.get(i),
          this.id2AliasSearchingTime.get(i));
      QueryLog queryLog = dataCollection.getQueryLog(i);
      for (MethodLog methodLog : queryLog.getLogList()) {
        LOGGER.info(methodLog.toString());
      }
    }
    LOGGER.info("Running Time: {}", totalAliasingDuration.toMillis());
  }
}
