package Interpreter;

import AST.BuiltInMethodDeclarationNode;
import AST.ClassNode;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Optional;

public class ConsoleWrite extends BuiltInMethodDeclarationNode {
    public LinkedList<String> console = new LinkedList<>();
    @Override
    public List<InterpreterDataType> Execute(List<InterpreterDataType> params) {
        StringBuilder sb = new StringBuilder();
        for (var i : params) {
            sb.append(i.toString());
            System.out.print(i.toString());
        }
        System.out.println();
        console.add(sb.toString());
        return List.of();
    }

    public static interface InterpreterDataType {
        public void Assign(InterpreterDataType in);
    }

    public static class NumberIDT implements InterpreterDataType {
        public float Value;

        public NumberIDT(float v) {
            Value = v;
        }

        @Override
        public void Assign(InterpreterDataType in) {
            if (in instanceof NumberIDT inv) {
                Value = inv.Value;
            } else {
                throw new RuntimeException("Trying to assign to a number IDT from a " + in.getClass());
            }
        }

        public String toString() {
            return String.valueOf(Value);
        }
    }

    public static class ObjectIDT implements InterpreterDataType {
        public final HashMap<String, InterpreterDataType> members = new HashMap<>();
        public final ClassNode astNode;

        public ObjectIDT(ClassNode astNode) {
            this.astNode = astNode;
        }

        @Override
        public void Assign(InterpreterDataType in) {
                throw new RuntimeException("Trying to assign to an object IDT from a " + in.getClass());
        }

        @Override
        public String toString() {
            StringBuilder out = new StringBuilder();
            for (var m : members.entrySet())
                out.append(m.getKey()).append(" : ").append(m.getValue().toString()).append("\n");
            return out.toString();
        }
    }

    public static class ReferenceIDT implements InterpreterDataType {
        public Optional<ObjectIDT> refersTo;
        @Override
        public void Assign(InterpreterDataType in) {
            if (in instanceof ReferenceIDT inv) {
                refersTo = inv.refersTo;
            } else if (in instanceof ObjectIDT obj) {
                refersTo = Optional.of(obj);
            } else {
                throw new RuntimeException("Trying to assign to a reference IDT from a " + in.getClass());
            }
        }

        @Override
        public String toString() {
            if (refersTo.isPresent()) {
                return refersTo.get().toString();
            }
            return "<<<NULL REFERENCE>>>";
        }

    }

    public static class StringIDT implements InterpreterDataType {
        public String Value;

        public StringIDT(String s) {
            Value = s;
        }

        @Override
        public void Assign(InterpreterDataType in) {
            if (in instanceof StringIDT inv) {
                Value = inv.Value;
            } else {
                throw new RuntimeException("Trying to assign to a string IDT from a " + in.getClass());
            }
        }

        @Override
        public String toString() {
            return Value;
        }
    }
}

