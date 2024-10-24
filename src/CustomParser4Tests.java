import AST.TranNode;
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
}
