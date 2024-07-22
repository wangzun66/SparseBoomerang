package boomerang.strategydecider;

import core.fx.base.*;
import core.fx.methodbased.AllocationCount;
import core.fx.methodbased.MethodStmtCount;
import core.fx.methodbased.PropOfInvokeAssign;
import core.fx.methodbased.methodstmtbased.*;
import core.fx.methodbased.methodvarbased.MethodVarFEU;
import core.fx.methodbased.methodvarbased.PropOfRelatedInvoke;
import core.fx.methodbased.methodvarbased.RelatedTypesCount;
import core.fx.variablebased.TypeHierarchySize;
import java.util.HashMap;
import java.util.Map;
import soot.SootMethod;
import soot.Value;
import soot.jimple.Stmt;

public class FeatureExtractor {

  private static FeatureExtractor INSTANCE;

  private MethodStmtFEU<Boolean> isReturnStmt = new IsReturnStmt();
  private MethodFEU<Integer> stmtCount = new MethodStmtCount();
  private MethodVarFEU<Integer> relatedTypesCount = new RelatedTypesCount();
  private VariableFEU<Integer> typeHierarchySize = new TypeHierarchySize();
  private MethodVarFEU<Double> propOfVisitedMethodTAS = new PropOfRelatedInvoke();
  private MethodFEU<Double> maxPropOfTAS = new PropOfInvokeAssign();
  private MethodStmtFEU<Double> stmtDepth = new StmtDepth();
  private MethodFEU<Integer> allocCount = new AllocationCount();
  private MethodStmtFEU<Double> maxPropOfAAS = new PropOfRelevantStmtBeforeStmt();
  private MethodStmtFEU<Double> propOfVisitedMethodAAS = new PropOfInvokeBeforeStmt();

  public static FeatureExtractor getInstance() {
    if (INSTANCE == null) {
      INSTANCE = new FeatureExtractor();
    }
    return INSTANCE;
  }

  public Map<String, Float> extract(SootMethod method, Value value, Stmt stmt) {
    Map<String, Float> featuresMap = new HashMap<>();
    featuresMap.put(
        "x1", isReturnStmt.extract(method, stmt).getValue().booleanValue() ? 1.0f : 0.0f);
    featuresMap.put("x2", stmtCount.extract(method).getValue().floatValue());
    featuresMap.put("x3", relatedTypesCount.extract(method, value).getValue().floatValue());
    featuresMap.put("x4", typeHierarchySize.extract(value).getValue().floatValue());
    featuresMap.put("x5", propOfVisitedMethodTAS.extract(method, value).getValue().floatValue());
    featuresMap.put("x6", maxPropOfTAS.extract(method).getValue().floatValue());
    featuresMap.put("x7", stmtDepth.extract(method, stmt).getValue().floatValue());
    featuresMap.put("x8", allocCount.extract(method).getValue().floatValue());
    featuresMap.put("x9", maxPropOfAAS.extract(method, stmt).getValue().floatValue());
    featuresMap.put("x10", propOfVisitedMethodAAS.extract(method, stmt).getValue().floatValue());
    return featuresMap;
  }
}
