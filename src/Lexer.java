import java.util.*;

public class Lexer {
    private final TextManager textManager;
    private final HashMap<String, Token.TokenTypes> keywords;
    private final HashMap<String, Token.TokenTypes> punctuation;
    private int lineNumber = 1, columnNumber = 0;
    private int scopeLevel = 0;

    public Lexer(String input) {
        this.textManager = new TextManager(input);

        // Fill keywords table
        this.keywords = new HashMap<>(14);
        keywords.put("accessor", Token.TokenTypes.ACCESSOR);
        keywords.put("mutator", Token.TokenTypes.MUTATOR);
        keywords.put("implements", Token.TokenTypes.IMPLEMENTS);
        keywords.put("class", Token.TokenTypes.CLASS);
        keywords.put("interface", Token.TokenTypes.INTERFACE);
        keywords.put("loop", Token.TokenTypes.LOOP);
        keywords.put("if", Token.TokenTypes.IF);
        keywords.put("else", Token.TokenTypes.ELSE);

        keywords.put("true", Token.TokenTypes.TRUE);
        keywords.put("new", Token.TokenTypes.NEW);
        keywords.put("false", Token.TokenTypes.FALSE);
        keywords.put("private", Token.TokenTypes.PRIVATE);
        keywords.put("shared", Token.TokenTypes.SHARED);
        keywords.put("construct", Token.TokenTypes.CONSTRUCT);

        keywords.put("and", Token.TokenTypes.AND);
        keywords.put("or", Token.TokenTypes.OR);
        keywords.put("not", Token.TokenTypes.NOT);

        // Fill punctuation table
        punctuation = new HashMap<>(21);
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
        punctuation.put("\n", Token.TokenTypes.NEWLINE);
        // (Include symbols support for bool operators)
        punctuation.put("&&", Token.TokenTypes.AND);
        punctuation.put("||", Token.TokenTypes.OR);
        punctuation.put("!", Token.TokenTypes.NOT);
    }

    public List<Token> Lex() throws Exception {
        var retVal = new LinkedList<Token>();
        Optional<Token> t = Optional.empty();
        boolean shouldUpdateScope = true;

        while (!textManager.isAtEnd()) {

            // Parse indents after NEWLINE has been added
            if (shouldUpdateScope) {
                parseIndents(retVal);
                shouldUpdateScope = false;
            }

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
            } else if ('\n' == x) {
                // Newline
                t = parsePunctuation();
                shouldUpdateScope = true; // Check indent level after every newline
            } else if ('\'' == x) {
                // QuotedCharacters
                t = parseQuotedCharacter();
            } else if ('"' == x) {
                // QuotedStrings
                t = parseQuotedString();
            } else if ('{' == x) {
                // Comments
                parseComment();
            } else {
                // Miscellaneous
                t = parsePunctuation();
            }

            // Filter out invalid tokens
            t.ifPresent(retVal::add);
            t = Optional.empty();
        }

        // DEDENT back to scope 0 as needed at End Of Text
        parseIndents(retVal);

