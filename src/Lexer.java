import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;

public class Lexer {
    private TextManager textManager;
    private final HashMap<String, Token.TokenTypes> keywords;
    private final HashMap<String, Token.TokenTypes> punctuation;
    private int lineNumber, characterPosition;

    public Lexer(String input) {
        this.textManager = new TextManager(input);

        // Fill keywords table
        this.keywords = new HashMap<>(4);
        keywords.put("if", Token.TokenTypes.IF);
        keywords.put("else", Token.TokenTypes.ELSE);

        // Fill punctuation table
        punctuation = new HashMap<>();
        punctuation.put("=", Token.TokenTypes.ASSIGN);
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
            char x = textManager.peekCharacter(0);
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
                } else {
                    // TODO: Handle method calls
                }
            } else {
//                t = parsePunctuation();
            }
            retVal.add(t);
        }

        return retVal;
    }

    private Token parseWord() {
        char c = textManager.getCharacter();
        StringBuilder currentWord = new StringBuilder();

        // Get next character until first non-letter
        // e.g. whitespace
        while (Character.isLetter(c) || '_' == c) {
            currentWord.append(c);

            if (textManager.isAtEnd()) break;

            c = textManager.getCharacter();
        }

        if (!currentWord.isEmpty()) {
            // Create token from currentWord
            String curWord = currentWord.toString();
            if (keywords.containsKey(curWord)) {
                return new Token(keywords.get(curWord), lineNumber, characterPosition, curWord);
            } else {
                return new Token(Token.TokenTypes.WORD, lineNumber, characterPosition, curWord);
            }
        } else {
            return null;
        }
    }

    private Token parseNumber() throws Exception {
        boolean seenDecimal = false;

        char c = textManager.getCharacter();
        StringBuilder currentWord = new StringBuilder();

        // TODO: Handle negative numbers
        while ( Character.isDigit(c) || '.' == c || '-' == c) {
            currentWord.append(c);

            if (textManager.isAtEnd()) break;

            // Handle 3.4.5 exception
            if (seenDecimal && '.' == c) {
                throw new SyntaxErrorException(
                        String.format("Invalid number %s", currentWord.append(c)),
                        lineNumber,
                        characterPosition
                );
            }
            if ('.' == c) {
                seenDecimal = true;
            }

            c = textManager.getCharacter();
            if ('-' == c) {
                throw new SyntaxErrorException(
                        "Invalid number " + currentWord.append(c),
                        lineNumber,
                        characterPosition
                );
            }
        }

        if (!currentWord.isEmpty()) {
            if (currentWord.length() >= 2) {
                // Remove negative zero
                if (currentWord.substring(0, 2).equals("-0")) {
                    currentWord.delete(0,2);
                }
                // Remove leading zeroes
                while (currentWord.charAt(0) == '0') {
                    currentWord.deleteCharAt(0);
                }
            }

            return new Token(Token.TokenTypes.NUMBER, lineNumber, characterPosition, currentWord.toString());
        } else {
            return null;
        }
    }

    int maxPunctuationLength = 2;
    private Token parsePunctuation() throws SyntaxErrorException {
        String bigOperator = "" + (textManager.getCharacter() + textManager.peekCharacter(maxPunctuationLength - 1));
        if (keywords.containsKey(bigOperator)) {
            return new Token(keywords.get(bigOperator), lineNumber, characterPosition);
        } else {
            String smallOperator = "" + (textManager.getCharacter());
            if (keywords.containsKey(smallOperator)) {
                return new Token(keywords.get(smallOperator), lineNumber, characterPosition);
            } else {
                throw new SyntaxErrorException("Invalid punctuation " + bigOperator, lineNumber, characterPosition);
            }
        }
    }

}
