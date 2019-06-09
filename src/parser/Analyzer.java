package parser;

import javafx.scene.control.Tab;
import lex.ParseException;
import lex.Scan;
import lex.Token;
import translation.Quaternion;
import translation.Table;
import translation.TableItem;
import translation.Value;

import java.util.ArrayList;
import java.util.List;

import static lex.Token.*;
import static parser.KeyWord.*;
import static parser.Operator.*;

public class Analyzer {
    private Scan scan;
    private Token lookahead;
    private int tc = 0;
    private Table table = Table.getInstance();
    List<Integer> args = null;
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
        table.levelDown();
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
        int typeName = TableItem.ERR;
        if (matchVal(INTEGER) || matchVal(FLOAT)) {
            if (matchVal(INTEGER)) typeName = TableItem.INT;
            else typeName = TableItem.DEC;
            getNextToken();
        } else {
            printError("错误的变量类型", true);
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
        String funcName = "";
        if (match(ID)) {
            funcName = lookahead.getStrVal();
            getNextToken();
        } else {
            printError("函数名应该是一个标识符", true);
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
        table.initFunArgs();
        List<Integer> list = paramList();
        if (!table.insert(funcName, TableItem.FUN, list)) {
            printError("存在同名标识符:" + lookahead.getStrVal(), false);
        }
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
            printError("缺少‘;’", false);
        }
        table.levelUp();
        varDecpart();
        procBody();
    }

    /***形参声明***/
    private List<Integer> paramList() {
        //ParamList→ ε| Param {';' Param}
        List<Integer> list = new ArrayList<>();
        if (matchVal(Operator.RP)) {
            //空
        } else {
            list = param();
            while (matchVal(F)) {
                getNextToken();
                list.addAll(param());
            }
        }
        return list;
    }

    private List<Integer> param() {
        //Param→ TypeName ID {',' ID}
        List<Integer> list = new ArrayList<>();
        int type = typeName();
        list.add(type);
        if (match(ID)) {
            if (!table.addArg(lookahead.getStrVal(),type)){
                printError("错误：存在同名参数'"+lookahead.getStrVal()+"'",true);
            }
            getNextToken();
        } else {
            printError("缺少一个标识符作为参数", true);
        }
        while (matchVal(Operator.D)) {
            getNextToken();
            if (match(ID)) {
                if (!table.addArg(lookahead.getStrVal(),type)){
                    printError("错误：存在同名参数'"+lookahead.getStrVal()+"'",true);
                }
                getNextToken();
                list.add(type);
            } else {
                printError("缺少一个标识符作为参数", true);
            }
        }
        return list;
    }

    /***过程体***/
    private void procBody() {
        //ProcBody→ 'begin' StmList 'end'
        if (matchVal(BEGIN)) {
            getNextToken();
        } else {
            if (match(ID)) {
                printError("期望‘begin’而不是" + lookahead.getStrVal(), false);
                getNextToken();
            } else {
                printError("缺少‘begin’", false);
            }
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
        Value rel = exp();
        System.out.println(new Quaternion("write", " ", " ", rel.getVal()));
    }

    private void CallStm() {
        // todo 需要添加四元式 函数调用 解决！
        //CallStm→ ID '(' ActParamList ')'
        match(ID);
        String funcName = lookahead.getStrVal();
        if (table.getType(funcName) == -1) {
            printError("未定义函数 " + funcName, false);
        }else{
            args = table.getArgsType();
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
        int type;
        if ((type = table.getType(var)) == -1) {
            printError("未定义变量 " + var, false);
        }
        getNextToken();
        if (matchVal(MOV)) {
            getNextToken();
        } else {
            printError("不是赋值语句", true);
        }
        Value rel = exp();
        System.out.println(new Quaternion("=", rel.getVal(), " ", var));
        if (type<rel.getType()){
            System.err.println("警告: float赋值给integer会造成精度丢失");
        }
    }

    private void conditionalStm() {
        // todo 需要添加四元式 条件语句 完成！
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
            int i = 0,k = args.size();
            Value arg = exp();
            if (arg.getType() != args.get(i++)){
                System.err.println("警告: 参数类型不匹配");
            }
            System.out.println(new Quaternion("push"," "," ",arg.getVal()));
            try {
                while (matchVal(D)) {
                    getNextToken();
                    arg = exp();
                    if (arg.getType() != args.get(i++)) {
                        System.err.println("警告: 参数类型不匹配");
                    }
                    System.out.println(new Quaternion("push", " ", " ", arg.getVal()));
                }
            }catch (IndexOutOfBoundsException e){
                printError("函数参数数量错误",true);
            }
            if (i!=k){
                printError("函数参数数量错误",true);
            }
//            } else
        }
    }

    /*** todo 四则表达式***/
    private Value exp() {
        //Exp→ Term {'+'|'-' Term}
        Value returnVal = term();
        String op;
        while (matchVal(ADD) || matchVal(SUB)) {
            op = lookahead.getStrVal();
            getNextToken();
            Value arg = term();
            String temp = getNextTemp();
            System.out.println(new Quaternion(op, returnVal.getVal(), arg.getVal(), temp));
            returnVal.setVal(temp);
            returnVal.setType(returnVal.getType()|arg.getType());
        }
        return returnVal;
    }

    private Value term() {
        //Term→ Factor {'*'|'/' Factor}
        Value returnVal = factor();
        String op;
        while (matchVal(MUL) || matchVal(DIV)) {
            op = lookahead.getStrVal();
            getNextToken();
            Value arg2 = factor();
            String temp = getNextTemp();
            System.out.println(new Quaternion(op, returnVal.getVal(), arg2.getVal(), temp));
            returnVal.setVal(temp);
            returnVal.setType(returnVal.getType()|arg2.getType());
        }
        return returnVal;
    }

    private Value factor() {
        //Factor→ ID | INTC | DECI | '(' Exp ')'
        Value result = null;
        int type;
        if (match(ID)) {
            if ((type = table.getType(lookahead.getStrVal())) == -1) {
                printError("未定义变量 " + lookahead.getStrVal(), false);
            }
            result = new Value(lookahead.getStrVal(),type);
            getNextToken();
        } else if (match(INTC)) {
            result = new Value(lookahead.getStrVal(),Value.INT);
            getNextToken();

        } else if (match(DECI)) {
            result = new Value(lookahead.getStrVal(),Value.DEC);
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
        Value exp1 = exp();
        String op = cmpOp();
        Value exp2 = exp();
        System.out.println(new Quaternion("j" + op, exp1.getVal(), exp2.getVal(), "____"));
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
