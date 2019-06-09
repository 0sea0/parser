package translation;

import java.util.ArrayList;
import java.util.List;

public class Value {
    public static final int INT = 0;
    public static final int DEC = 1;
    private String val;
    private int type;
    private List<Quaternion> qList;

    public Value() {
        qList = new ArrayList<>();
    }

    public Value(String val, int type) {
        this.val = val;
        this.type = type;
        qList = new ArrayList<>();
    }

    public List<Quaternion> getqList() {
        return qList;
    }

    public void setqList(List<Quaternion> qList) {
        this.qList = qList;
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
