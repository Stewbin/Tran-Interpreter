import AST.TranNode;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class CustomParser2Tests {

    private TranNode LexAndParse(String input) throws Exception {
        var l = new Lexer(input);
        var tokens = l.Lex();
        System.out.println(tokens);
        var tran = new TranNode();
        var p = new Parser(tran, tokens);
        p.Tran();
        return tran;
    }

    @Test
    public void simpleFieldWithAccessorAndMutator() throws Exception {
        var l = new Lexer(
                "class intFace\n" +
                "\tnumber numFaces\n" +
                "\t\taccessor:\n" +
                "\t\t\tstatement\n" +
                "\t\tmutator:\n" +
                "\t\t\t{Empty mutator}"
        );

        var tokes = l.Lex();
        System.out.println(tokes);

        var tran = new TranNode();
        var p = new Parser(tran, tokes);
        p.Tran();

        var classes = tran.Classes;
        Assertions.assertEquals(1, classes.size());
        var closs = tran.Classes.getFirst();
        Assertions.assertEquals("intFace", closs.name);
        var members = closs.members;
        Assertions.assertEquals(1, members.size());
        Assertions.assertTrue(members.getFirst().accessor.isPresent());
    }

//    @Test
    public void field_WithCrazyAmountOfNewLines_BetweenAccessorAndMutator() throws Exception {
        var tran = LexAndParse(
                "class closs\n" +
                "\tnumber numFaces\n" +
                "\t\taccessor:\n" +
                "\t\t\t\n" +
                "\t\t\t\n" +
                "\t\t\t\n" +
                "\t\t\tstatement\n" +
                "\t\t\t\n" +
                "\t\t\t\n" +
                "\t\tmutator:\n" +
                "\t\t\t\n" +
                "\t\t\t\n"
        );

        var classes  = tran.Classes;
        Assertions.assertEquals(1, classes.size());
        var closs = tran.Classes.getFirst();
        Assertions.assertEquals("closs", closs.name);
        var members = closs.members;
        Assertions.assertEquals(1, members.size());
        Assertions.assertTrue(members.getFirst().accessor.isPresent());
    }

    @Test
    public void classWithOnlySingleMember() throws Exception {
        var tran = LexAndParse(
                "class MyClass\n" +
                "\tstring name"
        );

        Assertions.assertEquals(1, tran.Classes.size());
        Assertions.assertEquals("MyClass", tran.Classes.getFirst().name);
        Assertions.assertEquals(1, tran.Classes.getFirst().members.size());
    }

    @Test
    public void testClassWithMultipleMembers() throws Exception {
        var t = LexAndParse("class Tran\n" +
                            "\tnumber w\n" +
                            "\tstring x\n" +
                            "\tboolean y\n" +
                            "\tcharacter z");
        Assertions.assertEquals(1, t.Classes.size());
        Assertions.assertEquals(4, t.Classes.getFirst().members.size());
        var m = t.Classes.getFirst().members;
        Assertions.assertEquals("number", m.getFirst().declaration.type);
        Assertions.assertEquals("w", m.getFirst().declaration.name);
        Assertions.assertEquals("string", m.get(1).declaration.type);
        Assertions.assertEquals("x", m.get(1).declaration.name);
        Assertions.assertEquals("boolean", m.get(2).declaration.type);
        Assertions.assertEquals("y", m.get(2).declaration.name);
        Assertions.assertEquals("character", m.get(3).declaration.type);
        Assertions.assertEquals("z", m.get(3).declaration.name);
    }
}
