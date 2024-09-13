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
        char c = ' ';
        if (position + ahead < text.length()) { // Check for end before peeking
            c = text.charAt(position + ahead);
        }
        return c;
    }

    public char peekCharacter() {
        return peekCharacter(0);
    }
}
