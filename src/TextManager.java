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
        return text.charAt(position + ahead);
    }

    public char peekCharacter() {
        return text.charAt(position);
    }
}
