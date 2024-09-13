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
        // Line 1 (whatever this is)
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
        punctuation.put("!=", Token.TokenTypes.NOTEQUAL);
        punctuation.put("<", Token.TokenTypes.LESSTHAN);
        punctuation.put(">", Token.TokenTypes.GREATERTHAN);
        punctuation.put("<=", Token.TokenTypes.LESSTHANEQUAL);
        punctuation.put(">=", Token.TokenTypes.GREATERTHANEQUAL);
        // Spaces
        punctuation.put("\t", Token.TokenTypes.INDENT);
        punctuation.put("\n", Token.TokenTypes.NEWLINE);
    }

    public List<Token> Lex() throws Exception {
        var retVal = new LinkedList<Token>();

        Token t = null;
        while (!textManager.isAtEnd()) {
            char x = textManager.peekCharacter();
            if (x == ' ') { // Ignore all whitespace for now
                lexerGetCharacter(); // Increment position
                continue;
            } else if (Character.isLetter(x)) {
                t = parseWord();
            } else if (Character.isDigit(x)) {
                t = parseNumber();
            } else if (x == '.') {
                char nextPeek = textManager.peekCharacter(1);
                if (Character.isDigit(nextPeek)) {
                    t = parseNumber();
                } else {
                    t = parsePunctuation();
                }
            } else {
                    t = parsePunctuation();
            }

            if (null != t) {
                retVal.add(t);
                t = null; // Reset token, to be safe
            }
        }

        return retVal;
    }

    private Token parseWord() {
        char c = textManager.peekCharacter();
        StringBuilder currentWord = new StringBuilder();

        while (Character.isLetter(c)) {
            currentWord.append(lexerGetCharacter());
            // Check if at end before get next char
            if (textManager.isAtEnd()) break;
            c = textManager.peekCharacter();
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
            currentWord.append(lexerGetCharacter());

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

            c = textManager.peekCharacter();
        }

        if (!currentWord.isEmpty()) {
            return new Token(Token.TokenTypes.NUMBER, lineNumber, characterPosition, currentWord.toString());
        } else {
            return null;
        }
    }

    private Token parsePunctuation() throws SyntaxErrorException {
        String smallOperator = "" + lexerGetCharacter();
        // Check if at end before peeking
        String bigOperator = textManager.isAtEnd() ? "" : smallOperator + textManager.peekCharacter();
        if (punctuation.containsKey(bigOperator)) {
            lexerGetCharacter();
            return new Token(punctuation.get(bigOperator), lineNumber, characterPosition);
        } else if (punctuation.containsKey(smallOperator)) {
            return new Token(punctuation.get(smallOperator), lineNumber, characterPosition);
        } else {
            return null;
        }
    }

    // TextManager.getCharacter(), but it tracks line and col #
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
