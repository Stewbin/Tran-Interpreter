package Interpreter;

import AST.BuiltInMethodDeclarationNode;
import Interpreter.DataTypes.InterpreterDataType;
import Interpreter.DataTypes.ObjectIDT;

import java.util.HashMap;
import java.util.List;

public class CloneObjectMethod extends BuiltInMethodDeclarationNode {
    private final ObjectIDT toBeCloned;

    public CloneObjectMethod(ObjectIDT toBeCloned) {
        this.toBeCloned = toBeCloned;
    }

    /**
     * Clones the caller object
     * @return A shallow copy of the caller object
     */
    @Override
    public List<InterpreterDataType> Execute(List<InterpreterDataType> params) {
        if (!params.isEmpty())
            throw new RuntimeException("Exactly 0 arguments expected");
        var clone = new ObjectIDT(toBeCloned.astNode); // Copy over class
        clone.members.putAll(toBeCloned.members); // Copy over members
        return List.of(clone);
    }
}
