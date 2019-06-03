package lex;

public class Token {
    private int type;
    private String strVal;
    private int row;
    private int column;

    public static final int ERROR = -1;//  错误
    public static final int EOF = 0;//  字符串结尾
    public static final int KEY = 1;
    public static final int ID = 2;
    public static final int INTC = 3;
    public static final int DECI = 4;
    public static final int OP = 5;
    public static final int DIVIDER = 6;
    public static final int NOTE = 7;
    private static final String[] s = {"\0", "保留字", "标识符", "无符号整数", "无符号小数", "运算符", "界符", "注释", "ERROR"};

    public Token() {
    }

    public Token(int row, int column) {
        this.row = row;
        this.column = column;
    }

    public int getType() {
        return type;
    }

    public void setType(int type) {
        this.type = type;
    }

    public String getStrVal() {
        return strVal;
    }

    public void setStrVal(String strVal) {
        // /r结尾的字符串后+字符串会让前面的字符串消失
        this.strVal = strVal.trim();
    }

    public int getRow() {
        return row;
    }

    public void setRow(int row) {
        this.row = row;
    }

    public int getColumn() {
        return column;
    }

    public void setColumn(int column) {
        this.column = column;
    }

    @Override
    public String toString() {
//        return strVal+ " " + type + " ( "+row+" : "+column+" )";
        return String.format("%-20s\t%-10s\t( %d , %d )", strVal, s[type], row, column);
    }
}
