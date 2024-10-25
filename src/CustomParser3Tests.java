import AST.*;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.LinkedList;
import java.util.List;
import java.util.Optional;

public class CustomParser3Tests {

    private TranNode LexAndParse(String input) throws Exception {
        var l = new Lexer(input);
        var tokens = l.Lex();
        var tran = new TranNode();
        var p = new Parser(tran, tokens);
        p.Tran();
        return tran;
    }

    private void whatDoesAMethodCallStatementNodeLookLike() {
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

    @Test
    public void andOrBoolExpressions_shouldParse() throws Exception {
        var t = LexAndParse(
                "class Tran\n" +
                        "\tcheckX(string x)\n" +
                        "\t\tif boolVariable\n" +
                        "\t\tif loft and reght\n" +
                        "\t\tif elft or rwght\n" +
                        "\t\tif a > b\n" +
                        "\t\tif a < b\n" +
                        "\t\tif a == b\n" +
                        "\t\tif a <= b\n" +
                        "\t\tif a >= b\n" +
                        "\t\tif a != b\n" +
                        "\t\tif a < b and b < c\n" +
                        "\t\tif b >= a and c <= b\n" +
                        "\t\tif a == b and not c > b\n" +
                        "\t\tif not not a != c and not not c > b\n"
        );

        Assertions.assertEquals(1, t.Classes.size());
        var m = t.Classes.getFirst().methods;
        Assertions.assertEquals(1, m.size());
        Assertions.assertEquals("checkX", m.getFirst().name);
        Assertions.assertEquals(1, m.getFirst().parameters.size());
        Assertions.assertEquals("string x", m.getFirst().parameters.getFirst().toString());

        var stmnts = m.getFirst().statements;
        Assertions.assertEquals(3, stmnts.size());
        stmnts.forEach(s -> {
            Assertions.assertInstanceOf(IfNode.class, s);
            Assertions.assertNull(((IfNode) s).statements);
            Assertions.assertEquals(Optional.empty(), ((IfNode) s).elseStatement);
        });

        Assertions.assertEquals("boolVariable", ((IfNode) stmnts.get(0)).condition.toString());
        Assertions.assertEquals("loft and reght", ((IfNode) stmnts.get(1)).condition.toString());
        Assertions.assertEquals("elft or rwght", ((IfNode) stmnts.get(2)).condition.toString());
    }

    @Test
    public void simpleNotOperatorTest() throws Exception {
        var t = LexAndParse(
                "class Criminal\n" +
                        "\tmethod()\n" +
                        "\t\tif !victimDead && hasMoney\n" +
                        "\t\t\tstealMoney()\n"
        );

        Assertions.assertEquals(1, t.Classes.size());
        Assertions.assertEquals(1, t.Classes.getFirst().methods.size());
        Assertions.assertEquals(1, t.Classes.getFirst().methods.getFirst().statements.size());

        var s = (IfNode) t.Classes.getFirst().methods.getFirst().statements.getFirst();
        var cond = ((BooleanOpNode) s.condition);
        Assertions.assertEquals(BooleanOpNode.BooleanOperations.and, cond.op);
        Assertions.assertEquals("not victim dead", cond.left.toString());
        Assertions.assertEquals("hasMoney", cond.right.toString());
        Assertions.assertEquals("stealMoney()", s.statements.getFirst().toString());
    }
}
