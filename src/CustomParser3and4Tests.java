import AST.*;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.LinkedList;
import java.util.List;
import java.util.Optional;

public class CustomParser3and4Tests {

    private TranNode LexAndParse(String input) throws Exception {
        var l = new Lexer(input);
        var tokens = l.Lex();
        var tran = new TranNode();
        var p = new Parser(tran, tokens);
        p.Tran();
        return tran;
    }

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

    @Test
    public void statementsOfAllTypes_shouldParse() throws Exception {
        var t = LexAndParse(
                "class Tran\n" +
                        "\tsqrt(number x) : number s, number minusS\n" +
                        "\t\tif condition\n" +
                        "\t\tloop l = condition\n" +
                        "\t\ts =\n"
        );

        Assertions.assertEquals(1, t.Classes.size());
        var sqrt = t.Classes.getFirst().methods.getFirst();

        Assertions.assertFalse(sqrt.isPrivate);
        Assertions.assertFalse(sqrt.isShared);
        Assertions.assertEquals(1, sqrt.parameters.size());
        Assertions.assertEquals("number x", sqrt.parameters.getFirst().toString());
        Assertions.assertEquals(2, sqrt.returns.size());
        Assertions.assertEquals("number s", sqrt.returns.getFirst().toString());
        Assertions.assertEquals("number minusS", sqrt.returns.getLast().toString());
        Assertions.assertEquals(3, sqrt.statements.size());
        Assertions.assertEquals(0, sqrt.locals.size());
        Assertions.assertEquals(
                "if (condition or null)\n" +
                        "null\n\n" +
                        "loop l = condition or null\n\n" +
                        "s = Placeholder-Expression\n\n",
                Node.statementListToString(sqrt.statements)
        );
    }


//    @Test
    public void methodCallsOfAllTypes_shouldParse() throws Exception {
        var t = LexAndParse(
                "class Tran\n" +
                        "\tnumber fancyMath\n" +
                        "\t\taccessor:\n" +
                        "\t\t\tvoidMethodCall()\n" +
                        "\t\t\tvoidMethodCall(x)\n" +
                        "\t\t\tvoidMethodCall(a, b, c)\n" +
                        "\t\t\tvoidMethodCall(anotherVoidMethodCall())\n" +
                        "\t\tmutator:\n" +
                        "\t\t\ty = singleReturnCall()\n" +
                        "\t\t\ty = singleReturnCall(x)\n" +
                        "\t\t\ty = singleReturnCall(a, b, c)\n" +
                        "\t\t\tw, y, z = multiReturnCall()\n"
        );

        Assertions.assertEquals(1, t.Classes.size());
        var m = t.Classes.getFirst().members;
        Assertions.assertEquals(1, m.size());
        Assertions.assertEquals(4, m.getFirst().accessor.get().size());
        Assertions.assertEquals(4, m.getFirst().mutator.get().size());
        Assertions.assertEquals(
                "number fancyMath\n" +
                "voidMethodCall()\n" +
                "voidMethodCall(x)\n" +
                "voidMethodCall(a, b, c)\n" +
                "voidMethodCall(anotherVoidMethodCall())\n" +
                "y = singleReturnCall()\n" +
                "y = singleReturnCall(x)\n" +
                "y = singleReturnCall(a, b, c)\n" +
                "w, y, z = multiReturnCall()\n",
                m.getFirst().toString()
                );
    }

    @Test
    public void testClassWithMethodsAndMembers() throws Exception {
        var t = LexAndParse(
                "class Tran\n" +
                        "\tnumber w\n" +
                        "\tstring x\n" +
                        "\tboolean y\n" +
                        "\tcharacter z\n" +
                        "\thelloWorld()\n" +
                        "\t\tloop x\n"
                );
        Assertions.assertEquals(1, t.Classes.size());
        var m = t.Classes.getFirst().members;
        Assertions.assertEquals(4, t.Classes.getFirst().members.size()); // scramble test order to break the "duplicate code" warning
        Assertions.assertEquals("boolean", m.get(2).declaration.type);
        Assertions.assertEquals("y", m.get(2).declaration.name);
        Assertions.assertEquals("character", m.get(3).declaration.type);
        Assertions.assertEquals("z", m.get(3).declaration.name);
        Assertions.assertEquals("string", m.get(1).declaration.type);
        Assertions.assertEquals("x", m.get(1).declaration.name);
        Assertions.assertEquals("number", m.getFirst().declaration.type);
        Assertions.assertEquals("w", m.getFirst().declaration.name);

        Assertions.assertEquals(1, t.Classes.getFirst().methods.size());
        Assertions.assertEquals(1, t.Classes.getFirst().methods.getFirst().statements.size());
    }
}
