package Parser;

import AST.*;
import Lexer.Lexer;
import Lexer.Token;
import Lexer.SyntaxErrorException;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.LinkedList;
import java.util.List;

public class Parser2Tests {

    // Test 1: Class Parsing
    @Test
    public void testClassParsing() throws Exception {
        var tran = new TranNode();
        List list = List.of(
                new Token(Token.TokenTypes.CLASS, 1, 1),
                new Token(Token.TokenTypes.WORD, 1, 2, "Tran"),
                new Token(Token.TokenTypes.NEWLINE, 1, 3),
                new Token(Token.TokenTypes.INDENT, 2, 1),
                new Token(Token.TokenTypes.WORD, 2, 2, "number"),
                new Token(Token.TokenTypes.WORD, 2, 3, "x"),
                new Token(Token.TokenTypes.NEWLINE, 2, 4),
                new Token(Token.TokenTypes.WORD, 4, 2, "string"),
                new Token(Token.TokenTypes.WORD, 4, 3, "y"),
                new Token(Token.TokenTypes.DEDENT, 5, 1)
        );
        var tokens = new LinkedList<>(list);//converting list to linked list so the token manager can handle this
        var p = new Parser(tran, tokens);
        p.Tran();
        Assertions.assertEquals(1, tran.Classes.size());
        Assertions.assertEquals("Tran", tran.Classes.getFirst().name);

    }

    // Test 2: Class with Implements
    /*
The below code is in the token list for this test:
interface someName
    square() : number s
interface someNameTwo
    squareTwo() : number STwo
class TranExample implements someName,someNameTwo
    number x
    string y
     */
    @Test
    public void testClassImplements() throws Exception {
        var tran = new TranNode();
        List list = List.of(
                new Token(Token.TokenTypes.INTERFACE, 1, 1),
                new Token(Token.TokenTypes.WORD, 1, 1, "someName"),
                new Token(Token.TokenTypes.NEWLINE, 1, 3),
                new Token(Token.TokenTypes.INDENT, 2, 1),
                new Token(Token.TokenTypes.WORD, 2, 2, "square"),
                new Token(Token.TokenTypes.LPAREN, 2, 3),
                new Token(Token.TokenTypes.RPAREN, 2, 4),
                new Token(Token.TokenTypes.COLON, 2, 5),
                new Token(Token.TokenTypes.WORD, 2, 6, "number"),
                new Token(Token.TokenTypes.WORD, 2, 7, "s"),
                new Token(Token.TokenTypes.NEWLINE, 2, 8),
                new Token(Token.TokenTypes.DEDENT, 3, 1),
                new Token(Token.TokenTypes.NEWLINE, 4, 2),

                new Token(Token.TokenTypes.INTERFACE, 5, 1),
                new Token(Token.TokenTypes.WORD, 5, 1, "someNameTwo"),
                new Token(Token.TokenTypes.NEWLINE, 5, 3),
                new Token(Token.TokenTypes.INDENT, 6, 1),
                new Token(Token.TokenTypes.WORD, 6, 2, "squareTwo"),
                new Token(Token.TokenTypes.LPAREN, 6, 3),
                new Token(Token.TokenTypes.RPAREN, 6, 4),
                new Token(Token.TokenTypes.COLON, 6, 5),
                new Token(Token.TokenTypes.WORD, 6, 6, "number"),
                new Token(Token.TokenTypes.WORD, 6, 7, "STwo"),
                new Token(Token.TokenTypes.NEWLINE, 6, 8),
                new Token(Token.TokenTypes.DEDENT, 7, 1),
                new Token(Token.TokenTypes.NEWLINE, 8, 2),


                new Token(Token.TokenTypes.CLASS, 9, 1),
                new Token(Token.TokenTypes.WORD, 9, 2, "Tran"),
                new Token(Token.TokenTypes.IMPLEMENTS, 9, 3),
                new Token(Token.TokenTypes.WORD, 9, 4, "someName"),
                new Token(Token.TokenTypes.COMMA, 9, 9),
                new Token(Token.TokenTypes.WORD, 9, 4, "someNameTwo"),
                new Token(Token.TokenTypes.NEWLINE, 9, 3),
                new Token(Token.TokenTypes.INDENT, 10, 1),
                new Token(Token.TokenTypes.WORD, 10, 2, "number"),
                new Token(Token.TokenTypes.WORD, 10, 3, "x"),
                new Token(Token.TokenTypes.NEWLINE, 10, 4),
                new Token(Token.TokenTypes.WORD, 11, 2, "string"),
                new Token(Token.TokenTypes.WORD, 11, 3, "y"),
                new Token(Token.TokenTypes.DEDENT, 11, 1)
        );
        var tokens = new LinkedList<>(list);//converting list to linked list so the token manager can handle this
        var p = new Parser(tran, tokens);
        p.Tran();
        var clazz = tran.Classes.getFirst();
        Assertions.assertEquals("Tran", clazz.name);
        Assertions.assertEquals(2, clazz.interfaces.size());
        Assertions.assertEquals("someName", clazz.interfaces.getFirst());
        Assertions.assertEquals("someNameTwo", clazz.interfaces.get(1));
        Assertions.assertEquals(2, tran.Interfaces.size());


    }
    // Test 3: Constructor Parsing
    /*
class Tran
    number x
    string y
    construct()
     */
    @Test
    public void testConstructorParsing() throws Exception {
        var tran = new TranNode();
        var list = List.of(
                new Token(Token.TokenTypes.CLASS, 1, 1),
                new Token(Token.TokenTypes.WORD, 1, 2, "Tran"),
                new Token(Token.TokenTypes.NEWLINE, 1, 3),
                new Token(Token.TokenTypes.INDENT, 2, 1),
                new Token(Token.TokenTypes.WORD, 2, 2, "number"),
                new Token(Token.TokenTypes.WORD, 2, 3, "x"),
                new Token(Token.TokenTypes.NEWLINE, 2, 4),
                new Token(Token.TokenTypes.WORD, 4, 2, "string"),
                new Token(Token.TokenTypes.WORD, 4, 3, "y"),
                new Token(Token.TokenTypes.NEWLINE, 4, 3),
                new Token(Token.TokenTypes.CONSTRUCT, 5, 2),
                new Token(Token.TokenTypes.LPAREN, 5, 3),
                new Token(Token.TokenTypes.RPAREN, 5, 4),
                new Token(Token.TokenTypes.NEWLINE, 5, 5),
                new Token(Token.TokenTypes.INDENT, 2, 1),
                new Token(Token.TokenTypes.DEDENT, 2, 1),

                new Token(Token.TokenTypes.DEDENT, 8, 1)

        );
        var tokens = new LinkedList<>(list);//converting list to linked list so the token manager can handle this
        var p = new Parser(tran, tokens);
        p.Tran();
        Assertions.assertEquals(1, tran.Classes.getFirst().constructors.size());
        Assertions.assertEquals(0, tran.Classes.getFirst().constructors.getFirst().statements.size());//

    }


