import AST.*;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class CustomParser4Tests {

    private TranNode LexAndParse(String input) throws Exception {
        var l = new Lexer(input);
        var tokens = l.Lex();
        var tran = new TranNode();
        var p = new Parser(tran, tokens);
        p.Tran();
        return tran;
    }

    @Test
    public void methodCallsOfAllTypes_shouldParse() throws Exception {
        var t = LexAndParse(
                "class Tran\n" +
                        "\tstring lotsOfMethods\n" +
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
        Assertions.assertEquals("string lotsOfMethods", m.getFirst().declaration.toString());

        var acc = m.getFirst().accessor;
        var accMethods = new String[] {
                "voidMethodCall ()",
                "voidMethodCall (x,)",
                "voidMethodCall (a,b,c,)",
                "voidMethodCall (anotherVoidMethodCall (),)"
        };
        Assertions.assertTrue(acc.isPresent());
        Assertions.assertEquals(4, acc.get().size());
        for (int i = 0; i < acc.get().size(); i++) {
            Assertions.assertEquals(accMethods[i], acc.get().get(i).toString());
        }

        var mut = m.getFirst().mutator;
        Assertions.assertTrue(mut.isPresent());
        Assertions.assertEquals(4, mut.get().size());
        var mutMethods = new String[] {
                "y = singleReturnCall ()\n",
                "y = singleReturnCall (x,)\n",
                "y = singleReturnCall (a,b,c,)\n",
                "w,y,z, = multiReturnCall ()",
        };
        for (int i = 0; i < acc.get().size(); i++) {
            Assertions.assertEquals(mutMethods[i], mut.get().get(i).toString());
        }
    }

    @Test
    public void testAllMathExpressions() throws Exception {
        var t = LexAndParse(
                "class Tran\n" +
                        "\tprivate shared doLotsOfMath()\n" +
                        "\t\ta = 1 + 1\n" +
                        "\t\tb = 1 - 1\n" +
                        "\t\tc = 7 * 100\n" +
                        "\t\td = 99 / 1039\n" +
                        "\t\te = 235 % 1293\n" +
                        "\t\tf = a + b\n" +
                        "\t\tg = c % 89\n" +
                        "\t\th = 78 - 2 * 39\n" +
                        "\t\ti = 1 + 2 - 3 * 4 / 8 % 9\n"
        );

        Assertions.assertEquals(1, t.Classes.getFirst().methods.size());
        var m = t.Classes.getFirst().methods.getFirst();
        Assertions.assertTrue(m.isShared);
        Assertions.assertTrue(m.isPrivate);
        Assertions.assertEquals("doLotsOfMath", m.name);

        var stmntStrs = new String[] {
                "a = 1.0  + 1.0 \n",
                "b = 1.0  - 1.0 \n",
                "c = 7.0  * 100.0 \n",
                "d = 99.0  / 1039.0 \n",
                "e = 235.0  % 1293.0 \n",
                "f = a + b\n",
                "g = c % 89.0 \n",
                "h = 78.0  - 2.0  * 39.0 \n",
                "i = 1.0  + 2.0  - 3.0  * 4.0  / 8.0  % 9.0 \n",
        };
        Assertions.assertEquals(stmntStrs.length, m.statements.size());
        for (int i = 0; i < stmntStrs.length; i++) {
            Assertions.assertInstanceOf(AssignmentNode.class, m.statements.get(i));
            Assertions.assertEquals(stmntStrs[i], m.statements.get(i).toString());
        }
        var stmntVals = new float[] {
                2,
                0,
                700,
                99 / 1039f,
                235 % 1293,
                0, // 78 - 2 * 39
                1 + 2 - 3 * 4 / 8f % 9
        };
        Assertions.assertEquals(stmntVals[0], evaluate(((AssignmentNode) m.statements.get(0)).expression)); // a
        Assertions.assertEquals(stmntVals[1], evaluate(((AssignmentNode) m.statements.get(1)).expression)); // b
        Assertions.assertEquals(stmntVals[2], evaluate(((AssignmentNode) m.statements.get(2)).expression)); // c
        Assertions.assertEquals(stmntVals[3], evaluate(((AssignmentNode) m.statements.get(3)).expression)); // d
        Assertions.assertEquals(stmntVals[4], evaluate(((AssignmentNode) m.statements.get(4)).expression)); // e
        Assertions.assertEquals(stmntVals[5], evaluate(((AssignmentNode) m.statements.get(7)).expression)); // h
        Assertions.assertEquals(stmntVals[6], evaluate(((AssignmentNode) m.statements.get(8)).expression)); // i
    }

    private float evaluate(ExpressionNode exp) {
        if (exp instanceof NumericLiteralNode number) // Base case
            return number.value;
        else if (exp instanceof MathOpNode mathOpNode) {
            return switch (mathOpNode.op) {
                case add:
                    yield evaluate(mathOpNode.left) + evaluate(mathOpNode.right);
                case subtract:
                    yield evaluate(mathOpNode.left) - evaluate(mathOpNode.right);
                case multiply:
                    yield evaluate(mathOpNode.left) * evaluate(mathOpNode.right);
                case divide:
                    yield evaluate(mathOpNode.left) / evaluate(mathOpNode.right);
                case modulo:
                    yield evaluate(mathOpNode.left) % evaluate(mathOpNode.right);
            };
        } else {
            System.out.println("Expression was: " + exp.toString());
            throw new AssertionError("Argument was not instance of MathOpNode of NumericLiteralNode");
        }
    }

    @Test
    public void testObjectInstantiation() throws Exception {
        var t = LexAndParse(
                "class Tran\n" +
                        "\tmethod()\n" +
                        "\t\tMyClass myFirstLocal\n" +
                        "\t\tMyClass mySSecondLocal\n" +
                        "\t\tMyClass myThirdLocal\n" +
                        "\t\tmyFirstLocal = new MyClass()\n" +
                        "\t\tmySecondLocal = new MyClass(argc, argv)\n" +
                        "\t\tmyThirdLocal = new MyClass(new MyClass())\n"
        );

        Assertions.assertEquals(1, t.Classes.getFirst().methods.size());
        var m = t.Classes.getFirst().methods.getFirst();
        Assertions.assertEquals("method", m.name);
        Assertions.assertFalse(m.isPrivate);
        Assertions.assertFalse(m.isShared);
        Assertions.assertEquals(3, m.locals.size());
        Assertions.assertEquals("MyClass myFirstLocal", m.locals.get(0).toString());
        Assertions.assertEquals("MyClass mySSecondLocal", m.locals.get(1).toString());
        Assertions.assertEquals("MyClass myThirdLocal", m.locals.get(2).toString());
        Assertions.assertEquals(3, m.statements.size());
        m.statements.forEach(stmnt -> Assertions.assertInstanceOf(AssignmentNode.class, stmnt));
    }

    @Test
    public void testStringsAndCharacters() throws Exception {
        var t = LexAndParse(
                "class Tran\n" +
                        "\tstring name\n" +
                        "\t\taccessor:\n" +
                        "\t\t\tvalue = \"A name!\"\n" +
                        "\tcharacter initial\n" +
                        "\t\tmutator : \n" +
                        "\t\t\tinitial = 'A'\n"
        );

        Assertions.assertEquals(2, t.Classes.getFirst().members.size());
        var name = t.Classes.getFirst().members.getFirst();
        Assertions.assertEquals("string name", name.declaration.toString());
        Assertions.assertTrue(name.mutator.isEmpty());
        Assertions.assertTrue(name.accessor.isPresent());
        Assertions.assertEquals(1, name.accessor.get().size());
        Assertions.assertInstanceOf(AssignmentNode.class, name.accessor.get().getFirst());
        var accStmnts = name.accessor.get();
        Assertions.assertInstanceOf(StringLiteralNode.class, ((AssignmentNode) accStmnts.getFirst()).expression);
        Assertions.assertEquals("A name!", ((StringLiteralNode) ((AssignmentNode) accStmnts.getFirst()).expression).value);

        var initial = t.Classes.getFirst().members.get(1);
        Assertions.assertEquals("character initial", initial.declaration.toString());
        Assertions.assertTrue(initial.accessor.isEmpty());
        Assertions.assertTrue(initial.mutator.isPresent());
        Assertions.assertEquals(1, initial.mutator.get().size());
        Assertions.assertInstanceOf(AssignmentNode.class, initial.mutator.get().getFirst());
        Assertions.assertInstanceOf(CharLiteralNode.class, ((AssignmentNode) initial.mutator.get().getFirst()).expression);
        Assertions.assertEquals('A', ((CharLiteralNode) ((AssignmentNode) initial.mutator.get().getFirst()).expression).value);
    }
}
