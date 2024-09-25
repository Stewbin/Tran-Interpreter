import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.List;

public class CustomLexer2Tests {

    @Test
    public void simpleQuotedCharacter() throws Exception {
        var l = new Lexer("'a'");
        var res = l.Lex();

        Assertions.assertEquals(1, res.size());
        Assertions.assertEquals(Token.TokenTypes.QUOTEDCHARACTER, res.getFirst().getType());
        Assertions.assertEquals("a", res.getFirst().getValue());
    }

    @Test
    public void quoteAndWhiteSpaceTest() throws Exception {
        var l = new Lexer(" \"abc\"");
        var res = l.Lex();
        Assertions.assertEquals(1, res.size());
        Assertions.assertEquals(Token.TokenTypes.QUOTEDSTRING, res.getFirst().getType());
        Assertions.assertEquals("abc", res.getFirst().getValue());
    }

    @Test
    public void throwExceptionAtUnclosedString() throws Exception {
        var l = new Lexer("\"hello world");
        Assertions.assertThrows(SyntaxErrorException.class, l::Lex);
    }

    @Test
    public void simpleIndentTest() throws Exception {
        var l1 = new Lexer("\s\s\s\s");
        var res1 = l1.Lex();
        var l2 = new Lexer("\t");
        var res2 = l2.Lex();

        Assertions.assertEquals(1, res1.size());
        Assertions.assertEquals(Token.TokenTypes.INDENT, res1.getFirst().getType());

        Assertions.assertEquals(1, res2.size());
        Assertions.assertEquals(Token.TokenTypes.INDENT, res2.getFirst().getType());
    }

    @Test
    public void simpleFalseIndentTest() throws Exception {
        var l = new Lexer("\s\s\s");
        var res = l.Lex();
        Assertions.assertEquals(0, res.size());
    }

    @Test
    public void harderFalseIndentTest() throws Exception {
        var l = new Lexer("\s\sm");
        var res = l.Lex();
        Assertions.assertEquals(1, res.size());
        Assertions.assertEquals(Token.TokenTypes.WORD, res.get(0).getType());
        Assertions.assertEquals("m", res.get(0).getValue());
    }

    @Test void randomTabTest() throws Exception {
        var l1 = new Lexer("who would be crazy \s\s\s\s to do this?");
        var res1 = l1.Lex();
        var l2 = new Lexer("who would be crazy \t to do this?");
        var res2 = l2.Lex();

        Object[] expected = {"who", "would", "be", "crazy", "to", "do", "this"};
        Object[] actual1 = arrangeLexerValues(res1);
        Object[] actual2 = arrangeLexerValues(res2);

        Assertions.assertArrayEquals(expected, actual1);
        Assertions.assertArrayEquals(expected, actual2);
    }

    @Test
    public void singleLevelIndentTest() throws Exception {
        var l = new Lexer(
                """
                shared
                \tlineOne
                \tlineTwo""");
        var res = l.Lex();
        res.forEach(System.out::println);

        // Method header
        Assertions.assertEquals(Token.TokenTypes.SHARED, res.get(0).getType());
        Assertions.assertEquals(Token.TokenTypes.NEWLINE, res.get(1).getType());
        // Line 1
        Assertions.assertEquals(Token.TokenTypes.INDENT, res.get(2).getType());
        Assertions.assertEquals(Token.TokenTypes.WORD, res.get(3).getType());
        Assertions.assertEquals("lineOne", res.get(3).getValue());
        Assertions.assertEquals(Token.TokenTypes.NEWLINE, res.get(4).getType());
        // Line 2
        Assertions.assertEquals(Token.TokenTypes.WORD, res.get(5).getType());
        Assertions.assertEquals("lineTwo", res.get(5).getValue());
    }

    @Test
    public void singleLevelDedentTest() throws Exception {
        var l = new Lexer(
                """
                abcdef
                \tghijk
                lmnop
                """);
        var res = l.Lex();

        Assertions.assertEquals(Token.TokenTypes.WORD, res.get(0).getType());
        Assertions.assertEquals("abcdef", res.get(0).getValue());
        Assertions.assertEquals(Token.TokenTypes.NEWLINE, res.get(1).getType());

        Assertions.assertEquals(Token.TokenTypes.INDENT, res.get(2).getType());
        Assertions.assertEquals(Token.TokenTypes.WORD, res.get(3).getType());
        Assertions.assertEquals("ghijk", res.get(3).getValue());
        Assertions.assertEquals(Token.TokenTypes.NEWLINE, res.get(4).getType());

        Assertions.assertEquals(Token.TokenTypes.DEDENT, res.get(5).getType());
        Assertions.assertEquals(Token.TokenTypes.WORD, res.get(6).getType());
        Assertions.assertEquals("lmnop", res.get(6).getValue());
    }

