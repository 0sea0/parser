package lex;

public class ParseException extends Exception {
    private Token token;

    public ParseException(String message, Token token) {
        super(message);
        this.token = token;
    }

    public void printException() {
        System.err.println("错误: " + getMessage() + ":" + token.getStrVal() + "   在 ( " + token.getRow() + " , " + token.getColumn() + " )");
    }

    public Token getToken() {
        return token;
    }
}
