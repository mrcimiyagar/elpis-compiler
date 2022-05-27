package tra.models.temp;

public class Switch {
    public Point point;
    public int currentRuleIndex;
    public int currentRuleTokenIndex;
    public int startedAt;

    public Rule currentRule() {
        return this.point.connections.get(this.currentRuleIndex);
    }

    public Object currentRuleToken() {
        return this.currentRule().sentence.get(this.currentRuleTokenIndex);
    }

    public boolean reachedRuleEnd() {
        return this.currentRuleTokenIndex >= this.currentRule().sentence.size();
    }

    public boolean reachedRuleStorageEnd() {
        return this.currentRuleIndex >= this.point.connections.size();
    }
}
