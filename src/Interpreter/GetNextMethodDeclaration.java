package Interpreter;

import AST.BuiltInMethodDeclarationNode;
import AST.VariableDeclarationNode;
import Interpreter.DataTypes.BooleanIDT;
import Interpreter.DataTypes.InterpreterDataType;
import Interpreter.DataTypes.NumberIDT;

import java.util.Arrays;
import java.util.List;

public class GetNextMethodDeclaration extends BuiltInMethodDeclarationNode {
    private final int max;
    private int currVal;

    public GetNextMethodDeclaration(int n) {
        super();
        currVal = 1;
        max = n;
        // Make method signature properly implement iterator //
        // Name
        super.name = "getNext";
        // Return types
        var bool = new VariableDeclarationNode();
        bool.type = "boolean";
        bool.name = "b";
        super.returns.add(bool);
        var element = new VariableDeclarationNode();
        element.type = "number";
        element.name = "i";
        super.returns.add(element);
    }

    /**
     * Returns two things: a boolean, and the next element in iteration.
     * @return Value1: True if the iterator is not done, if else false. <br/>
     *         Value2: The next value in the collection. Returns -1 if no elements left.
     */
    @Override
    public List<InterpreterDataType> Execute(List<InterpreterDataType> params) {
        var retVals = new InterpreterDataType[2];
        if (currVal <= max) {
            retVals[0] = new BooleanIDT(true);
            retVals[1] = new NumberIDT(currVal++);
        } else {
            retVals[0] = new BooleanIDT(false);
            retVals[1] = new NumberIDT(-1);
        }
        return Arrays.stream(retVals).toList();
    }
}
