package boomerang.scene.sparse;

public class EmptySparseCFG implements SparseCFG {

  String methodSig;

  public EmptySparseCFG(String methodSig) {
    this.methodSig = methodSig;
  }

  public String getMethodSig() {
    return this.methodSig;
  }
}
