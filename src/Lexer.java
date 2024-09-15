import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;

public class Lexer {
    private final TextManager textManager;
    private final HashMap<String, Token.TokenTypes> keywords;
    private final HashMap<String, Token.TokenTypes> punctuation;
    private int lineNumber = 1, characterPosition = 0;
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

        keywords.put("true", Token.TokenTypes.TRUE);
        keywords.put("false", Token.TokenTypes.FALSE);
        keywords.put("new", Token.TokenTypes.NEW);
        keywords.put("private", Token.TokenTypes.PRIVATE);
        keywords.put("shared", Token.TokenTypes.SHARED);
        keywords.put("constructor", Token.TokenTypes.CONSTRUCT);

        // Fill punctuation table
        punctuation = new HashMap<>();
        // ???
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
        // Relations
        punctuation.put("==", Token.TokenTypes.EQUAL);
        punctuation.put("!=", Token.TokenTypes.NOTEQUAL);
        punctuation.put("<", Token.TokenTypes.LESSTHAN);
        punctuation.put(">", Token.TokenTypes.GREATERTHAN);
        punctuation.put("<=", Token.TokenTypes.LESSTHANEQUAL);
        punctuation.put(">=", Token.TokenTypes.GREATERTHANEQUAL);
        // Spaces
        punctuation.put("\t", Token.TokenTypes.INDENT);
        punctuation.put("    ", Token.TokenTypes.INDENT);
        punctuation.put("\n", Token.TokenTypes.NEWLINE);
        // Logic
        punctuation.put("&&", Token.TokenTypes.AND);
        punctuation.put("||", Token.TokenTypes.OR);
        punctuation.put("!", Token.TokenTypes.NOT);
        // Quote
        punctuation.put("'", Token.TokenTypes.QUOTEDCHARACTER);
        punctuation.put("\"", Token.TokenTypes.QUOTEDSTRING);

    }

    public List<Token> Lex() throws Exception {
        var retVal = new LinkedList<Token>();
        Token t = null;

        while (!textManager.isAtEnd()) {
            char x = textManager.peekCharacter();
            if (Character.isLetter(x)) {
                // Words & Keywords
                t = parseWord();
            } else if (Character.isDigit(x)) {
                // Numbers
                t = parseNumber();
            } else if (x == '.') {
                // Float Numbers
                char nextPeek = textManager.peekCharacter(1);
                if (Character.isDigit(nextPeek)) {
                    t = parseNumber();
                } else {
                    // DOT
                    t = parsePunctuation();
                }
            } else if ('\'' == x) {
                // QuotedCharacters
                t = parseQuotedCharacter();
            } else if ('"' == x) {
                // QuotedStrings
                t = parseQuotedString();
            } else if ('{' == x) {
                // Comments
                parseComment();
            } else if ('\t' == x || ' ' == x) {
                t = parseIndent();
            } else {
                t = parsePunctuation();
            }

            // Filter out invalid tokens
            if (null != t) {
                retVal.add(t);
                t = null; // Reset token, to be safe
            }
        }

        return retVal;
    }

    private Token parseWord() {
        StringBuilder currentWord = new StringBuilder();
        char c = textManager.peekCharacter();

        while (Character.isLetter(c)) {
            currentWord.append(lexerGetCharacter());
            // Check if at end before get next char
            if (textManager.isAtEnd()) {
                break;
            }
            c = textManager.peekCharacter();
        }

        if (!currentWord.isEmpty()) {
            // Create token from currentWord
            String curWord = currentWord.toString();
            if (keywords.containsKey(curWord)) {
                // Keyword found
                return new Token(keywords.get(curWord), lineNumber, characterPosition);
            } else {
                // Name found
                return new Token(Token.TokenTypes.WORD, lineNumber, characterPosition, curWord);
            }
        } else {
            // Invalid token
            return null;
        }
    }

    private Token parseNumber() throws SyntaxErrorException {
        boolean seenDecimal = false;
        char c = textManager.peekCharacter();
        StringBuilder currentWord = new StringBuilder();

        // TODO: Handle negative numbers
        while (Character.isDigit(c) || '.' == c) {
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

    private Token parseQuotedCharacter() throws SyntaxErrorException {
        // e.g. 'c'
        lexerGetCharacter(); // consume the '\''
        String c = "" + lexerGetCharacter(); // = c
        if (lexerGetCharacter() != '\'') {
            throw new SyntaxErrorException("Unclosed char literal", lineNumber, characterPosition);
        }

        return new Token(Token.TokenTypes.QUOTEDCHARACTER, lineNumber, characterPosition, c);
    }
    private Token parseQuotedString() throws SyntaxErrorException {
        lexerGetCharacter(); // consume the '"'
        boolean isInQuote = true;
        StringBuilder currentWord = new StringBuilder();

        while (!textManager.isAtEnd()) {
            char c = lexerGetCharacter();
            if ('\"' == c) {
                isInQuote = false; // Begins as false}
                break;
            }
            currentWord.append(c);
        }

        if (isInQuote) { // && textManager.isAtEnd()
            throw new SyntaxErrorException("Unclosed string literal", lineNumber, characterPosition);
        }

        return new Token(Token.TokenTypes.QUOTEDSTRING, lineNumber, characterPosition, currentWord.toString());
    }

    private void parseComment() throws SyntaxErrorException {
        char c = textManager.peekCharacter();
        int closingBracesNeeded = 1;

        while (closingBracesNeeded > 0) {
            c = lexerGetCharacter();
            if ('}' == c) closingBracesNeeded--;
            if ('{' == c) closingBracesNeeded++;

            if (textManager.isAtEnd() && closingBracesNeeded > 0) {
                throw new SyntaxErrorException("Unclosed comment", lineNumber, characterPosition);
            }
        }
    }

    private int scopeIndentationLevel = 0;

    private Token parseIndent() {
        char c = lexerGetCharacter(); // c = '\t' or '\s'

        final int SPACES_PER_INDENT = 4;
        for (int i = 0; i < SPACES_PER_INDENT; i++) {
            if (c != ' ') {
                break;
            }
            c = lexerGetCharacter();
        }

        // int indentCount = 0;
        // if c == \t or equivalent
        // indentCount++;
        // if indentCount - scopeIndentationLevel > 0
        //      return new INDENT
        // else if indentCount - scopeIndentationLevel < 0
        //      return new DEDENT
        return null;
    }

    // EOC
}
