package translation;

/**
 * 四元式类
 */
public class Quaternion {

    private static int count = -1;
    private String op;
    private String arg1;
    private String arg2;
    private String result;

    public Quaternion() {
        count++;
    }

    public Quaternion(String op, String arg1, String arg2, String result) {
        this();
        this.op = op;
        this.arg1 = arg1;
        this.arg2 = arg2;
        this.result = result;
    }

    @Override
    public String toString() {
        return count + "  ( " + op + " , " + arg1 + " , " + arg2 + " , " + result + " )";
    }
}
