package lex;

public class ParseException extends Exception {
    private Token token;

    public ParseException(String message, Token token) {
        super(message);
        this.token = token;
    }

    public void printException() {
        System.err.println("ERROR: " + getMessage() + ":" + token.getStrVal() + "   第" + token.getRow() + "行,第" + token.getColumn() + "列");
    }

    public Token getToken() {
        return token;
    }
}
