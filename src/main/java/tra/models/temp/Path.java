package tra.models.temp;

import tra.models.Action;
import tra.models.Pair;
import tra.models.Triple;

import java.util.ArrayList;
import java.util.Stack;

public class Path {
    public Stack<Pair<Switch, ArrayList<Object>>> pathStack;

    public Path() {
        this.pathStack = new Stack<>();
    }

    public void addSwitch(Switch switchPoint) {
        this.pathStack.push(new Pair<>(switchPoint, new ArrayList<>()));
    }
}
