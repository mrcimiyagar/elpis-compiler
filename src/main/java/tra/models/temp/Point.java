package tra.models.temp;

import java.util.ArrayList;
import java.util.List;

public class Point {
    public String name;
    public ArrayList<Rule> connections;

    public Point(String name) {
        this.name = name;
        this.connections = new ArrayList<>();
    }
}
