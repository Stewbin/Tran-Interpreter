package Parser;

import Lexer.Token;

import java.util.List;
import java.util.Optional;

public class TokenManager {

    private final List<Token> tokens;
    private int position;

    public TokenManager(List<Token> tokens) {
        this.tokens = tokens;
    }

    public boolean done() {
        return position == tokens.size();
    }

    public boolean isOnlyDedentsLeft() {
        return tokens.stream().skip(position+1).allMatch(token -> token.getType() == Token.TokenTypes.DEDENT);
    }

    public Optional<Token> matchAndRemove(Token.TokenTypes t) {
        if (!done()) {
            if (tokens.get(position).getType() == t) {
                return Optional.of(tokens.get(position++));
            }
//            System.out.printf("Expected a %s token, but received a %s\n", t, tokens.get(position));
        }
        return Optional.empty();
    }

    public Optional<Token> peek(int i) {
        if (position + i >= tokens.size())
            return Optional.empty();
        return Optional.of(tokens.get(position + i));
    }

    public boolean nextTwoTokensMatch(Token.TokenTypes first, Token.TokenTypes second) {
        boolean doesFirstMatch = false, doesSecondMatch = false;
        if (peek(0).isPresent()) { doesFirstMatch = peek(0).get().getType() == first; }
        if (peek(1).isPresent()) { doesSecondMatch = peek(1).get().getType() == second; }

        return doesFirstMatch && doesSecondMatch;
    }

    public int getCurrentLine() {
        if (done()) { return -1; } // Out of bounds handling
        return tokens.get(position).getLineNumber();
    }

    public int getCurrentColumnNumber() {
        if (done()) { return -1; } // Out of bounds handling
        return tokens.get(position).getColumnNumber();
    }
}
