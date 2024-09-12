import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;

public class Lexer {
    private final TextManager textManager;
    private final HashMap<String, Token.TokenTypes> keywords;
    private final HashMap<String, Token.TokenTypes> punctuation;
    private int lineNumber, characterPosition;

    public Lexer(String input) {
        this.textManager = new TextManager(input);

        // Fill keywords table
        this.keywords = new HashMap<>(4);
        keywords.put("accessor", Token.TokenTypes.ACCESSOR);
        keywords.put("mutator", Token.TokenTypes.MUTATOR);
        keywords.put("implements", Token.TokenTypes.IMPLEMENTS);
        keywords.put("class", Token.TokenTypes.CLASS);
        keywords.put("interface", Token.TokenTypes.INTERFACE);
        keywords.put("loop", Token.TokenTypes.LOOP);
        keywords.put("if", Token.TokenTypes.IF);
        keywords.put("else", Token.TokenTypes.ELSE);

        // Fill punctuation table
        punctuation = new HashMap<>();
        punctuation.put("=", Token.TokenTypes.ASSIGN);
        punctuation.put("(", Token.TokenTypes.LPAREN);
        punctuation.put(")", Token.TokenTypes.RPAREN);
        punctuation.put(":", Token.TokenTypes.COLON);
        punctuation.put(".", Token.TokenTypes.DOT);
        // Operations
        punctuation.put("+", Token.TokenTypes.PLUS);
        punctuation.put("-", Token.TokenTypes.MINUS);
        punctuation.put("*", Token.TokenTypes.TIMES);
        punctuation.put("/", Token.TokenTypes.DIVIDE);
        punctuation.put("%", Token.TokenTypes.MODULO);
        punctuation.put(",", Token.TokenTypes.COMMA);
        // Truth
        punctuation.put("==", Token.TokenTypes.EQUAL);
        punctuation.put("<", Token.TokenTypes.LESSTHAN);
        punctuation.put(">", Token.TokenTypes.GREATERTHAN);
        punctuation.put("<=", Token.TokenTypes.LESSTHANEQUAL);
        punctuation.put(">=", Token.TokenTypes.GREATERTHANEQUAL);
    }

    public List<Token> Lex() throws Exception {
        var retVal = new LinkedList<Token>();

        Token t = null;
        while (!textManager.isAtEnd()) {
            char x = textManager.peekCharacter();
            if (Character.isLetter(x)) {
                t = parseWord();
            } else if (Character.isDigit(x)) {
                t = parseNumber();
            } else if (x == '-') {
                if (Character.isDigit(textManager.peekCharacter(1))) {
                    t = parseNumber();
                } else {
                    throw new SyntaxErrorException("", lineNumber, characterPosition);
                }
            } else if (x == '.') {
                char nextPeek = textManager.peekCharacter(1); // char after the '.'
                if (Character.isDigit(nextPeek)) {
                    t = parseNumber();
                }
            } else {
                t = parsePunctuation();
            }

            if (null != t) {
                retVal.add(t);
            }
        }

        return retVal;
    }

    private Token parseWord() {
        char c = lexerGetCharacter();
        StringBuilder currentWord = new StringBuilder();

        while (Character.isLetter(c) || '_' == c) {
            currentWord.append(c);
            // Check if at end before get next char
            if (textManager.isAtEnd()) break;
            c = lexerGetCharacter();
        }

        if (!currentWord.isEmpty()) {
            // Create token from currentWord
            String curWord = currentWord.toString();
            return new Token(keywords.getOrDefault(curWord, Token.TokenTypes.WORD), lineNumber, characterPosition, curWord);
        } else {
            return null;
        }
    }

    private Token parseNumber() throws SyntaxErrorException {
        boolean seenDecimal = false;

        char c = textManager.peekCharacter();
        StringBuilder currentWord = new StringBuilder();

        // TODO: Handle negative numbers
        while ( Character.isDigit(c) || '.' == c) {
            currentWord.append(c);

            if (textManager.isAtEnd()) break;

            if (seenDecimal && '.' == c) {
                // Handle 3.4.5 exception
//                throw new SyntaxErrorException(
//                        String.format("Invalid number %s", currentWord.append(c)),
//                        lineNumber,
//                        characterPosition
//                );
                break;
            }
            if ('.' == c) {
                seenDecimal = true;
            }

            c = lexerGetCharacter();
        }

        if (!currentWord.isEmpty()) {
            return new Token(Token.TokenTypes.NUMBER, lineNumber, characterPosition, currentWord.toString());
        } else {
            return null;
        }
    }

    private Token parsePunctuation() throws SyntaxErrorException {
        String smallOperator = "" + lexerGetCharacter();
        String bigOperator = smallOperator + textManager.peekCharacter();
        if (punctuation.containsKey(bigOperator)) {
            lexerGetCharacter();
            return new Token(punctuation.get(bigOperator), lineNumber, characterPosition);
        } else if (punctuation.containsKey(smallOperator)) {
            return new Token(punctuation.get(smallOperator), lineNumber, characterPosition);
        } else {
            return null;
        }
    }

    // getCharacter(), but it tracks line and col #
    private char lexerGetCharacter() {
        char c = textManager.getCharacter();
        characterPosition++;
        if ('\n' == c) {
            lineNumber++;
            characterPosition = 0;
        }
        return c;
    }
    // EOC
}
