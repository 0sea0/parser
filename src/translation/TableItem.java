package translation;

public class TableItem {
    /**
     * 函数名 0
     * 整型变量 1
     * 小数变量 2
     * 变量类型打错 -2
     */
    public static final int FUN = 0;
    public static final int INT = 1;
    public static final int DEC = 2;
    public static final int ERR = 2;
    private String name;
    private int type;
    private boolean used;

    public TableItem(String name, int type) {
        this.name = name;
        this.type = type;
        this.used = false;
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
