package Interpreter;

import AST.BuiltInMethodDeclarationNode;
import AST.ClassNode;
import Interpreter.DataTypes.InterpreterDataType;
import Interpreter.DataTypes.NumberIDT;
import Interpreter.DataTypes.ObjectIDT;

import java.util.List;

public class CreateTimesIterator extends BuiltInMethodDeclarationNode {
    private final int callerValue;

    public CreateTimesIterator(NumberIDT callerNum) {
        this.callerValue = ((int) callerNum.Value);
    }

    /**
     * x.times() creates an `iterator` object that iterates over the numbers 0 to x.
     * @return An `iterator` that returns the integers from 0 up to the caller's Value
     */
    @Override
    public List<InterpreterDataType> Execute(List<InterpreterDataType> params) {
        // Validate passed-in parameters
        if (params.isEmpty())
            throw new RuntimeException("Exactly 1 argument expected");

        // Create an object implementing `iterator`
        var iteratorClass = new ClassNode();
        iteratorClass.interfaces.add("iterator");
        iteratorClass.methods.add(new GetNext(callerValue));
        var interator = new ObjectIDT(iteratorClass);
        return List.of(interator);
    }
}
