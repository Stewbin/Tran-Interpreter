import AST.*;

public class PrettyPrinter {
    private final TranNode tranRoot;

    public PrettyPrinter(TranNode tran) {
        tranRoot = tran;
    }

    public void Print() {
        for (ClassNode classNode : tranRoot.Classes) {
            System.out.println(classNode);
        }
    }
}
