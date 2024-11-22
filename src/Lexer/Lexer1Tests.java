package Lexer;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

public class Lexer1Tests {

    @Test
    public void SimpleLexerTest() {
        var l = new Lexer("ab cd ef gh");
        try {
            var res = l.Lex();
            Assertions.assertEquals(4, res.size());
            Assertions.assertEquals("ab", res.get(0).getValue());
            Assertions.assertEquals("cd", res.get(1).getValue());
            Assertions.assertEquals("ef", res.get(2).getValue());
            Assertions.assertEquals("gh", res.get(3).getValue());
            for (var result : res)
                Assertions.assertEquals(Token.TokenTypes.WORD, result.getType());
        }
        catch (Exception e) {
            Assertions.fail("exception occurred: " +  e.getMessage());
        }
    }

    @Test
    public void MultilineLexerTest() {
        var l = new Lexer("ab cd ef gh\nasdjkdsajkl\ndsajkdsa asdjksald dsajhkl \n");
        try {
            var res = l.Lex();
//            Assertions.assertEquals(11, res.size());
            Assertions.assertEquals("ab", res.get(0).getValue());
            Assertions.assertEquals("cd", res.get(1).getValue());
            Assertions.assertEquals("ef", res.get(2).getValue());
            Assertions.assertEquals("gh", res.get(3).getValue());
            Assertions.assertEquals(Token.TokenTypes.NEWLINE, res.get(4).getType());
            Assertions.assertEquals("asdjkdsajkl", res.get(5).getValue());
            Assertions.assertEquals(Token.TokenTypes.NEWLINE, res.get(6).getType());
            Assertions.assertEquals("dsajkdsa", res.get(7).getValue());
            Assertions.assertEquals("asdjksald", res.get(8).getValue());
            Assertions.assertEquals("dsajhkl", res.get(9).getValue());
            Assertions.assertEquals(Token.TokenTypes.NEWLINE, res.get(10).getType());
        }
        catch (Exception e) {
            Assertions.fail("exception occurred: " +  e.getMessage());
        }
    }

    @Test
    public void NotEqualsTest() {
        var l = new Lexer("!=");
        try {
            var res = l.Lex();
            Assertions.assertEquals(1, res.size());
            Assertions.assertEquals(Token.TokenTypes.NOTEQUAL, res.get(0).getType());
        }
        catch (Exception e) {
            Assertions.fail("exception occurred: " +  e.getMessage());
        }
    }

    @Test
    public void TwoCharacterTest() {
        var l = new Lexer(">= > <= < = == !=");
        try {
            var res = l.Lex();
            Assertions.assertEquals(7, res.size());
            Assertions.assertEquals(Token.TokenTypes.GREATERTHANEQUAL, res.get(0).getType());
            Assertions.assertEquals(Token.TokenTypes.GREATERTHAN, res.get(1).getType());
            Assertions.assertEquals(Token.TokenTypes.LESSTHANEQUAL, res.get(2).getType());
            Assertions.assertEquals(Token.TokenTypes.LESSTHAN, res.get(3).getType());
            Assertions.assertEquals(Token.TokenTypes.ASSIGN, res.get(4).getType());
            Assertions.assertEquals(Token.TokenTypes.EQUAL, res.get(5).getType());
            Assertions.assertEquals(Token.TokenTypes.NOTEQUAL, res.get(6).getType());
        }
        catch (Exception e) {
            Assertions.fail("exception occurred: " +  e.getMessage());
        }
    }

    @Test
    public void MixedTest() {
        var l = new Lexer("word 1.2 : ( )");
        try {
            var res = l.Lex();
            Assertions.assertEquals(5, res.size());
            Assertions.assertEquals(Token.TokenTypes.WORD, res.get(0).getType());
            Assertions.assertEquals("word", res.get(0).getValue());
            Assertions.assertEquals(Token.TokenTypes.NUMBER, res.get(1).getType());
            Assertions.assertEquals("1.2", res.get(1).getValue());
            Assertions.assertEquals(Token.TokenTypes.COLON, res.get(2).getType());
            Assertions.assertEquals(Token.TokenTypes.LPAREN, res.get(3).getType());
            Assertions.assertEquals(Token.TokenTypes.RPAREN, res.get(4).getType());
        }
        catch (Exception e) {
            Assertions.fail("exception occurred: " +  e.getMessage());
        }
    }

    @Nested
    class CustomLexer1Tests {

        @Test
        public void singleWordTest() throws Exception {
            Lexer lexer = new Lexer("Hello");
            Assertions.assertEquals("Hello", lexer.Lex().getFirst().getValue());
        }

        @Test
        public void simpleSentenceTest() throws Exception {
            String text = "there was a pneumonoultramicroscopicsilicovolcanoconiosis";
            Lexer lexer = new Lexer(text);

            String[] correctWords = text.split("\\s");
            String[] testWords = lexer.Lex().stream().map(Token::getValue).toArray(String[]::new); // I'm a stream MASTER!!

            Assertions.assertArrayEquals(correctWords, testWords);
        }

