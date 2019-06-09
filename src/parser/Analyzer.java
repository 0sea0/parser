package parser;

import lex.ParseException;
import lex.Scan;
import lex.Token;
import translation.*;

import java.util.ArrayList;
import java.util.LinkedList;
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
    List<Quaternion> qList = new ArrayList<>();
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
        Info info = stmList();
        for (Quaternion q :info.getQList()){
            System.out.println(q);
        }
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
    private Info stmList() {
        //StmList→ ε | Stm {';' Stm}
        Info info = null;
        if (matchVal(END) || matchVal(ELSE) || matchVal(FI) || matchVal(ENDWH)) {
            //kong
        } else {
            info = stm();
            while (matchVal(F)) {
                getNextToken();
                Info info2 = stm();
                info.getQList().addAll(info2.getQList());
            }
        }
//        if (matchVal(IF) || matchVal(WHILE) || matchVal(READ)
//                || matchVal(WRITE) || match(ID)) {
//
//        } else  else {
//            //出错
//        }
        return info;
    }

    /***语句***/
    private Info stm() {
        //Stm→ConditionalStm | LoopStm | InputStm | OutputStm | CallStm | AssignmentStm
        Info info = null;
        if (matchVal(IF)) {
            info = conditionalStm();
        } else if (matchVal(WHILE)) {
            info = loopStm();
        } else if (matchVal(READ)) {
            info = inputStm();
        } else if (matchVal(WRITE)) {
            info = outputStm();
        } else if (match(ID)) {
            if (table.getType(lookahead.getStrVal()) == TableItem.FUN) {
                info = CallStm();
            } else {
                info = assignmentStm();
            }
        } else {
            printError("无法识别由" + lookahead.getStrVal() + "构成的语句", true);
            System.exit(0);
        }
        return info;
    }

    private Info inputStm() {
        //InputStm→'read' ID
        Info info = new Info();
        matchVal(READ);
        getNextToken();
        if (match(ID)) {
            if (table.getType(lookahead.getStrVal()) == -1) {
                printError("未定义变量 " + lookahead.getStrVal(), false);
            }
            info.getQList().add(new Quaternion("read", " ", " ", lookahead.getStrVal()));
            getNextToken();
        } else {
            printError("缺少一个标识符", true);
        }
        return info;
    }

    private Info outputStm() {
        //todo 四元式的输出 解决!
        //OutputStm→'write' Exp
        Info info = new Info();
        matchVal(WRITE);
        getNextToken();
        Value rel = exp();
        for (Quaternion q : rel.getqList()){
            System.out.println(q);
        }
        info.getQList().add(new Quaternion("write", " ", " ", rel.getVal()));
        return info;
    }

    private Info CallStm() {
        // todo 需要添加四元式 函数调用 解决！
        //CallStm→ ID '(' ActParamList ')'
        Info info = new Info();
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
        info.getQList().add(new Quaternion("call", " ", " ", funcName));
        return info;
    }

    private Info assignmentStm() {
        // todo 需要添加四元式 赋值语句 解决!
        //AssignmentStm→ ID '=' Exp
        Info info = new Info();
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
        info.getQList().addAll(rel.getqList());
        info.getQList().add(new Quaternion("=", rel.getVal(), " ", var));
        if (type<rel.getType()){
            System.err.println("警告: float赋值给integer会造成精度丢失");
        }
        return info;
    }

    private Info conditionalStm() {
        // todo 需要添加四元式 条件语句 完成！
        //ConditionalStm→'if' ConditionalExp 'then' StmList 'else' StmList 'fi'
        matchVal(IF);//todo
        getNextToken();
        Info info1 = conditionalExp();
        if (matchVal(THEN)) {
            getNextToken();
        } else {
            printError("缺少‘then’", false);
            if (match(ID)) {
                getNextToken();
            }
        }
        Info info2 = stmList();
        info1.getQList().addAll(info2.getQList());
        if (matchVal(ELSE)) {
            getNextToken();
        } else {
            printError("缺少‘else’", false);
            if (match(ID)) {
                getNextToken();
            }
        }
        Quaternion q = new Quaternion("j", " ", " ",null);
        info2 = stmList();
        while (info1.getFirstFalseJmp()!=null){
            info1.getFirstFalseJmp().setResult(info2.getQList().get(0).getNum()+"");
        }
        q.setResult(info2.getQList().get(info2.getQList().size()-1).getNum()+1+"");
        info1.getQList().add(q);
        info1.getQList().addAll(info2.getQList());
        if (matchVal(FI)) {
            getNextToken();
        } else {
            printError("缺少‘fi’", false);
            if (match(ID)) {
                getNextToken();
            }
        }
        return info1;
    }

    private Info loopStm() {
        // todo 需要添加四元式 循环语句 完成！
        //LoopStm→'while' ConditionalExp 'do' StmList 'endwh'
        Info info = new Info();
        info.setBegin(Quaternion.getCount()+1);
        matchVal(WHILE);
        getNextToken();
        Info info1 = conditionalExp();
        if (matchVal(DO)) {
            getNextToken();
        } else {
            printError("缺少‘do’", false);
            if (match(ID)) {
                getNextToken();
            }
        }
        Info info2 = stmList();
        info1.getQList().addAll(info2.getQList());
        info1.getQList().add(new Quaternion("j", " ", " ", String.valueOf(info.getBegin())));
//        info.setEnd(Quaternion.getCount());
        while(info1.getFirstFalseJmp()!=null) {
            info1.getFirstFalseJmp().setResult(Quaternion.getCount() + 1 + "");
        }
//        for (Quaternion q : info1.getQList()){
//            System.out.println(q);
//        }
        if (matchVal(ENDWH)) {
            getNextToken();
        } else {
            printError("缺少‘endwh’", false);
            if (match(ID)) {
                getNextToken();
            }
        }
        return info1;
    }

    /***实参声明***/
    private Info actParamList() {
        //ActParamList→ ε | Exp {',' Exp}
        Info info = new Info();
        if (matchVal(RP)) {
            //kong
        } else {
//            if (match(ID) || match(INTC) || match(DECI) || matchVal(LP)) {
            int i = 0,k = args.size();
            Value arg = exp();
            for (Quaternion q : arg.getqList()){
                System.out.println(q);
            }
            if (arg.getType() != args.get(i++)){
                System.err.println("警告: 参数类型不匹配");
            }
            info.getQList().add(new Quaternion("push"," "," ",arg.getVal()));
            try {
                while (matchVal(D)) {
                    getNextToken();
                    arg = exp();
                    for (Quaternion q : arg.getqList()){
                        System.out.println(q);
                    }
                    if (arg.getType() != args.get(i++)) {
                        System.err.println("警告: 参数类型不匹配");
                    }
                    info.getQList().add(new Quaternion("push", " ", " ", arg.getVal()));
                }
            }catch (IndexOutOfBoundsException e){
                printError("函数参数数量错误",true);
            }
            if (i!=k){
                printError("函数参数数量错误",true);
            }
//            } else
        }
        return info;
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
            returnVal.getqList().addAll(arg.getqList());
            returnVal.getqList().add(new Quaternion(op, returnVal.getVal(), arg.getVal(), temp));
//            System.out.println(new Quaternion(op, returnVal.getVal(), arg.getVal(), temp));
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
            returnVal.getqList().addAll(arg2.getqList());
            returnVal.getqList().add(new Quaternion(op, returnVal.getVal(), arg2.getVal(), temp));
//            System.out.println(new Quaternion(op, returnVal.getVal(), arg2.getVal(), temp));
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
    private Info conditionalExp() {
        //ConditionalExp→RelationExp {'or' RelationExp}
        Info info1 = relationExp();
//        info1.getFirstTrueJmp().setResult(info.getEnd()+1+"");
        while (matchVal(OR)) {
            getNextToken();
            Info info2 = relationExp();
            info1.getQList().addAll(info2.getQList());
            info1.getFirstFalseJmp().setResult(info2.getQList().get(0).getNum()+"");
        }
        int end = info1.getQList().get(info1.getQList().size()-1).getNum();
        while(info1.getFirstTrueJmp()!=null) {
            info1.getFirstTrueJmp().setResult(end+1+"");
        }
        return info1;
    }

    private Info relationExp() {
        //RelationExp→ CompExp {'and' CompExp}
        Info info1= compExp();
        Info info2;
        while (matchVal(AND)) {
            getNextToken();
            info2 = compExp();
            info1.getQList().addAll(info2.getQList());
            info1.getFirstTrueJmp().setResult(info2.getQList().get(0).getNum()+"");
        }
        return info1;
    }

    private Info compExp() {
        //CompExp→ Exp CmpOp Exp
        Info info1 = new Info(Quaternion.getCount()+1);
        Value exp1 = exp();
        info1.getQList().addAll(exp1.getqList());
        String op = cmpOp();
        Value exp2 = exp();
        info1.getQList().addAll(exp2.getqList());
        info1.getQList().add(new Quaternion("j" + op, exp1.getVal(), exp2.getVal(), null));
        info1.getQList().add(new Quaternion("j", " ", " ", null));
        return info1;
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
