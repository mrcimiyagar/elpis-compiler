package tra.v5;

import java_cup.runtime.Symbol;

import java.io.Serializable;

public interface Action extends Serializable {

    public Object act(Symbol arg);
}
