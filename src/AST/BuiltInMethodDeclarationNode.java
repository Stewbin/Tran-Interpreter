package AST;

import Interpreter.ConsoleWrite;

import java.util.List;

public abstract class BuiltInMethodDeclarationNode extends MethodDeclarationNode {
    public boolean isVariadic = false;
    public abstract List<ConsoleWrite.InterpreterDataType> Execute(List<ConsoleWrite.InterpreterDataType> params);
    @Override
    public String toString() {
        return "Built-in method, isVariadic = " + isVariadic + super.toString();
    }
}
