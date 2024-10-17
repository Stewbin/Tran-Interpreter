import AST.BooleanLiteralNode;
import AST.MethodCallStatementNode;
import AST.VariableDeclarationNode;
import AST.VariableReferenceNode;
import org.junit.jupiter.api.Test;

import java.util.LinkedList;
import java.util.List;
import java.util.Optional;

public class CustomParser3Tests {

    @Test
    public void whatDoesAMethodCallStatementNodeLookLike() {
        var methodCall = new MethodCallStatementNode();

        methodCall.methodName = "myMethod";
        methodCall.objectName = Optional.empty();
        methodCall.parameters = new LinkedList<>();

        var retRef = new VariableReferenceNode();
        retRef.name = "result";
        methodCall.returnValues = List.of(retRef);

        System.out.println(methodCall);
    }
}
