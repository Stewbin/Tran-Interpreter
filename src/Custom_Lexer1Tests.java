import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class Custom_Lexer1Tests {

    @Test
    public void singleWordTest() throws Exception {
        Lexer lexer = new Lexer("Hello");
        Assertions.assertEquals("Hello", lexer.Lex().getFirst().getValue());
    }

    @Test
    public void simpleSentenceTest() throws Exception {
        String text = "there was a pneumonoultramicroscopicsilicovolcanoconiosis";
        Lexer lexer = new Lexer(text);

        String[] correctWords = text.split(" ");
        String[] testWords = lexer.Lex().stream().map(Token::getValue).toArray(String[]::new); // I'm a stream MASTER!!

        Assertions.assertArrayEquals(correctWords, testWords);
    }

    @Test
    public void snakeCaseTest() throws Exception {
        Lexer lexer = new Lexer("snake_case");
        Assertions.assertEquals("snake_case", lexer.Lex().getFirst().getValue());
    }

    @Test
    public void multilineStringTest() throws Exception {
        String text = "This is a line. \n This is another line.";
        Lexer lexer = new Lexer(text);

        String[] correct = text.split("\\W+");
        String[] test = lexer.Lex().stream().map(Token::getValue).toArray(String[]::new);

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

    @Test
    public void negMixedNumbersTest() throws Exception {
        String txt =  "-3.14145 -451 -0.14143";
        Lexer lexer = new Lexer(txt);

        String[] correct = txt.split(" ");
        String[] test = lexer.Lex().stream().map(Token::getValue).toArray(String[]::new);

        Assertions.assertArrayEquals(correct, test);
    }

    @Test
    public void throwExceptionAtNotNumbersTest() {
        String txt =  "5.14.145 3.4.5";
        Lexer lexer = new Lexer(txt);

        Assertions.assertThrows(Exception.class, lexer::Lex);
    }

    @Test
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

        String[] correct = text.split("\\W+");
        String[] test = lexer.Lex().stream().map(Token::getValue).toArray(String[]::new);

        Assertions.assertArrayEquals(correct, test);
    }
    


    // Actually testing Tran snippets
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


}
