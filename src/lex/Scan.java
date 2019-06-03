package lex;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;

public class Scan {
    private static int row = 1;
    private static int column = 0;
    private static final List<String> opList = Arrays.asList("==", "=", "<", "<=", ">", ">=", "<>", "+", "-", "*", "/");
    private static final List<String> keyList = Arrays.asList("program", "var", "integer", "float", "procedure", "begin",
            "end", "read", "write", "if", "then", "else", "fi", "while", "do", "endwh", "and", "or");
    private BufferedReader reader;
    private char ch = ' ';
    private Token token;
    private StringBuilder builder = null;

    public Scan(String file) {
        try {
            reader = new BufferedReader(new FileReader(file));
        } catch (FileNotFoundException e) {
            e.printStackTrace();
            System.out.println("没有找到该文件");
        }
    }

    private void getNextChar() {
        try {
            ch = (char) reader.read();
            column++;
            if (ch == '\n') {
                row++;
                column = 0;
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        if ((int) ch == 0xffff) {
            ch = '\0';
        }
    }

    public Token getNextToken() throws ParseException {
        builder = new StringBuilder();
        while (Character.isWhitespace(ch)) getNextChar();
        token = new Token(row, column);
        if (isLetter(ch) || ch == '_') {
            //单词
            while (isLetterOrDigit(ch) || ch == '_') {
                builder.append(ch);
                getNextChar();
            }
            String s = builder.toString();
            token.setStrVal(s);
            if (keyList.contains(s.toLowerCase())) {
                //是关键词
                token.setType(Token.KEY);
            } else {
                //普通单词
                token.setType(Token.ID);
                if (s.length() > 20) {
                    throw new ParseException("标识符长度不能超过20字符", token);
                }
            }
        } else if (Character.isDigit(ch) || ch == '.') {
            //数字
            while (isLetterOrDigit(ch) || ch == '.' || ch == '_') {
                builder.append(ch);
                getNextChar();
            }
            String s = builder.toString();
            token.setStrVal(s);
            if (isInteger(s)) {
                token.setType(Token.INTC);
                //todo 整数和小数数值的保存 可以设置两个不同成员或者字符串保存
//                token.setValue(Integer.parseInt(s));
            } else if (isDecimal(s)) {
                token.setType(Token.DECI);
//                token.setValue();
            } else {
                if (s.startsWith("0")){
                    if (s.contains(".")) token.setType(Token.DECI);
                    else token.setType(Token.INTC);
                    throw new ParseException("不能以0开头", token);
                }
                if (s.indexOf('.') != s.lastIndexOf('.')){
                    token.setType(Token.DECI);
                    throw new ParseException("包含多个小数点", token);
                }
                if (s.matches("\\.\\d+")){
                    token.setType(Token.DECI);
                    throw new ParseException("缺少整数部分", token);
                }
                if (s.matches("\\.\\w+") || s.matches("\\d+\\w+")){
                    token.setType(Token.ID);
                    throw new ParseException("错误的标识符", token);
                }
                if (s.equals(".")){
                    token.setType(Token.ERROR);
                    throw new ParseException("无法解析标志", token);
                }
                token.setType(Token.ERROR);
                throw new ParseException("错误", token);
            }
        } else if (ch == '/') {
            //注释或者除号
            builder.append(ch);
            getNextChar();
            //单行注释
            if (ch == '/') {
                while (!(ch == '\n' || ch == '\0')) {
                    builder.append(ch);
                    getNextChar();
                }
                token.setType(Token.NOTE);
                token.setStrVal(builder.toString());
            } else if (ch == '*') {
                //多行注释
                boolean flag = false;
                while (!(ch == '/' && flag)) {
                    if (flag) flag = false;
                    builder.append(ch);
                    if (ch == '*') flag = true;
                    if (ch == '\0') break;
                    getNextChar();
                }
                builder.append(ch);
                getNextChar();
                token.setType(Token.NOTE);
                token.setStrVal(builder.toString());
            } else {
                //除号
                token.setType(Token.OP);
                token.setStrVal(builder.toString());
            }
        } else if (isOperator(ch)) {
            //操作符
            while (isOperator(ch)) {
                builder.append(ch);
                getNextChar();
            }
            String s = builder.toString();
            token.setStrVal(s);
            if (opList.contains(s)) {
                token.setType(Token.OP);
            } else {
                token.setType(Token.OP);
                throw new ParseException("没有该操作符", token);
            }
        } else if (ch == '\0') {
            //EOF
            token.setType(Token.EOF);
        } else if (isDivider(ch)) {
            //界符
            token.setType(Token.DIVIDER);
            token.setStrVal(String.valueOf(ch));
            getNextChar();
        } else {
            //其他未使用到的符号
//            while(!(isDivider(ch)||Character.isWhitespace(ch))) {
//                builder.append(ch);
//                getNextChar();
//            }
            token.setType(Token.ERROR);
            token.setStrVal(String.valueOf(ch));
            getNextChar();
            throw new ParseException("非法字符", token);
        }
        return token;
    }

    private static boolean isDivider(char ch) {
        return ch == '(' || ch == ')' || ch == ',' || ch == ';';
    }

    private static boolean isOperator(char ch) {
        return ch == '=' || ch == '<' || ch == '>' || ch == '+' || ch == '-' || ch == '*' || ch == '/';
    }

    private static boolean isLetter(char ch) {
        return (ch >= 'a' && ch <= 'z') || (ch >= 'A' && ch <= 'Z');
    }

    private static boolean isLetterOrDigit(char ch) {
        return isLetter(ch) || (ch >= '0' && ch <= '9');
    }

    private static boolean isInteger(String s) {
        return s.matches("([1-9]\\d*)|0");
    }

    private static boolean isDecimal(String s) {
        return s.matches("(([1-9]\\d*)|0)\\.\\d*");
    }
}
