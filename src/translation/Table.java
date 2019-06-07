package translation;

import java.util.ArrayList;
import java.util.List;

/**
 * 符号表
 */
public class Table {
    private int level = 0;
    private List<List<TableItem>> table;
    private static Table instance = null;

    public static Table getInstance() {
        if (instance == null) instance = new Table();
        return instance;
    }

    private Table() {
        table = new ArrayList<>();
        table.add(new ArrayList<>());
    }

    public boolean insert(String name, int type) {
        TableItem item = new TableItem(name, type);
        List<TableItem> t = table.get(level);
        if (t.contains(item)) {
            return false;
        }
        t.add(item);
        return true;
    }

    public int getType(String name) {
        for (int i = level; i >= 0; i--) {
            List<TableItem> t = table.get(i);
            for (TableItem item : t) {
                if (item.getName().equals(name)) return item.getType();
            }
        }
        return -1;
    }

    public void levelUp() {
        table.add(new ArrayList<>());
        level++;
    }

    public void levelDown() {
        table.remove(level);
        level--;
    }
}
