import java.util.List;
import java.util.Optional;

public class TokenManager {

    private final List<Token> tokens;
    private int position;

    public TokenManager(List<Token> tokens) {
        this.tokens = tokens;
    }

    public boolean done() {
        return tokens.size() == position;
    }

    public Optional<Token> matchAndRemove(Token.TokenTypes t) {
        if (!done()) {
            if (tokens.get(position).getType() == t) {
                return Optional.of(tokens.get(position++));
            }
        }
        return Optional.empty();
    }

    public Optional<Token> peek(int i) {
        if (done()) {
            return Optional.empty();
        }
        return Optional.of(tokens.get(position + i));
    }

    public boolean nextTwoTokensMatch(Token.TokenTypes first, Token.TokenTypes second) {
        boolean doesFirstMatch = false, doesSecondMatch = false;
        if (peek(0).isPresent()) { doesFirstMatch = peek(0).get().getType() == first; }
        if (peek(1).isPresent()) { doesSecondMatch = peek(1).get().getType() == second; }

        return doesFirstMatch && doesSecondMatch;
    }

    public int getCurrentLine() {

        // TODO: What if we call get_() while out of bounds?
        // Should this be handled? OR an error should be thrown?
        return tokens.get(position).getLineNumber();
    }

    public int getCurrentColumn() {
        return tokens.get(position).getColumnNumber();
    }
}
