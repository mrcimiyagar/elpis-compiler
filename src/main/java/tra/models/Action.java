package tra.models;

import java_cup.runtime.Symbol;

import java.util.ArrayList;
import java.util.List;

public interface Action {

    public Object act(ArrayList<Object> args);
}
