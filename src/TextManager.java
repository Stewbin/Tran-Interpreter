public class TextManager {
    private final String text;
    private int position = 0;

    public TextManager(String text) {
        this.text = text;
    }

    public char getCharacter() {
        return text.charAt(position++);
    }

    public boolean isAtEnd() {
        return text.length() == position;
    }

    public char peekCharacter(int ahead) {
        // Check if peek-able before peeking
        int peekIndex = position + ahead;
        return peekIndex < text.length() ? text.charAt(peekIndex) : '~';
    }

    public char peekCharacter() {
        return peekCharacter(0);
    }
}
