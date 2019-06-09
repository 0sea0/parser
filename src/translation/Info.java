package translation;

import java.util.ArrayList;
import java.util.List;

public class Info {
    private int begin;
    private int end;
    private List<Quaternion> qList;
    private int Tptr;
    private int Fptr;
    public Info() {
        qList = new ArrayList<>();
        Tptr = Fptr = 0;
    }

    public Info(int begin) {
        this();
        this.begin = begin;
    }

    public List<Quaternion> getQList() {
        return qList;
    }

    public void setQList(List<Quaternion> qList) {
        this.qList = qList;
    }

    public int getBegin() {
        return begin;
    }

    public void setBegin(int begin) {
        this.begin = begin;
    }

    public int getEnd() {
        return end;
    }

    public void setEnd(int end) {
        this.end = end;
    }

    public Quaternion getFirstTrueJmp(){
        Quaternion q = null;
        for (int i = Tptr;i<qList.size();i++){
            Quaternion p = qList.get(i);
            if (p.getResult()==null&&p.getOp().matches("j.+")){
                q = p;
                Tptr = i;
                break;
            }
        }
        return q;
    }
    public Quaternion getFirstFalseJmp(){
        Quaternion q = null;
        for (int i = Fptr;i<qList.size();i++){
            Quaternion p = qList.get(i);
            if (p.getResult()==null&&p.getOp().equals("j")){
                q = p;
                Fptr = i;
                break;
            }
        }
        return q;
    }
}
