package tra.v5;

import tra.models.Triple;

import java.io.Serializable;
import java.util.LinkedHashMap;

public class Node implements Serializable {
    public String name;
    public LinkedHashMap<String, Tuple<StackActions, Action, Node, Node>> nextTable;
    public Node(String name) {
        this.name = name;
        this.nextTable = new LinkedHashMap<>();
    }
    public Node addRule(String trigger, StackActions sa, Action action, Node next) {
        this.nextTable.put(trigger, new Tuple<>(sa, action, next, this));
        return this;
    }
}
