package parser;

import lex.ParseException;
import lex.Scan;
import lex.Token;

import static lex.Token.*;
import static parser.KeyWord.*;
import static parser.Operator.*;

public class Analyzer {
    private Scan scan;
    private Token lookahead;
    StringBuilder buff;
    public Analyzer(String filename) {
        this.scan = new Scan(filename);
    }
    private void printError(String msg){
        System.out.println(msg);
        if (match(EOF)){
            System.exit(0);
        }
    }
    private void printBuff(){
        System.out.println(buff);
        buff.delete(0,buff.length());
    }
    //开始分析程序，对外的接口
    public void startAnalysis() {
        getNextToken();
        //总体表达式
        program();
        if (lookahead.getType() != EOF) {
            System.err.println("出错啦：" + lookahead.getStrVal());
            System.exit(0);
        }else{
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
            System.err.println("期待关键词‘program’");
        }
        if (match(ID)) {
            getNextToken();
        } else {
            System.err.println("期待一个程序名");
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
        typeName();
        if (match(ID)) {
            getNextToken();
        } else {
            System.err.println("期待变量名");
            //没有变量名，可能是写的别的类型，也可能是写的，或者；忘记了变量名
            if (!(matchVal(Operator.D)||matchVal(F))){
                getNextToken();
            }
        }
        while (matchVal(Operator.D)) {
            getNextToken();
            if (match(ID)) {
                getNextToken();
            } else {
                System.err.println("期待变量名");
                //没有变量名，可能是写的别的类型，也可能是写的，或者；忘记了变量名
                if (!(matchVal(Operator.D)||matchVal(F))){
                    getNextToken();
                }
            }
        }
        if (matchVal(F)){
            getNextToken();
        }else {
            System.err.println("期待‘；’");
            //todo 这里认为是忘记分号 所以不获取下一个token
        }
    }

    private void typeName() {
        //TypeName→'integer'| 'float'
        if (matchVal(INTEGER) || matchVal(FLOAT)) {
            getNextToken();
        } else if (match(ID)){
            System.err.println("没有该变量类型");
        } else {
            System.err.println("期待一个变量类型");
        }
    }

    /***过程声明***/
    private void programBody() {
        //ProgramBody→ε| ProcDec {ProcDec}
        if (match(EOF)) {
            //ε
        }else{
            procDec();
            while (matchVal(PROCEDURE)) {
                procDec();
            }
        }
    }

    private void procDec() {
        //ProcDec→ 'procedure' ID '(' ParamList ')' ';' VarDecpart ProcBody
        if (matchVal(PROCEDURE)){
            getNextToken();
        }else{
            if (match(ID)){//认为拼错了
                System.err.println("期待‘procedure’而不是"+lookahead.getStrVal());
                getNextToken();
            }else{
                System.err.println("缺少关键词‘procedure’");
            }
        }
        if (match(ID)){
            getNextToken();
        }else{
            System.err.println("期待一个标识符作为函数名");
            if (!matchVal(Operator.LP)){
                getNextToken();
            }
        }
        if (matchVal(Operator.LP)){
            getNextToken();
        }else{
            System.err.println("缺少‘（’");
            if (!matchVal(INTEGER) && !matchVal(FLOAT)) {
                getNextToken();
            }
        }
        paramList();
        if (matchVal(Operator.RP)){
            getNextToken();
        }else{
            System.err.println("缺少‘)’");
            if (!matchVal(INTEGER) && !matchVal(FLOAT)) {
                getNextToken();
            }
        }
        if (matchVal(F)){
            getNextToken();
        }else {
            System.err.println("缺少‘；’");
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
        if (match(ID)){
            getNextToken();
        }else{
            System.err.println("缺少一个标识符作为变量");
        }
        while (matchVal(Operator.D)) {
            getNextToken();
            if (match(ID)){
                getNextToken();
            }else{
                System.err.println("缺少一个标识符作为变量");
            }
        }
    }

    /***过程体***/
    private void procBody() {
        //ProcBody→ 'begin' StmList 'end'
        if (matchVal(BEGIN)){
            getNextToken();
        }else {
            if (match(ID)){
                System.err.println("期望‘begin’而不是"+lookahead.getStrVal());
                getNextToken();
            }else{
                System.err.println("缺少‘begin’");
            }
        }
        stmList();

        if (matchVal(END)){
            getNextToken();
        }else {
            if (match(ID)){
                System.err.println("期望‘end’而不是"+lookahead.getStrVal());
                getNextToken();
            }else{
                System.err.println("缺少‘end’");
            }
        }
    }

    /***语句***/
    private void stmList() {
        //StmList→ ε | Stm {';' Stm}
        if (matchVal(END) || matchVal(ELSE) || matchVal(FI) || matchVal(ENDWH)) {
            //kong
        }else{
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
            getNextToken();
            if (matchVal(LP)) {
                CallStm();
            } else if (matchVal(MOV)) {
                assignmentStm();
            } else {
                System.err.println("缺少'='或‘(’");
                //todo 在实验3有了符号表可以判断ID是变量名还是函数调用
            }
        } else {
            System.err.println("无法识别由"+lookahead.getStrVal()+"构成的语句");
            System.exit(0);
        }
    }

    private void inputStm() {
        //InputStm→'read' ID
        if (matchVal(READ)){
            getNextToken();
        }else{
            System.err.println("缺少‘read’");
        }
        if (match(ID)){
            getNextToken();
        }else{
            System.err.println("缺少一个标识符");
        }
    }

    private void outputStm() {
        //OutputStm→'write' Exp
        if (matchVal(WRITE)){
            getNextToken();
        }else{
            System.err.println("缺少‘write’");
        }
        exp();
    }

    private void CallStm() {
        //CallStm→ ID '(' ActParamList ')'
        //match(ID); 前面匹配过了
        if (matchVal(LP)){
            getNextToken();
        }else{
            System.err.println("缺少‘（’");
        }
        actParamList();
        if (matchVal(RP)){
            getNextToken();
        }else {
            System.err.println("缺少‘）’");
        }

    }

    private void assignmentStm() {
        //AssignmentStm→ ID '=' Exp
        //match(ID); 前面匹配过了
        matchVal(MOV);
        getNextToken();
        exp();
    }

    private void conditionalStm() {
        //ConditionalStm→'if' ConditionalExp 'then' StmList 'else' StmList 'fi'
        matchVal(IF);//todo
        getNextToken();
        conditionalExp();
        if (matchVal(THEN)){
            getNextToken();
        }else{
            System.err.println("缺少‘then’");
            if (match(ID)){
                getNextToken();
            }
        }
        stmList();
        if (matchVal(ELSE)){
            getNextToken();
        }else{
            System.err.println("缺少‘else’");
            if (match(ID)){
                getNextToken();
            }
        }
        stmList();
        if (matchVal(FI)){
            getNextToken();
        }else{
            System.err.println("缺少‘fi’");
            if (match(ID)){
                getNextToken();
            }
        }
    }

    private void loopStm() {
        //LoopStm→'while' ConditionalExp 'do' StmList 'endwh'
        matchVal(WHILE);
        getNextToken();
        conditionalExp();
        if (matchVal(DO)){
            getNextToken();
        }else{
            System.err.println("缺少‘do’");
            if (match(ID)){
                getNextToken();
            }
        }
        stmList();
        if (matchVal(ENDWH)){
            getNextToken();
        }else{
            System.err.println("缺少‘endwh’");
            if (match(ID)){
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
            exp();
            while (matchVal(D)) {
                getNextToken();
                exp();
            }
//            } else
        }
    }

    /***四则表达式***/
    private void exp() {
        //Exp→ Term {'+'|'-' Term}
        term();
        while (matchVal(ADD) || matchVal(SUB)) {
            getNextToken();
            term();
        }
    }

    private void term() {
        //Term→ Factor {'*'|'/' Factor}
        factor();
        while (matchVal(MUL) || matchVal(DIV)) {
            getNextToken();
            factor();
        }
    }

    private void factor() {
        //Factor→ ID | INTC | DECI | '(' Exp ')'
        if (match(ID)) {
            getNextToken();

        } else if (match(INTC)) {
            getNextToken();

        } else if (match(DECI)) {
            getNextToken();

        } else if (matchVal(LP)) {
            getNextToken();
            exp();
            if (matchVal(RP)){
                getNextToken();
            }else{
                System.err.println("缺少‘）’");
            }
        }
    }

    /***条件表达式***/
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
        exp();
        cmpOp();
        exp();
    }

    private void cmpOp() {
        //CmpOp→'<' | '<=' | '>' | '>=| '==' | '<>'
        if (matchVal(LT) || matchVal(LE) || matchVal(GT) || matchVal(GE) || matchVal(EQ) || matchVal(NE)) {
            getNextToken();
        } else {
            System.err.println("缺少运算符");
        }
    }

    //比较lookahead的类型
    private boolean match(int type) {
        return lookahead.getType() == type;
    }

    //比较lookahead的内容
    private boolean matchVal(String val) {
        if (lookahead.getType()==KEY) return val.equals(lookahead.getStrVal().toLowerCase());
        return val.equals(lookahead.getStrVal());
    }

    //对词法分析器的getNextToken()进行一次封装
    private void getNextToken() {
        try {
            do {
                lookahead = scan.getNextToken();
            }while(lookahead.getType()==NOTE);
        } catch (ParseException e) {
            e.printStackTrace();
            lookahead = e.getToken();
        }
    }
}
