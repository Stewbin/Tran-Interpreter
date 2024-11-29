package Interpreter;

import AST.BuiltInMethodDeclarationNode;
import AST.ClassNode;
import Interpreter.DataTypes.InterpreterDataType;
import Interpreter.DataTypes.NumberIDT;
import Interpreter.DataTypes.ObjectIDT;

import java.util.List;

public class CreateInteratorMethodDeclaration extends BuiltInMethodDeclarationNode {
    private final int callerValue;

    public CreateInteratorMethodDeclaration(NumberIDT callingNum) {
        this.callerValue = ((int) callingNum.Value);
    }

    /**
     * x.times() creates an `iterator` object that iterates over the numbers 1 to x.
     * @return An `iterator` that returns the integers from 0 up to the caller's Value
     */
    @Override
    public List<InterpreterDataType> Execute(List<InterpreterDataType> params) {
        // Validate passed-in parameters
        if (!params.isEmpty())
            throw new RuntimeException("Expected 0 argument of type <Number> but received " + params);

        // Create an object implementing <iterator>
        var interatorClass = new ClassNode();
        interatorClass.name = "Interator";
        interatorClass.interfaces.add("iterator");
        interatorClass.methods.add(new GetNextMethodDeclaration(callerValue));
        var interator = new ObjectIDT(interatorClass);
        return List.of(interator);
    }
}
