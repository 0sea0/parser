package translation;

public class Value {
    public static final int INT = 0;
    public static final int DEC = 1;
    private String val;
    private int type;

    public Value() {
    }

    public Value(String val, int type) {
        this.val = val;
        this.type = type;
    }

    public String getVal() {
        return val;
    }

    public void setVal(String val) {
        this.val = val;
    }

    public int getType() {
        return type;
    }

    public void setType(int type) {
        this.type = type;
    }

}
