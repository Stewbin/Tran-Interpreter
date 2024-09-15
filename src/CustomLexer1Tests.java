import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Assertions;

import java.util.Arrays;
import java.util.regex.*;

public class CustomLexer1Tests {

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
}
