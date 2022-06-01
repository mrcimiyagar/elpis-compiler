package tra.v5;

import com.fasterxml.jackson.annotation.JsonIgnore;

import java.io.Serializable;

public class Tuple<A, B, C, D> implements Serializable {

    public A a;
    public B b;
    public C c;
    public D d;

    public Tuple(A a, B b, C c, D d) {
        this.a = a;
        this.b = b;
        this.c = c;
        this.d = d;
    }
}