        return retVal;
    }

    private Optional<Token> parseWord() {
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
                return Optional.of(new Token(keywords.get(curWord), lineNumber, columnNumber));
            } else {
                // Name found
                return Optional.of(new Token(Token.TokenTypes.WORD, lineNumber, columnNumber, curWord));
            }
        } else {
            // Invalid token
            return Optional.empty();
        }
    }

    private Optional<Token> parseNumber() {
        boolean seenDecimal = false;
        char c = textManager.peekCharacter();
        StringBuilder currentWord = new StringBuilder();


        while (Character.isDigit(c) || '.' == c) {
            currentWord.append(lexerGetCharacter());

            if (textManager.isAtEnd()) break;

            // Handle 3.4.5 exception
            if (seenDecimal && '.' == c) {
                break;
            }
            if ('.' == c) {
                seenDecimal = true;
            }

            c = textManager.peekCharacter();
        }

        if (!currentWord.isEmpty()) {
            return Optional.of(new Token(Token.TokenTypes.NUMBER, lineNumber, columnNumber, currentWord.toString()));
        } else {
            return Optional.empty();
        }
    }

    private Optional<Token> parsePunctuation() {
        // textManager.peekCharacter() is stuck at the end of the text
        if (textManager.isAtEnd() && textManager.THROW_AWAY_CHAR == textManager.peekCharacter()) return Optional.empty();

        String smallOperator = String.valueOf(lexerGetCharacter());
        // Check if at end before getting bigger operator
        String bigOperator = textManager.isAtEnd() ? "" : smallOperator + textManager.peekCharacter();

        if (punctuation.containsKey(bigOperator)) {
            lexerGetCharacter();
            return Optional.of(new Token(punctuation.get(bigOperator), lineNumber, columnNumber));
        } else if (punctuation.containsKey(smallOperator)) {
            return Optional.of(new Token(punctuation.get(smallOperator), lineNumber, columnNumber));
        } else {
            return Optional.empty();
        }
    }

    // TextManager.getCharacter(), but it tracks line and col #
    private char lexerGetCharacter() {
        char c = textManager.getCharacter();
        columnNumber++;
        if ('\n' == c) {
            lineNumber++;
            columnNumber = 0;
        }
        return c;
    }

    private Optional<Token> parseQuotedCharacter() throws SyntaxErrorException {
        // e.g. 'A'
        lexerGetCharacter(); // consume the opening '\''
        String c = String.valueOf(lexerGetCharacter());
        if (lexerGetCharacter() != '\'') {
            throw new SyntaxErrorException("Unclosed char literal", lineNumber, columnNumber);
        }

        return Optional.of(new Token(Token.TokenTypes.QUOTEDCHARACTER, lineNumber, columnNumber, c));
    }

    private Optional<Token> parseQuotedString() throws SyntaxErrorException {
        lexerGetCharacter(); // consume the '\"'
        boolean isInQuote = true;
        StringBuilder currentWord = new StringBuilder();

        while (!textManager.isAtEnd()) {
            char c = lexerGetCharacter();
            if ('\"' == c) {
                isInQuote = false; // Begins as false
                break;
            }
            currentWord.append(c);
        }

        if (isInQuote) { // && textManager.isAtEnd()
            throw new SyntaxErrorException("Unclosed string literal", lineNumber, columnNumber);
        }

        return Optional.of(new Token(Token.TokenTypes.QUOTEDSTRING, lineNumber, columnNumber, currentWord.toString()));
    }

    private void parseComment() throws SyntaxErrorException {
        lexerGetCharacter(); // Consume the first '{'
        int closingBracesNeeded = 1;

        while (closingBracesNeeded > 0) {
            char c = lexerGetCharacter();
            if ('}' == c) closingBracesNeeded--;
            if ('{' == c) closingBracesNeeded++;

            if (textManager.isAtEnd() && closingBracesNeeded > 0) {
                throw new SyntaxErrorException("Unclosed comment", lineNumber, columnNumber);
            }
        }
    }

    private void parseIndents(LinkedList<Token> retVal) {
        int indentCount = countIndents();

        // Create INDENT's or DEDENT's until scope level is appropriate
        while (scopeLevel < indentCount) {
            scopeLevel++;
            retVal.add(new Token(Token.TokenTypes.INDENT, lineNumber, columnNumber));
        }
        while (scopeLevel > indentCount) {
            scopeLevel--;
            retVal.add(new Token(Token.TokenTypes.DEDENT, lineNumber, columnNumber));
        }
    }

    private int countIndents() {
        final int SPACES_PER_INDENT = 4;
        boolean isIndent;
        int indentCount = 0;

        // Parse a single indent
        do {
            char c = textManager.peekCharacter();

            if ('\t' == c) { // Check if tab character
                isIndent = true;
                lexerGetCharacter(); // Increment position
            } else if (' ' == c) { // Check if there are 4 spaces
                isIndent = true;
                for (int i = 0; i < SPACES_PER_INDENT; i++) {
                    c = textManager.peekCharacter();
                    if (textManager.isAtEnd() || c != ' ') {
                        isIndent = false;
                        break;
                    }
                    lexerGetCharacter(); // Increment position
                }
            }
            else {
                isIndent = false;
            }

            if (isIndent) {
                indentCount++;
            }
        } while (isIndent);

        return indentCount;
    }

    // EOC
}
