import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

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
        var l = new Lexer("\sm");
        var res = l.Lex();
        Assertions.assertEquals(1, res.size());
        Assertions.assertEquals(Token.TokenTypes.WORD, res.get(0).getType());
        Assertions.assertEquals("m", res.get(0).getValue());
    }

    @Test
    public void singleLevelIndentTest() throws Exception {
        var l = new Lexer(
                """
                shared method : number x
                \tlineOne
                \tlineTwo
                \tlineThree
                """);
        var res = l.Lex();
//        res.forEach(System.out::println);

        // Method header
        Assertions.assertEquals(Token.TokenTypes.SHARED, res.get(0).getType());
        Assertions.assertEquals(Token.TokenTypes.WORD, res.get(1).getType());
        Assertions.assertEquals("method", res.get(1).getValue());
        Assertions.assertEquals(Token.TokenTypes.COLON, res.get(2).getType());
        Assertions.assertEquals(Token.TokenTypes.WORD, res.get(3).getType());
        Assertions.assertEquals("number", res.get(3).getValue());
        Assertions.assertEquals(Token.TokenTypes.WORD, res.get(4).getType());
        Assertions.assertEquals("x", res.get(4).getValue());
        Assertions.assertEquals(Token.TokenTypes.NEWLINE, res.get(5).getType());
        // Line 1
        Assertions.assertEquals(Token.TokenTypes.INDENT, res.get(6).getType());
        Assertions.assertEquals(Token.TokenTypes.WORD, res.get(7).getType());
        Assertions.assertEquals("lineOne", res.get(7).getValue());
        Assertions.assertEquals(Token.TokenTypes.NEWLINE, res.get(8).getType());
        // Line 2
        Assertions.assertEquals(Token.TokenTypes.WORD, res.get(9).getType());
        Assertions.assertEquals("lineTwo", res.get(9).getValue());
        Assertions.assertEquals(Token.TokenTypes.NEWLINE, res.get(10).getType());
        // Line 3
        Assertions.assertEquals(Token.TokenTypes.WORD, res.get(11).getType());
        Assertions.assertEquals("lineThree", res.get(11).getValue());
        Assertions.assertEquals(Token.TokenTypes.NEWLINE, res.get(12).getType());
    }

    @Test
    public void singleLevelDedentTest() throws Exception {
        var l = new Lexer(
                """
                abcdef\tghijk lmnop
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
        Assertions.assertEquals(14, res.size());
        // 1st line
        Assertions.assertEquals(Token.TokenTypes.WORD, res.get(0).getType());
        Assertions.assertEquals("parentMethodLogic", res.get(0).getValue());
        Assertions.assertEquals(Token.TokenTypes.NEWLINE, res.get(1).getType());
        // 2nd line
        Assertions.assertEquals(Token.TokenTypes.INDENT, res.get(2).getType());
        Assertions.assertEquals(Token.TokenTypes.WORD, res.get(3).getType());
        Assertions.assertEquals("if", res.get(3).getValue());
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
    }

    private Object[] arrangeLexerValues(String text) throws Exception {
        Lexer lexer = new Lexer(text);
        return lexer.Lex().stream().map(token -> {
            if (token.getType() == Token.TokenTypes.WORD || token.getType() == Token.TokenTypes.NUMBER) {
                return token.getValue();
            } else {
                return token.getType();
            }
        }).toArray(Object[]::new);
    }
}
