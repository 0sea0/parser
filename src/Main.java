import parser.Analyzer;

public class Main {

    public static void main(String[] args) {
        //从终端读取文件路径
/*        Scanner scanner = new Scanner(System.in);
        String filename = scanner.next();*/
        /*便于测试*/
        String filename = "input2.txt";
        Analyzer analyzer = new Analyzer(filename);
        analyzer.startAnalysis();
/*        Scan scan = new Scan(filename);
        Token token;
        while(true){
            try {
                token = scan.getNextToken();
                if (token.getType()==Token.EOF) break;
                //下一行是是否显示注释
//                if (token.getType() == lex.Token.NOTE) continue;
            } catch (ParseException e) {
                e.printException();
                continue;
            }
            System.out.println(token);
        }*/
    }
}
