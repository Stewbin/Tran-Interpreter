import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class Lexer2Tests {

    @Test
    public void KeyWordLexerTest() {
        var l = new Lexer("class interface something accessor: mutator: if else loop");
        try {
            var res = l.Lex();
            Assertions.assertEquals(10, res.size());
            Assertions.assertEquals(Token.TokenTypes.CLASS, res.get(0).getType());
            Assertions.assertEquals(Token.TokenTypes.INTERFACE, res.get(1).getType());
            Assertions.assertEquals(Token.TokenTypes.WORD, res.get(2).getType());
            Assertions.assertEquals("something", res.get(2).getValue());
            Assertions.assertEquals(Token.TokenTypes.ACCESSOR, res.get(3).getType());
            Assertions.assertEquals(Token.TokenTypes.COLON, res.get(4).getType());
            Assertions.assertEquals(Token.TokenTypes.MUTATOR, res.get(5).getType());
            Assertions.assertEquals(Token.TokenTypes.COLON, res.get(6).getType());
            Assertions.assertEquals(Token.TokenTypes.IF, res.get(7).getType());
            Assertions.assertEquals(Token.TokenTypes.ELSE, res.get(8).getType());
            Assertions.assertEquals(Token.TokenTypes.LOOP, res.get(9).getType());
        }
        catch (Exception e) {
            Assertions.fail("exception occurred: " +  e.getMessage());
        }
    }

    @Test
    public void QuotedStringLexerTest() {
        var l = new Lexer("test \"hello\" \"there\" 1.2");
        try {
            var res = l.Lex();
            Assertions.assertEquals(4, res.size());
            Assertions.assertEquals(Token.TokenTypes.WORD, res.get(0).getType());
            Assertions.assertEquals("test", res.get(0).getValue());
            Assertions.assertEquals(Token.TokenTypes.QUOTEDSTRING, res.get(1).getType());
            Assertions.assertEquals("hello", res.get(1).getValue());
            Assertions.assertEquals(Token.TokenTypes.QUOTEDSTRING, res.get(2).getType());
            Assertions.assertEquals("there", res.get(2).getValue());
            Assertions.assertEquals(Token.TokenTypes.NUMBER, res.get(3).getType());
            Assertions.assertEquals("1.2", res.get(3).getValue());
        }
        catch (Exception e) {
            Assertions.fail("exception occurred: " +  e.getMessage());
        }
    }


    @Test
    public void IndentTest() {
        var l = new Lexer(
                "loop keepGoing\n" +
                        "    if n >= 15\n" +
                        "        keepGoing = false\n"
        );
        try {
            var res = l.Lex();
            Assertions.assertEquals(16, res.size());
        } catch (Exception e) {
            Assertions.fail("exception occurred: " + e.getMessage());
        }
    }

    // Custom tests //

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
        var l = new Lexer(" \"\"");
        var res = l.Lex();
        Assertions.assertEquals(2, res.size());
        Assertions.assertEquals(Token.TokenTypes.QUOTEDSTRING, res.get(0).getType());
        Assertions.assertEquals(Token.TokenTypes.QUOTEDSTRING, res.get(1).getType());
    }

    @Test
    public void throwExceptionAtUnclosedString() throws Exception {
        var l = new Lexer("\"hello world");
        Assertions.assertThrows(SyntaxErrorException.class, l::Lex);
    }

    @Test void indentJumpTest() throws Exception {
        var l = new Lexer(
                """
                        parentMethodLogic\n
                        \tif someCondition\n
                        \t\tsomeLogic\n
                        \t\tsomeMoreLogic\n
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
}