        @Test
        public void multilineStringTest() throws Exception {
            String text = "This is a line. \n This is another line.";
            Lexer lexer = new Lexer(text);
            var res = lexer.Lex();

            Assertions.assertEquals("This", res.get(0).getValue());
            Assertions.assertEquals("is", res.get(1).getValue());
            Assertions.assertEquals("a", res.get(2).getValue());
            Assertions.assertEquals("line", res.get(3).getValue());
            Assertions.assertEquals(Token.TokenTypes.DOT, res.get(4).getType());
            Assertions.assertEquals(Token.TokenTypes.NEWLINE, res.get(5).getType());
            Assertions.assertEquals("This", res.get(6).getValue());
            Assertions.assertEquals("is", res.get(7).getValue());
            Assertions.assertEquals("another", res.get(8).getValue());
            Assertions.assertEquals("line", res.get(9).getValue());
            Assertions.assertEquals(Token.TokenTypes.DOT, res.get(10).getType());

    //        Assertions.assertArrayEquals(correct, test);
        }

        @Test
        public void wholeNumbersTest() throws Exception {
            String txt =  "1 2 3 4 100 12491";
            Lexer lexer = new Lexer(txt);

            String[] correct = txt.split(" ");
            String[] test = lexer.Lex().stream().map(Token::getValue).toArray(String[]::new);

            Assertions.assertArrayEquals(correct, test);
        }

        @Test
        public void floatNumbersTest() throws Exception {
            String txt =  "3.14145 4.0151 0.11010 0.14143 0.0";
            Lexer lexer = new Lexer(txt);

            String[] correct = txt.split(" ");
            String[] test = lexer.Lex().stream().map(Token::getValue).toArray(String[]::new);

            Assertions.assertArrayEquals(correct, test);
        }

    //    @Test
        public void negMixedNumbersTest() throws Exception {
            String txt =  "-3.14145 -451 -0.14143";
            Lexer lexer = new Lexer(txt);

            String[] correct = txt.split(" ");
            String[] test = lexer.Lex().stream().map(Token::getValue).toArray(String[]::new);

            Assertions.assertArrayEquals(correct, test);
        }

    //    @Test
        public void throwExceptionAtNotNumbersTest() {
            String txt =  "5.14.145 3.4.5";
            Lexer lexer = new Lexer(txt);

            Assertions.assertThrows(Exception.class, lexer::Lex);
        }


        @Test
        public void completeSentenceTest() throws Exception {
            String text = "I don't like green eggs and ham. I don't like them, Sam I am.";
            Lexer lexer = new Lexer(text);
            var res = lexer.Lex();

            Assertions.assertEquals("I", res.get(0).getValue());
            Assertions.assertEquals("don", res.get(1).getValue());
            Assertions.assertEquals("t", res.get(2).getValue());
            Assertions.assertEquals("like", res.get(3).getValue());
            Assertions.assertEquals("green", res.get(4).getValue());
            Assertions.assertEquals("eggs", res.get(5).getValue());
            Assertions.assertEquals("and", res.get(6).getValue());
            Assertions.assertEquals("ham", res.get(7).getValue());
            Assertions.assertEquals(Token.TokenTypes.DOT, res.get(8).getType());
            Assertions.assertEquals("I", res.get(9).getValue());
            Assertions.assertEquals("don", res.get(10).getValue());
            Assertions.assertEquals("t", res.get(11).getValue());
            Assertions.assertEquals("like", res.get(12).getValue());
            Assertions.assertEquals("them", res.get(13).getValue());
            Assertions.assertEquals(Token.TokenTypes.COMMA, res.get(14).getType());
            Assertions.assertEquals("Sam", res.get(15).getValue());
            Assertions.assertEquals("I", res.get(16).getValue());
            Assertions.assertEquals("am", res.get(17).getValue());
            Assertions.assertEquals(Token.TokenTypes.DOT, res.get(18).getType());
        }


        @Test
        public void singleDotCharTest() throws Exception {
            var l = new Lexer(".");
            var res = l.Lex();

            Assertions.assertEquals(res.getFirst().getType(), Token.TokenTypes.DOT);
        }

        @Test
        public void miniNewLineTest() throws Exception {
            var l = new Lexer("awd\nawdwd");
            var res = l.Lex();

            Assertions.assertEquals("awd", res.get(0).getValue());
            Assertions.assertEquals( Token.TokenTypes.NEWLINE, res.get(1).getType());
            Assertions.assertEquals("awdwd", res.get(2).getValue());
        }

        private String[] arrangeLexerValues(String text) throws Exception {
            Lexer lexer = new Lexer(text);
            return lexer.Lex().stream().map(Token::getValue).toArray(String[]::new);
        }

        @Test
        public void specialSymbolsTest() throws Exception {
            Lexer l = new Lexer("@ # @#@ &_");
            var res = l.Lex();

            Assertions.assertEquals(0, res.size());
        }

        @Test
        public void testDebuggabilityOfAddCharAndString() {
            String actual = addCharAndString();

            Assertions.assertEquals("hello world", actual);
        }

        private String addCharAndString() {
            String str = "hello worl";
            TextManager textManager = new TextManager("ddddd");
            String res = str + textManager.peekCharacter();
            return res;
        }
    }
}
