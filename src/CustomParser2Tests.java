import AST.TranNode;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class CustomParser2Tests {

    private TranNode LexAndParse(String input) throws Exception {
        var l = new Lexer(input);
        var tokens = l.Lex();
//        System.out.println(tokens);
        var tran = new TranNode();
        var p = new Parser(tran, tokens);
        p.Tran();
        return tran;
    }

    @Test
    public void incorrectMethodHeadersThrowSyntaxError() throws Exception {
        var p1 = new Parser(new TranNode(),
                new Lexer(
                    "interface node\n" +
                    "\texponent(,number x) : number s,"
                ).Lex()
        );

        var p2 = new Parser(
                new TranNode(),
                new Lexer(
                    "interface node\n" +
                    "\tkill(, string person)\n"
                ).Lex()
        );

        var p3 = new Parser(
                new TranNode(),
                new Lexer(
                    "interface node\n" +
                    "\tupdateClock(number t,)\n"
                ).Lex()
        );


        Assertions.assertThrows(SyntaxErrorException.class, p1::Tran);
        Assertions.assertThrows(SyntaxErrorException.class, p2::Tran);
        Assertions.assertThrows(SyntaxErrorException.class, p3::Tran);
    }

    @Test
    public void singleFieldWithAccessorAndMutator() throws Exception {
        var l = new Lexer(
                "class intFace\n" +
                "\tnumber numFaces\n" +
                "\t\taccessor:\n" +
                "\t\t\tif\n" +
                "\t\tmutator:\n" +
                "\t\t\tloop\n" +
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
                "\t\t\t\n" +
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

    @Test
    public void privatedAndSharedMethods() throws Exception {
        var tran = LexAndParse(
                "class Trann\n" +
                "\tprivate doSomethingP()\n" +
                "\tshared doSomethingS()\n" +
                "\tprivate shared doSomethingSandP()\n"
        );

        Assertions.assertEquals(1, tran.Classes.size());
        Assertions.assertEquals("Trann", tran.Classes.getFirst().name);
        Assertions.assertEquals(3, tran.Classes.getFirst().methods.size());

        Assertions.assertEquals("doSomethingP", tran.Classes.getFirst().methods.get(0).name);
        Assertions.assertTrue(tran.Classes.getFirst().methods.get(0).isPrivate);
        Assertions.assertFalse(tran.Classes.getFirst().methods.get(0).isShared);

        Assertions.assertEquals("doSomethingS", tran.Classes.getFirst().methods.get(1).name);
        Assertions.assertFalse(tran.Classes.getFirst().methods.get(1).isPrivate);
        Assertions.assertTrue(tran.Classes.getFirst().methods.get(1).isShared);

        Assertions.assertEquals("doSomethingSandP", tran.Classes.getFirst().methods.get(2).name);
        Assertions.assertTrue(tran.Classes.getFirst().methods.get(2).isPrivate);
        Assertions.assertTrue(tran.Classes.getFirst().methods.get(2).isShared);
    }

    @Test
    public void throwErrorAtNoNewLines() throws Exception {
        var l = new Lexer(
                "class Trann\n" +
                "\tdoSomethingP()" +
                "\tdoSomethingS()" +
                "\tdoSomethingSandP()"
        );
        var tran = new TranNode();
        var p = new Parser(tran, l.Lex());
        Assertions.assertThrows(SyntaxErrorException.class, p::Tran);
    }
}
