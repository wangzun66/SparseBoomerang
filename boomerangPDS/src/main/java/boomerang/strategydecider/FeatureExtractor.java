package boomerang.strategydecider;

import core.fx.base.*;
import core.fx.methodbased.AllocationCount;
import core.fx.methodbased.MethodStmtCount;
import core.fx.methodbased.ProportionOfRelevantStmts;
import core.fx.methodstmtbased.ProportionOfRelevantStmtsBeforeStmt;
import core.fx.methodstmtbased.ProportionOfVisitedMethodBeforeStmt;
import core.fx.methodstmtbased.StmtDepthProportion;
import core.fx.methodvarbased.ProportionOfVisitedMethod;
import core.fx.methodvarbased.RelatedTypesCount;
import core.fx.methodvarbased.TypeHierarchySize;
import soot.SootMethod;
import soot.Value;
import soot.jimple.Stmt;
import soot.jimple.internal.JReturnStmt;
import soot.jimple.internal.JReturnVoidStmt;

import java.util.HashMap;
import java.util.Map;

public class FeatureExtractor {

    private static FeatureExtractor INSTANCE;

    private MethodFEU<Integer> stmtCount = new MethodStmtCount();
    private MethodVarFEU<Integer> relatedTypesCount = new RelatedTypesCount();
    private MethodVarFEU<Integer> typeHierarchySize= new TypeHierarchySize();
    private MethodVarFEU<Double> propOfVisitedMethodTAS = new ProportionOfVisitedMethod();
    private MethodFEU<Double>  maxPropOfTAS = new ProportionOfRelevantStmts();
    private MethodStmtFEU<Double> stmtDepth = new StmtDepthProportion();
    private MethodFEU<Integer> allocCount = new AllocationCount();
    private MethodStmtFEU<Double> maxPropOfAAS = new ProportionOfRelevantStmtsBeforeStmt();
    private MethodStmtFEU<Double> propOfVisitedMethodAAS = new ProportionOfVisitedMethodBeforeStmt();

    public static FeatureExtractor getInstance() {
        if (INSTANCE == null) {
            INSTANCE = new FeatureExtractor();
        }
        return INSTANCE;
    }

    //todo: change key name later
    public Map<String, Float> extract(SootMethod method, Value value, Stmt stmt) {
        Map<String, Float> featuresMap = new HashMap<>();
        //HasSameReturnedType
        if(method.getReturnType()!=null && method.getReturnType().equals(value.getType())){
            featuresMap.put("x1", 1.0f);
        }else {
            featuresMap.put("x1", 0.0f);
        }
        //IsTailStmt
        if(stmt instanceof JReturnStmt || stmt instanceof JReturnVoidStmt){
            featuresMap.put("x2", 1.0f);
        }else {
            featuresMap.put("x2", 0.0f);
        }
        featuresMap.put("x3", stmtCount.extract(method).getValue().floatValue());
        featuresMap.put("x4", relatedTypesCount.extract(method, value).getValue().floatValue());
        featuresMap.put("x5", typeHierarchySize.extract(method, value).getValue().floatValue());
        featuresMap.put("x6", propOfVisitedMethodTAS.extract(method, value).getValue().floatValue());
        featuresMap.put("x7", maxPropOfTAS.extract(method).getValue().floatValue());
        featuresMap.put("x8", stmtDepth.extract(method, stmt).getValue().floatValue());
        featuresMap.put("x9", allocCount.extract(method).getValue().floatValue());
        featuresMap.put("x10", maxPropOfAAS.extract(method, stmt).getValue().floatValue());
        featuresMap.put("x11", propOfVisitedMethodAAS.extract(method, stmt).getValue().floatValue());
        return featuresMap;
    }
}
