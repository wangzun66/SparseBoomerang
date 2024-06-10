package boomerang.datacollection;

import com.google.common.base.Stopwatch;

import java.time.Duration;

public class DecisionLog {

    private int decision;
    private String methodSig;
    private Stopwatch watch;

    public DecisionLog(String methodSig, int decision) {
        this.decision = decision;
        this.methodSig = methodSig;
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

    public String getMethodSig() {
        return this.methodSig;
    }

    public int getDecision() {
        return this.decision;
    }

    @Override
    public String toString() {
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append(methodSig + ", " + getDuration().toNanos());
        return stringBuilder.toString();
    }
}