    // Test 4: Class with members
    /*
The below code is in the token list for this test:
interface someName
    square() : number s
class TranExample implements someName
    number m
    string str
    start()
        number x
        number y
     */
    @Test
    public void testMembers_and_methoddeclaration() throws Exception {
        var tran = new TranNode();
        //Ignore the line and column number here, all you will be using the line number and columnNumber in parser is for printing syntax error in Tran code lexed by you.
        List<Token> list = List.of(
                new Token(Token.TokenTypes.INTERFACE, 1, 9),
                new Token(Token.TokenTypes.WORD, 1, 18, "someName"),
                new Token(Token.TokenTypes.NEWLINE, 2, 0),
                new Token(Token.TokenTypes.INDENT, 2, 4),
                new Token(Token.TokenTypes.WORD, 2, 10, "square"),
                new Token(Token.TokenTypes.LPAREN, 2, 11),
                new Token(Token.TokenTypes.RPAREN, 2, 12),
                new Token(Token.TokenTypes.COLON, 2, 14),
                new Token(Token.TokenTypes.WORD, 2, 21, "number"),
                new Token(Token.TokenTypes.WORD, 2, 23, "s"),
                new Token(Token.TokenTypes.NEWLINE, 3, 0),
                new Token(Token.TokenTypes.DEDENT, 3, 0),
                new Token(Token.TokenTypes.CLASS, 3, 5),
                new Token(Token.TokenTypes.WORD, 3, 17, "TranExample"),
                new Token(Token.TokenTypes.IMPLEMENTS, 3, 28),
                new Token(Token.TokenTypes.WORD, 3, 37, "someName"),
                new Token(Token.TokenTypes.NEWLINE, 4, 0),
                new Token(Token.TokenTypes.INDENT, 4, 4),
                new Token(Token.TokenTypes.WORD, 4, 10, "number"),
                new Token(Token.TokenTypes.WORD, 4, 12, "m"),
                new Token(Token.TokenTypes.NEWLINE, 5, 0),
                new Token(Token.TokenTypes.WORD, 4, 10, "string"),
                new Token(Token.TokenTypes.WORD, 4, 12, "str"),
                new Token(Token.TokenTypes.NEWLINE, 5, 0),
                new Token(Token.TokenTypes.WORD, 5, 9, "start"),
                new Token(Token.TokenTypes.LPAREN, 5, 10),
                new Token(Token.TokenTypes.RPAREN, 5, 11),
                new Token(Token.TokenTypes.NEWLINE, 6, 0),
                new Token(Token.TokenTypes.INDENT, 6, 8),
                new Token(Token.TokenTypes.WORD, 6, 14, "number"),
                new Token(Token.TokenTypes.WORD, 6, 16, "x"),
                new Token(Token.TokenTypes.NEWLINE, 7, 0),
                new Token(Token.TokenTypes.WORD, 7, 14, "number"),
                new Token(Token.TokenTypes.WORD, 7, 16, "y"),
                new Token(Token.TokenTypes.NEWLINE, 8, 0),
                new Token(Token.TokenTypes.DEDENT, 8, 4),
                new Token(Token.TokenTypes.DEDENT, 8, 4)

        );

           /*
        Lexer.Lexer L= new Lexer.Lexer("interface someName\n" +
                "    square() : number s\n" +
                "class TranExample implements someName\n" +
                "    number m\n"+
                "    start()\n" +
                "        number x\n" +
                "        number y\n" );
        var LT= L.Lex();
         System.out.println(LT);
        */

        var tokens = new LinkedList<>(list);//converting list to linked list so the token manager can handle this
        tokens.forEach(System.out::println);
        var p = new Parser(tran, tokens);
        p.Tran();
        var clazz = tran.Classes.getFirst();
        Assertions.assertEquals("s", tran.Interfaces.get(0).methods.getFirst().returns.get(0).name);
        Assertions.assertEquals("someName", clazz.interfaces.getFirst());
        Assertions.assertEquals(2, tran.Classes.getFirst().members.size());
        Assertions.assertEquals("m", tran.Classes.getFirst().members.getFirst().declaration.name);
        Assertions.assertEquals(2, tran.Classes.getFirst().methods.getFirst().locals.size());
        Assertions.assertEquals("x", tran.Classes.getFirst().methods.getFirst().locals.get(0).name);
        Assertions.assertEquals("y", tran.Classes.getFirst().methods.getFirst().locals.get(1).name);
    }
    @Test
    public void testAccessors() throws Exception {
        var tran = new TranNode();
        //Ignore the line and column number here, all you will be using the line number and columnNumber in parser is for printing syntax error in Tran code lexed by you.
        Lexer L= new Lexer(
                "interface someName\n" +
                "    square() : number s\n" +
                "class TranExample implements someName\n" +
                "\tnumber m\n" +
                "\t\taccessor:"
        );
        var LT= L.Lex();
        System.out.println(LT);

        var tokens = new LinkedList<>(LT);//converting list to linked list so the token manager can handle this
        var p = new Parser(tran, LT);
        p.Tran();
        var clazz = tran.Classes.getFirst();
    }
    @Test
    public void testmutator() throws Exception {
        var tran = new TranNode();
        //Ignore the line and column number here, all you will be using the line number and columnNumber in parser is for printing syntax error in Tran code lexed by you.
        Lexer L= new Lexer(
                "interface someName\n" +
                "    square() : number s\n" +
                "class TranExample implements someName\n" +
                "\tnumber m\n" +
                "\t\tmutator:"
        );
        var LT= L.Lex();
        System.out.println(LT);


        var tokens = new LinkedList<>(LT);//converting list to linked list so the token manager can handle this
        var p = new Parser(tran, LT);
        p.Tran();
        var clazz = tran.Classes.getFirst();
    }

