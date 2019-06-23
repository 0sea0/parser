package translation;

import java.util.List;

public class TableItem {
    public static final int FUN = 2;//变量名
    public static final int INT = 0;//整型变量
    public static final int DEC = 1;//小数变量
    public static final int ERR = -2;//变量类型打错
    private String name;
    private int type;
    private boolean used;
    private List<Integer> args;

    public TableItem(String name, int type) {
        this.name = name;
        this.type = type;
        this.used = false;
    }

    public TableItem(String name, int type, List<Integer> args) {
        this(name, type);
        this.args = args;
    }

    public List<Integer> getArgs() {
        return args;
    }

    public void setArgs(List<Integer> args) {
        this.args = args;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public int getType() {
        return type;
    }

    public void setType(int type) {
        this.type = type;
    }

    public boolean isUsed() {
        return used;
    }

    public void setUsed(boolean used) {
        this.used = used;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == null) return false;
        if (!(obj instanceof TableItem)) return false;
        return name.equals(((TableItem) obj).name);
    }
}
