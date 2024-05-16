package boomerang.datacollection;

import com.google.common.base.Stopwatch;
import java.time.Duration;
import soot.SootMethod;

/** Collect data for each method */
public class MethodLog {

  private SootMethod method;
  private Stopwatch watch;

  public MethodLog(SootMethod method) {
    this.method = method;
    this.watch = Stopwatch.createUnstarted();
  }

  public void logStart() {
    this.watch.start();
  }

  public void logEnd() {
    this.watch.stop();
  }

  public Duration getDuration() {
    return this.watch.elapsed();
  }

  public SootMethod getMethod() {
    return this.method;
  }

  @Override
  public String toString() {
    StringBuilder stringBuilder = new StringBuilder();
    stringBuilder.append(method.getSignature() + ", " + getDuration().toNanos());
    return stringBuilder.toString();
  }
}