    @Test
    public void doubleLevelIndentTest() throws Exception {
        var l = new Lexer(
                """
                        parentMethodLogic
                        \tif someCondition
                        \t\tsomeLogic
                        \t\tsomeMoreLogic
                        returnToParentMethod
                        """
        );
        var res = l.Lex();
        Assertions.assertEquals(15, res.size());
        // 1st line
        Assertions.assertEquals(Token.TokenTypes.WORD, res.get(0).getType());
        Assertions.assertEquals("parentMethodLogic", res.get(0).getValue());
        Assertions.assertEquals(Token.TokenTypes.NEWLINE, res.get(1).getType());
        // 2nd line
        Assertions.assertEquals(Token.TokenTypes.INDENT, res.get(2).getType());
        Assertions.assertEquals(Token.TokenTypes.IF, res.get(3).getType());
        Assertions.assertEquals(Token.TokenTypes.WORD, res.get(4).getType());
        Assertions.assertEquals("someCondition", res.get(4).getValue());
        Assertions.assertEquals(Token.TokenTypes.NEWLINE, res.get(5).getType());
        // 3rd line
        Assertions.assertEquals(Token.TokenTypes.INDENT, res.get(6).getType());
        Assertions.assertEquals(Token.TokenTypes.WORD, res.get(7).getType());
        Assertions.assertEquals("someLogic", res.get(7).getValue());
        Assertions.assertEquals(Token.TokenTypes.NEWLINE, res.get(8).getType());
        // 4th line
        Assertions.assertEquals(Token.TokenTypes.WORD, res.get(9).getType());
        Assertions.assertEquals("someMoreLogic", res.get(9).getValue());
        Assertions.assertEquals(Token.TokenTypes.NEWLINE, res.get(10).getType());
        // 5th line
        Assertions.assertEquals(Token.TokenTypes.DEDENT, res.get(11).getType());
        Assertions.assertEquals(Token.TokenTypes.DEDENT, res.get(12).getType());
        Assertions.assertEquals(Token.TokenTypes.WORD, res.get(13).getType());
        Assertions.assertEquals("returnToParentMethod", res.get(13).getValue());
        Assertions.assertEquals(Token.TokenTypes.NEWLINE, res.get(14).getType());
    }

    @Test
    public void doubleNewLineTest() throws Exception {
        var l = new Lexer("\t\tScopeLevel2\n\nScopeLevel0");
        var res = l.Lex();

        Object[] actual = arrangeLexerValues(res);
        Object[] expected = {
                Token.TokenTypes.INDENT,
                Token.TokenTypes.INDENT,
                "ScopeLevel2",
                Token.TokenTypes.NEWLINE,
                Token.TokenTypes.DEDENT,
                Token.TokenTypes.DEDENT,
                Token.TokenTypes.NEWLINE,
                "ScopeLevel0"
        };

    }

    @Test
    public void simpleCommentTest() throws Exception {
        var l = new Lexer("{This is a comment!}");
        var res = l.Lex();

        Assertions.assertEquals(0, res.size());
    }

    @Test
    public void nestedCommentTest() throws Exception {
        var l = new Lexer("{This is {also a comment} too!}");
        var res = l.Lex();

        Assertions.assertEquals(0, res.size());
    }

    @Test
    public void dedentAtEndOfFileTest() throws Exception {
        var l = new Lexer(
                "class foo {indent level = 0}\n" +
                        "\tBar (number x) {indent level = 1}\n" +
                        "\t\tloop x>0 {indent level = 2}\n" +
                        "\t\t\tx=x-1 {indent level = 3}\n" +
                    "{because of end of file - output 3 dedent tokens}");
        var res = l.Lex();
        res.forEach(System.out::println);
        Object[] actual = arrangeLexerValues(res);
        Object[] expected = {Token.TokenTypes.CLASS, "foo", Token.TokenTypes.NEWLINE,
                Token.TokenTypes.INDENT, "Bar", Token.TokenTypes.LPAREN, "number", "x", Token.TokenTypes.RPAREN, Token.TokenTypes.NEWLINE,
                Token.TokenTypes.INDENT, Token.TokenTypes.LOOP, "x", Token.TokenTypes.GREATERTHAN, "0", Token.TokenTypes.NEWLINE,
                Token.TokenTypes.INDENT, "x", Token.TokenTypes.ASSIGN, "x", Token.TokenTypes.MINUS, "1", Token.TokenTypes.NEWLINE,
                Token.TokenTypes.DEDENT, Token.TokenTypes.DEDENT, Token.TokenTypes.DEDENT};

//        System.out.println(actual[14].getClass());
//        System.out.println(expected[14].getClass());

        Assertions.assertArrayEquals(expected, actual);
    }

    private Object[] arrangeLexerValues(List<Token> tokens) throws Exception {
        return tokens.stream().map(token -> {
            if (token.getType() == Token.TokenTypes.WORD || token.getType() == Token.TokenTypes.NUMBER) {
                return token.getValue();
            } else {
                return token.getType();
            }
        }).toArray(Object[]::new);
    }

    @Test
    public void testProperCommentSyntaxErrorLocation() throws Exception {
        String txt = "{Bad";
        try {
            var l = new Lexer(txt);
            var res = l.Lex();
        } catch (Exception e) {
            Assertions.assertTrue(e instanceof SyntaxErrorException);
            Assertions.assertEquals("Unclosed comment", e.getMessage());
            Assertions.assertEquals(String.format("Error at line 1 at character %d at SyntaxErrorException: Unclosed comment", txt.length()), e.toString());
        }

    }
}