    // Decommissioned
//    @Test
    public void testLoop() throws Exception {
        Lexer L = new Lexer("class Tran\n" +
                        "\thelloWorld()\n" +
                        "\t\tloop\n" );
        var rev= L.Lex();
        TranNode t= new TranNode();
        Parser p= new Parser(t,rev);
        p.Tran();
    }

    // Decommissioned
//    @Test
    public void testClassIf() throws Exception {
        Lexer L = new Lexer(
                "class Tran\n" +
                "\thelloWorld()\n" +
                "\t\tif\n" );
        var rev= L.Lex();
        TranNode t= new TranNode();
        Parser p= new Parser(t,rev);
        p.Tran();
    }

    private TranNode LexAndParse(String input) throws Exception {
        var l = new Lexer(input);
        var tokens = l.Lex();
//        System.out.println(tokens);
        var tran = new TranNode();
        var p = new Parser(tran, tokens);
        p.Tran();
        return tran;
    }

    // Decommissioned
//    @Test
    public void testConstructorsOfAllSignatures() throws Exception {
        var tran = LexAndParse(
                "class Student\n" +
                        "    string name\n" +
                        "    number powerLevel\n" +
                        "    construct()\n" +
                        "        string n\n" +
                        "        number p\n" +
                        "        if\n" +
                        "\t\n" +
                        "    construct(string n)\n" +
                        "        number p\n" +
                        "        loop\n" +
                        "\t\n" +
                        "    construct(string n, number p)\n" +
                        "        if\n" +
                        "        loop\n" +
                        "\t\n" +
                        "    construct(character a, boolean b, string c, number d)\n" +
                        "\t\n"
        );

        // Classes
        Assertions.assertEquals(1, tran.Classes.size());
        Assertions.assertEquals("Student", tran.Classes.getFirst().name);
        // Fields
        var members = tran.Classes.getFirst().members;
        Assertions.assertEquals(2, members.size());
        Assertions.assertEquals("string name\n", members.getFirst().toString());
        Assertions.assertEquals("number powerLevel\n", members.getLast().toString());
        // Constructors
        var constructors = tran.Classes.getFirst().constructors;
        Assertions.assertEquals(4, constructors.size());
        // 1st constructor
        Assertions.assertEquals(0, constructors.get(0).parameters.size());
        Assertions.assertEquals(2, constructors.get(0).locals.size());
        Assertions.assertEquals("string n", constructors.get(0).locals.get(0).toString());
        Assertions.assertEquals("number p", constructors.get(0).locals.get(1).toString());
        Assertions.assertEquals(1, constructors.get(0).statements.size());
        Assertions.assertInstanceOf(IfNode.class, constructors.get(0).statements.getFirst());
        // 2nd constructor
        Assertions.assertEquals(1, constructors.get(1).parameters.size());
        Assertions.assertEquals("string n", constructors.get(1).parameters.getFirst().toString());
        Assertions.assertEquals(1, constructors.get(1).locals.size());
        Assertions.assertEquals("number p", constructors.get(1).locals.getFirst().toString());
        Assertions.assertEquals(1, constructors.get(1).statements.size());
        Assertions.assertInstanceOf(LoopNode.class, constructors.get(1).statements.getFirst());
        // 3rd constructor
        Assertions.assertEquals(2, constructors.get(2).parameters.size());
        Assertions.assertEquals("string n", constructors.get(2).parameters.get(0).toString());
        Assertions.assertEquals("number p", constructors.get(2).parameters.get(1).toString());
        Assertions.assertEquals(0, constructors.get(2).locals.size());
        Assertions.assertEquals(2, constructors.get(2).statements.size());
        Assertions.assertInstanceOf(IfNode.class, constructors.get(2).statements.getFirst());
        Assertions.assertInstanceOf(LoopNode.class, constructors.get(2).statements.getLast());
        // 4th constructor
        Assertions.assertEquals(4, constructors.get(3).parameters.size());
        Assertions.assertEquals("character a", constructors.get(3).parameters.getFirst().toString());
        Assertions.assertEquals("boolean b", constructors.get(3).parameters.get(1).toString());
        Assertions.assertEquals("string c", constructors.get(3).parameters.get(2).toString());
        Assertions.assertEquals("number d", constructors.get(3).parameters.get(3).toString());
        Assertions.assertEquals(0, constructors.get(3).locals.size());
        Assertions.assertEquals(0, constructors.get(3).statements.size());
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

    // Decommissioned-- made for a younger parser
//    @Test
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
//        System.out.println(tokes);

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