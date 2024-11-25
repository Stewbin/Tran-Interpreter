package Interpreter;

import AST.BuiltInMethodDeclarationNode;
import AST.ClassNode;
import Interpreter.DataTypes.InterpreterDataType;
import Interpreter.DataTypes.ObjectIDT;
import Interpreter.DataTypes.StringIDT;

import java.util.List;

public class GetDateMethod extends BuiltInMethodDeclarationNode {
    @Override
    public List<InterpreterDataType> Execute(List<InterpreterDataType> params) {
        if (!params.isEmpty())
            throw new RuntimeException("Exactly 0 arguments expected");
        // Create <datetime> object
        var dateTime = new ClassNode();
        dateTime.name = "datetime";
        var currentDate = new ObjectIDT(dateTime);
        currentDate.members.put("Day", new StringIDT("How am I supposed to know?"));
        currentDate.members.put("Month", new StringIDT("You're asking too much of a undergrad!"));
        currentDate.members.put("Year", new StringIDT("How long has it been since the last power up?! A thousand years? A million?!"));
        return List.of(currentDate);
    }
}
