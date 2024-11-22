package Interpreter.DataTypes;

import Interpreter.ConsoleWrite;

public class BooleanIDT implements ConsoleWrite.InterpreterDataType {
    public boolean Value;

    public BooleanIDT(boolean value) {
        this.Value = value;
    }

    @Override
    public void Assign(ConsoleWrite.InterpreterDataType in) {
        if (in instanceof BooleanIDT inv) {
            Value = inv.Value;
        }
        else {
            throw new RuntimeException("Trying to assign to a boolean IDT from a " + in.getClass());
        }
    }

    @Override
    public String toString() {
        return Value?"true":"false";
    }
}
