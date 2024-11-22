package Lexer;

public class TextManager {
    private final String text;
    private int position = 0;
    public final char THROW_AWAY_CHAR = '~';

    public TextManager(String text) {
        this.text = text;
    }

    // TODO: Refactor using Optional<char>
    public char getCharacter() {
        return text.charAt(position++);
    }

    public boolean isAtEnd() {
        return text.length() == position;
    }

    public char peekCharacter(int ahead) {
        // Check if peek-able before peeking
        int peekIndex = position + ahead;
        return peekIndex < text.length() ? text.charAt(peekIndex) : THROW_AWAY_CHAR;
    }

    public char peekCharacter() {
        return peekCharacter(0);
    }
}
