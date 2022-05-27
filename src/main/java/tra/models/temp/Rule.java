package tra.models.temp;

import tra.models.Action;

import java.util.List;

public class Rule {

    private static long idCounter = 0;

    public long id;
    public List<Object> sentence;
    public Action action;

    public Rule(List<Object> sentence, Action action) {
        this.id = ++idCounter;
        this.sentence = sentence;
        this.action = action;
    }
}
