package parser;

import lex.ParseException;
import lex.Scan;
import lex.Token;
import translation.Quaternion;
import translation.Table;
import translation.TableItem;

import static lex.Token.*;
import static parser.KeyWord.*;
import static parser.Operator.*;

public class Analyzer {
    private Scan scan;
    private Token lookahead;
    private int tc = 0;
    private Table table = Table.getInstance();

    public Analyzer(String filename) {
        this.scan = new Scan(filename);
    }

    private void printError(String msg, boolean exit) {
        System.err.println("错误：" + msg + "  在 ( " + lookahead.getRow() + " , " + lookahead.getColumn() + " )");
//        if (match(EOF)) {
        if (exit) System.exit(0);
    }

    //开始分析程序，对外的接口
    public void startAnalysis() {
        getNextToken();
        //总体表达式
        program();
        if (lookahead.getType() != EOF) {
            printError(lookahead.getStrVal(), true);
            System.exit(0);
        } else {
            System.out.println("分析完毕");
        }
    }

    /***总程序***/
    private void program() {
        //Program → ProgramHead VarDecpart ProgramBody
        programHead();
        varDecpart();
        programBody();
    }

    private void programHead() {
        //ProgramHead→ 'program' ID
        if (matchVal(PROGRAM)) {
            getNextToken();
        } else {
            printError("缺少关键词‘program’", false);
        }
        if (match(ID)) {
            getNextToken();
        } else {
            printError("程序名称应该是一个标识符", false);
            getNextToken();
        }
    }

    /***变量声明***/
    private void varDecpart() {
        //VarDecpart→ ε| 'var' VarDecList
        if (matchVal(VAR)) {
            getNextToken();
            varDecList();
        } else if (match(EOF) || matchVal(PROCEDURE) || matchVal(BEGIN)) {
            //空的时候求follow 什么也不做
        } else {

        }
    }

    private void varDecList() {
        //VarDecList→ VarIdList {VarIdList}
        varIdList();
        while (matchVal(INTEGER) || matchVal(FLOAT)) {
            varIdList();
        }
    }

    private void varIdList() {
        //VarIdList→ TypeName ID {',' ID} ';'
        int type = typeName();
        if (match(ID)) {
            if (!table.insert(lookahead.getStrVal(), type)) {
                printError("存在同名标识符：" + lookahead.getStrVal(), false);
            }
            getNextToken();
        } else {
            printError("缺少变量名", false);
            //没有变量名，可能是写的别的类型，也可能是写的，或者；忘记了变量名
            if (!(matchVal(Operator.D) || matchVal(F))) {
                getNextToken();
            }
        }
        while (matchVal(Operator.D)) {
            getNextToken();
            if (match(ID)) {
                if (!table.insert(lookahead.getStrVal(), type)) {
                    printError("存在同名标识符：" + lookahead.getStrVal(), false);
                }
                getNextToken();
            } else {
                printError("缺少变量名", false);
                //没有变量名，可能是写的别的类型，也可能是写的，或者；忘记了变量名
                if (!(matchVal(Operator.D) || matchVal(F))) {
                    getNextToken();
                }
            }
        }
        if (matchVal(F)) {
            getNextToken();
        } else {
            printError("缺少‘；’", false);
            //这里认为是忘记分号 所以不获取下一个token
        }
    }

    private int typeName() {
        //TypeName→'integer'| 'float'
        int typeName = -2;
        if (matchVal(INTEGER) || matchVal(FLOAT)) {
            if (matchVal(INTEGER)) typeName = 1;
            else typeName = 2;
            getNextToken();
        } else {
            printError("错误的变量类型", false);//fixme 再考虑
            getNextToken();
        }
        return typeName;
    }

    /***过程声明***/
    private void programBody() {
        //ProgramBody→ε| ProcDec {ProcDec}
        if (match(EOF)) {
            //ε
        } else {
            procDec();
            while (matchVal(PROCEDURE)) {
                procDec();
            }
        }
    }

    private void procDec() {
        //ProcDec→ 'procedure' ID '(' ParamList ')' ';' VarDecpart ProcBody
        if (matchVal(PROCEDURE)) {
            getNextToken();
        } else {
            if (match(ID)) {//认为拼错了
                printError("期待‘procedure’而不是" + lookahead.getStrVal(), false);
                getNextToken();
            } else {
                printError("缺少关键词‘procedure’", true);
            }
        }
        if (match(ID)) {
            if (!table.insert(lookahead.getStrVal(), TableItem.FUN)) {
                printError("存在同名标识符:" + lookahead.getStrVal(), false);
            }
            getNextToken();
        } else {
            printError("函数名应该是一个标识符", false);//fixme 再考虑 2
            if (!matchVal(Operator.LP)) {
                getNextToken();
            }
        }
        if (matchVal(Operator.LP)) {
            getNextToken();
        } else {
            printError("缺少‘(’", false);
            if (!matchVal(INTEGER) && !matchVal(FLOAT)) {
                getNextToken();
            }
        }
        paramList();
        if (matchVal(Operator.RP)) {
            getNextToken();
        } else {
            printError("缺少‘)’", false);
            if (!matchVal(INTEGER) && !matchVal(FLOAT)) {
                getNextToken();
            }
        }
        if (matchVal(F)) {
            getNextToken();
        } else {
            printError("缺少‘,’", false);
        }
        varDecpart();
        procBody();
    }

