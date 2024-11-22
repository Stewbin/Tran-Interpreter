package Interpreter.DataTypes;

import Interpreter.ConsoleWrite;

public class CharIDT implements ConsoleWrite.InterpreterDataType {
    public char Value;

    public CharIDT(char value) {
        Value = value;
    }

    @Override
    public void Assign(ConsoleWrite.InterpreterDataType in) {
        if (in instanceof CharIDT inv) {
            Value = inv.Value;
        } else {
            throw new RuntimeException("Trying to assign to a character IDT from a " + in.getClass());
        }
    }

    public String toString() {
        return String.valueOf(Value);
    }
}
