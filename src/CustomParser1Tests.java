import AST.MethodHeaderNode;
import AST.TranNode;
import AST.VariableDeclarationNode;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

public class CustomParser1Tests {

    private TranNode LexAndParse(String input, int tokenCount) throws Exception {
        var l = new Lexer(input);
        var tokens = l.Lex();
//        tokens.forEach(System.out::println);
        Assertions.assertEquals(tokenCount, tokens.size());
        var tran = new TranNode();
//        System.out.println(tran);
        var p = new Parser(tran, tokens);
        p.Tran();
        return tran;
    }


    @Test
    public void testInterface() throws Exception {
        List<Token> tokens = new ArrayList<>();
        tokens.add(new Token(Token.TokenTypes.INTERFACE, 1, 1, "interface"));
        tokens.add(new Token(Token.TokenTypes.WORD, 1, 11, "someName"));
        tokens.add(new Token(Token.TokenTypes.NEWLINE, 1, 19));
        tokens.add(new Token(Token.TokenTypes.INDENT, 2, 1));
        tokens.add(new Token(Token.TokenTypes.WORD, 2, 2, "updateClock"));
        tokens.add(new Token(Token.TokenTypes.LPAREN, 2, 13));
        tokens.add(new Token(Token.TokenTypes.RPAREN, 2, 14));
        tokens.add(new Token(Token.TokenTypes.NEWLINE, 2, 15));
        tokens.add(new Token(Token.TokenTypes.WORD, 3, 2, "square"));
        tokens.add(new Token(Token.TokenTypes.LPAREN, 3, 8));
        tokens.add(new Token(Token.TokenTypes.RPAREN, 3, 9));
        tokens.add(new Token(Token.TokenTypes.COLON, 3, 11));
        tokens.add(new Token(Token.TokenTypes.WORD, 3, 13, "number"));
        tokens.add(new Token(Token.TokenTypes.WORD, 3, 20, "s"));
        tokens.add(new Token(Token.TokenTypes.DEDENT, 4, 23));

        //        interface someName\n
        //        \t updateClock()\n
        //        \t square() : number s

        var tran = new TranNode();
        var p = new Parser(tran, tokens);
        p.Tran();

        Assertions.assertEquals(1, tran.Interfaces.size());
        Assertions.assertEquals(2, tran.Interfaces.getFirst().methods.size());

        Assertions.assertEquals("updateClock ()\n", tran.Interfaces.getFirst().methods.get(0).toString());
        Assertions.assertEquals("square () : number s,\n", tran.Interfaces.getFirst().methods.get(1).toString());
    }

    @Test
    public void testParserConstructor() throws Exception {
        // Given an input string and expected token count
        List<Token> tokens = new ArrayList<>();
        tokens.add(new Token(Token.TokenTypes.INTERFACE, 1, 1, "interface"));
        tokens.add(new Token(Token.TokenTypes.WORD, 1, 11, "someName"));
        tokens.add(new Token(Token.TokenTypes.NEWLINE, 1, 19));
        tokens.add(new Token(Token.TokenTypes.INDENT, 2, 1));
        tokens.add(new Token(Token.TokenTypes.WORD, 2, 2, "updateClock"));
        tokens.add(new Token(Token.TokenTypes.LPAREN, 2, 13));
        tokens.add(new Token(Token.TokenTypes.RPAREN, 2, 14));
        tokens.add(new Token(Token.TokenTypes.NEWLINE, 2, 15));
        tokens.add(new Token(Token.TokenTypes.WORD, 3, 2, "square"));
        tokens.add(new Token(Token.TokenTypes.LPAREN, 3, 8));
        tokens.add(new Token(Token.TokenTypes.RPAREN, 3, 9));
        tokens.add(new Token(Token.TokenTypes.COLON, 3, 11));
        tokens.add(new Token(Token.TokenTypes.WORD, 3, 13, "number"));
        tokens.add(new Token(Token.TokenTypes.WORD, 3, 20, "s"));
        tokens.add(new Token(Token.TokenTypes.DEDENT, 4, 23));

        var tran = new TranNode();
        var p = new Parser(tran, tokens);

        // Create a TranNode
        TranNode tranNode = new TranNode();

        // Create the Parser with the TranNode and tokens
        Parser parser = new Parser(tranNode, tokens);

        // Assert that parser was successfully created
    }
}