    /***形参声明***/
    private void paramList() {
        //ParamList→ ε| Param {';' Param}
        if (matchVal(Operator.RP)) {
            //空
        } else {
            param();
            while (matchVal(F)) {
                getNextToken();
                param();
            }
        }
    }

    private void param() {
        //Param→ TypeName ID {',' ID}
        typeName();
        if (match(ID)) {
            getNextToken();
        } else {
            printError("缺少一个标识符作为变量", true);
        }
        while (matchVal(Operator.D)) {
            getNextToken();
            if (match(ID)) {
                getNextToken();
            } else {
                printError("缺少一个标识符作为变量", true);
            }
        }
    }

    /***过程体***/
    private void procBody() {
        //ProcBody→ 'begin' StmList 'end'
        if (matchVal(BEGIN)) {
            table.levelUp();
            getNextToken();
        } else {
            if (match(ID)) {
                printError("期望‘begin’而不是" + lookahead.getStrVal(), false);
                table.levelUp();
                getNextToken();
            } else {
                printError("缺少‘begin’", false);
            }
            table.levelUp();
        }
        stmList();

        if (matchVal(END)) {
            table.levelDown();
            getNextToken();
        } else {
            if (match(ID)) {
                printError("期望‘end’而不是" + lookahead.getStrVal(), false);
                getNextToken();
            } else {
                printError("缺少‘end’", false);
            }
            table.levelDown();
        }
    }

    /***语句***/
    private void stmList() {
        //StmList→ ε | Stm {';' Stm}
        if (matchVal(END) || matchVal(ELSE) || matchVal(FI) || matchVal(ENDWH)) {
            //kong
        } else {
            stm();
            while (matchVal(F)) {
                getNextToken();
                stm();
            }
        }
//        if (matchVal(IF) || matchVal(WHILE) || matchVal(READ)
//                || matchVal(WRITE) || match(ID)) {
//
//        } else  else {
//            //出错
//        }
    }

    /***语句***/
    private void stm() {
        //Stm→ConditionalStm | LoopStm | InputStm | OutputStm | CallStm | AssignmentStm
        if (matchVal(IF)) {
            conditionalStm();
        } else if (matchVal(WHILE)) {
            loopStm();
        } else if (matchVal(READ)) {
            inputStm();
        } else if (matchVal(WRITE)) {
            outputStm();
        } else if (match(ID)) {
            if (table.getType(lookahead.getStrVal()) == TableItem.FUN) {
                CallStm();
            } else {
                assignmentStm();
            }
        } else {
            printError("无法识别由" + lookahead.getStrVal() + "构成的语句", true);
            System.exit(0);
        }
    }

    private void inputStm() {
        //InputStm→'read' ID
        matchVal(READ);
        getNextToken();
        if (match(ID)) {
            if (table.getType(lookahead.getStrVal()) == -1) {
                printError("未定义变量 " + lookahead.getStrVal(), false);
            }
            System.out.println(new Quaternion("read", " ", " ", lookahead.getStrVal()));
            getNextToken();
        } else {
            printError("缺少一个标识符", true);
        }
    }

    private void outputStm() {
        //todo 四元式的输出 解决!
        //OutputStm→'write' Exp
        matchVal(WRITE);
        getNextToken();
        String rel = exp();
        System.out.println(new Quaternion("write", " ", " ", rel));
    }

    private void CallStm() {
        // todo 需要添加四元式 函数调用 解决！
        //CallStm→ ID '(' ActParamList ')'
        match(ID);
        String funcName = lookahead.getStrVal();
        if (table.getType(funcName) == -1) {
            printError("未定义函数 " + funcName, false);
        }
        getNextToken();
        if (matchVal(LP)) {
            getNextToken();
        } else {
            printError("缺少‘(’", false);
        }
        actParamList();
        if (matchVal(RP)) {
            getNextToken();
        } else {
            printError("缺少‘)’", false);
        }
        System.out.println(new Quaternion("call", " ", " ", funcName));
    }

    private void assignmentStm() {
        // todo 需要添加四元式 赋值语句 解决!
        //AssignmentStm→ ID '=' Exp
        match(ID);
        String var = lookahead.getStrVal();
        if (table.getType(var) == -1) {
            printError("未定义变量 " + var, false);
        }
        getNextToken();
        if (matchVal(MOV)) {
            getNextToken();
        } else {
            printError("不是赋值语句", true);
        }
        String rel = exp();
        System.out.println(new Quaternion("=", rel, " ", var));
    }

