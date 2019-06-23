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
    private List<TableItem> funArgs;
    private List<Integer> funArgsType;

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
    public boolean insert(String name, int type, List<Integer> list) {
        TableItem item = new TableItem(name, type,list);
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
                if (item.getName().equals(name)) {
                    item.setUsed(true);
                    if (item.getType()==TableItem.FUN){
                        funArgsType = item.getArgs();
                    }
                    return item.getType();
                }
            }
        }
        return -1;
    }

    public void initFunArgs(){
        funArgs = new ArrayList<>();
    }

    public boolean addArg(String name,int type){
        TableItem item = new TableItem(name, type);
        if (funArgs.contains(item)) {
            return false;
        }
        funArgs.add(item);
        return true;
    }

    public List<Integer> getArgsType(){
        return funArgsType;
    }

    public void levelUp() {
        table.add(funArgs);
        level++;
//        System.err.println(level);
    }

    public void levelDown() {
        List<TableItem> t = table.get(level);
        for (TableItem item : t) {
            if (!item.isUsed()){
                if (item.getType() == TableItem.FUN){
                    System.err.println("警告：函数'"+item.getName()+"'未使用");
                }else{
                    System.err.println("警告：变量'"+item.getName()+"'未使用");
                }
            }
        }
        table.remove(level);
        level--;
//        System.err.println(level);
    }
}
