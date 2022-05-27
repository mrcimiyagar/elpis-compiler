package tra.models;

import java.io.Serializable;

public class Triple<A, B, C> implements Serializable {

    public A a;
    public B b;
    public C c;

    public Triple(A a, B b, C c) {
        this.a = a;
        this.b = b;
        this.c = c;
    }
}