    private void conditionalStm() {
        // todo 需要添加四元式 条件语句
        //ConditionalStm→'if' ConditionalExp 'then' StmList 'else' StmList 'fi'
        matchVal(IF);//todo
        getNextToken();
        conditionalExp();
        if (matchVal(THEN)) {
            getNextToken();
        } else {
            printError("缺少‘then’", false);
            if (match(ID)) {
                getNextToken();
            }
        }
        stmList();
        if (matchVal(ELSE)) {
            System.out.println(new Quaternion("j", " ", " ", "___"));
            getNextToken();
        } else {
            printError("缺少‘else’", false);
            if (match(ID)) {
                getNextToken();
            }
        }
        stmList();
        if (matchVal(FI)) {
            getNextToken();
        } else {
            printError("缺少‘fi’", false);
            if (match(ID)) {
                getNextToken();
            }
        }
    }

    private void loopStm() {
        // todo 需要添加四元式 循环语句 完成！
        //LoopStm→'while' ConditionalExp 'do' StmList 'endwh'
        matchVal(WHILE);
        getNextToken();
        conditionalExp();
        if (matchVal(DO)) {
            getNextToken();
        } else {
            printError("缺少‘do’", false);
            if (match(ID)) {
                getNextToken();
            }
        }
        stmList();
        System.out.println(new Quaternion("j", " ", " ", "___"));
        if (matchVal(ENDWH)) {
            getNextToken();
        } else {
            printError("缺少‘endwh’", false);
            if (match(ID)) {
                getNextToken();
            }
        }
    }

    /***实参声明***/
    private void actParamList() {
        //ActParamList→ ε | Exp {',' Exp}
        if (matchVal(RP)) {
            //kong
        } else {
//            if (match(ID) || match(INTC) || match(DECI) || matchVal(LP)) {
            String arg = exp();
            System.out.println(new Quaternion("push"," "," ",arg));
            while (matchVal(D)) {
                getNextToken();
                arg = exp();
                System.out.println(new Quaternion("push"," "," ",arg));
            }
//            } else
        }
    }

    /*** todo 四则表达式***/
    private String exp() {
        //Exp→ Term {'+'|'-' Term}
        String returnVal = term();
        String op;
        while (matchVal(ADD) || matchVal(SUB)) {
            op = lookahead.getStrVal();
            getNextToken();
            String arg = term();
            String temp = getNextTemp();
            System.out.println(new Quaternion(op, returnVal, arg, temp));
            returnVal = temp;
        }
        return returnVal;
    }

    private String term() {
        //Term→ Factor {'*'|'/' Factor}
        String returnVal = factor();
        String op;
        while (matchVal(MUL) || matchVal(DIV)) {
            op = lookahead.getStrVal();
            getNextToken();
            String arg2 = factor();
            String temp = getNextTemp();
            System.out.println(new Quaternion(op, returnVal, arg2, temp));
            returnVal = temp;
        }
        return returnVal;
    }

    private String factor() {
        //Factor→ ID | INTC | DECI | '(' Exp ')'
        String result = null;
        if (match(ID)) {
            if (table.getType(lookahead.getStrVal()) == -1) {
                printError("未定义变量 " + lookahead.getStrVal(), false);
            }
            result = lookahead.getStrVal();
            getNextToken();
        } else if (match(INTC)) {
            result = lookahead.getStrVal();
            getNextToken();

        } else if (match(DECI)) {
            result = lookahead.getStrVal();
            getNextToken();

        } else if (matchVal(LP)) {
            getNextToken();
            result = exp();
            if (matchVal(RP)) {
                getNextToken();
            } else {
                printError("缺少‘)’", false);
            }
        }//else
        return result;
    }

    /*** todo 条件表达式***/
    private void conditionalExp() {
        //ConditionalExp→RelationExp {'or' RelationExp}
        relationExp();
        while (matchVal(OR)) {
            getNextToken();
            relationExp();
        }
    }

    private void relationExp() {
        //RelationExp→ CompExp {'and' CompExp}
        compExp();
        while (matchVal(AND)) {
            getNextToken();
            compExp();
        }
    }

    private void compExp() {
        //CompExp→ Exp CmpOp Exp
        String exp1 = exp();
        String op = cmpOp();
        String exp2 = exp();
        System.out.println(new Quaternion("j" + op, exp1, exp2, "____"));
        System.out.println(new Quaternion("j", " ", " ", "____"));
    }

    private String cmpOp() {
        //CmpOp→'<' | '<=' | '>' | '>=| '==' | '<>'
        String op = null;
//        if (matchVal(LT) || matchVal(LE) || matchVal(GT) || matchVal(GE) || matchVal(EQ) || matchVal(NE))
        if (match(OP)) {
            op = lookahead.getStrVal();
            getNextToken();
        } else {
            printError("缺少运算符", true);
        }
        return op;
    }

    //比较lookahead的类型
    private boolean match(int type) {
        return lookahead.getType() == type;
    }

    //比较lookahead的内容
    private boolean matchVal(String val) {
        if (lookahead.getType() == KEY) return val.equals(lookahead.getStrVal().toLowerCase());
        return val.equals(lookahead.getStrVal());
    }

    //对词法分析器的getNextToken()进行一次封装
    private void getNextToken() {
        try {
            do {
                lookahead = scan.getNextToken();
            } while (lookahead.getType() == NOTE);
        } catch (ParseException e) {
            e.printException();
            lookahead = e.getToken();
        }
    }

    private String getNextTemp() {
        return "t" + tc++;
    }
}
