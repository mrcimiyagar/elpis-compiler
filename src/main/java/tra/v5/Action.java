package tra.v5;

import java.io.Serializable;
import java.util.ArrayList;

public interface Action extends Serializable {

    public Object act(Object arg);
}
