package boomerang.strategydecider;

import java.util.HashMap;
import java.util.Map;
import org.jpmml.evaluator.Evaluator;
import org.jpmml.evaluator.EvaluatorUtil;

public class DecisionCache {

  private static DecisionCache INSTANCE;
  private Map<String, Integer> cache;
  private Evaluator evaluator;

  public static DecisionCache getInstance(Evaluator evaluator) {
    if (INSTANCE == null) {
      INSTANCE = new DecisionCache(evaluator);
    }
    return INSTANCE;
  }

  private DecisionCache(Evaluator evaluator) {
    this.evaluator = evaluator;
    this.cache = new HashMap<>();
  }

  private String convert2FeatureString(Map<String, Float> features) {
    StringBuilder builder = new StringBuilder();
    builder.append(features.get("x1") + ",");
    builder.append(features.get("x2") + ",");
    builder.append(features.get("x3") + ",");
    builder.append(features.get("x4") + ",");
    builder.append(features.get("x5") + ",");
    builder.append(features.get("x6") + ",");
    builder.append(features.get("x7") + ",");
    builder.append(features.get("x8") + ",");
    builder.append(features.get("x9") + ",");
    builder.append(features.get("x10"));
    return builder.toString();
  }

  public Integer getDecision(Map<String, Float> features) {
    String key = convert2FeatureString(features);
    if (cache.containsKey(key)) {
      return cache.get(key);
    } else {
      Map<String, ?> results = EvaluatorUtil.decodeAll(evaluator.evaluate(features));
      int y = Integer.valueOf(results.get("y").toString().substring(0, 1));
      cache.put(key, y);
      return y;
    }
  }
}
