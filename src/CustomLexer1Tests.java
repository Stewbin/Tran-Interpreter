import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

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

        String[] test = lexer.Lex().stream().map(Token::getValue).toArray(String[]::new);
        String[] correct = {"This", "is", "a", "line", ".", "This", "is", "another", "line", "."};

        for (int i = 0; i < test.length; i++) {
            System.out.println("Test: " + test[i]);
            System.out.println("Correct: " + correct[i]);
        }

        Assertions.assertArrayEquals(correct, test);
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
//    public void throwExceptionAtNotNumbersTest() {
//        String txt =  "5.14.145 3.4.5";
//        Lexer lexer = new Lexer(txt);
//
//        Assertions.assertThrows(Exception.class, lexer::Lex);
//    }

//    @Test
    public void reformatSavableNumbersTest() throws Exception {
        String txt =  "-0 00 0123 -0124";
        Lexer lexer = new Lexer(txt);

        String[] correct = {"0", "0", "123", "-124"};
        String[] test = lexer.Lex().stream().map(Token::getValue).toArray(String[]::new);

        Assertions.assertArrayEquals(correct, test);
    }

    @Test
    public void completeSentenceTest() throws Exception {
        String text = "I don't like green eggs and ham. I don't like them, Sam I am.";
        Lexer lexer = new Lexer(text);

        String[] correct = makeTokens(text);
        System.out.println("Correct:\n" + Arrays.toString(correct));
        String[] test = lexer.Lex().stream().map(Token::getValue).toArray(String[]::new);
        System.out.println("Test:\n" + Arrays.toString(test));

        Assertions.assertArrayEquals(correct, test);
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

    private String[] makeTokens(String txt) {
        Matcher matcher = Pattern.compile("\\d*\\.\\d+|\\w+|\\.").matcher(txt);

        return matcher.results().map(MatchResult::group).toArray(String[]::new);
    }

    @Test
    public void testMakeTokens() {
        String txt = "Tokens tokens . 123 moar TOKENS 0.1134 .";
        String[] actual = makeTokens(txt);
        String[] expected = {"Tokens", "tokens", ".", "123", "moar", "TOKENS", "0.1134", "."};

        Assertions.assertArrayEquals(expected, actual);
    }

    private String[] arrangeLexerValues(String text) throws Exception {
        Lexer lexer = new Lexer(text);
        return lexer.Lex().stream().map(Token::getValue).toArray(String[]::new);
    }
}
