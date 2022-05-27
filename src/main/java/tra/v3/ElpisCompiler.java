package tra.v3;

import java_cup.runtime.Symbol;
import tra.models.Action;
import tra.models.Pair;
import tra.models.sym;
import tra.models.temp.Point;
import tra.models.temp.Rule;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class ElpisCompiler {

    private TraLexer lexer;
    private Point root;

    public ElpisCompiler(TraLexer lexer) {
        this.lexer = lexer;
        this.root = new Point("root");
        Point exp = new Point("exp");
        this.root.connections.add(new Rule(
                Collections.singletonList(exp),
                new Action() {
                    @Override
                    public Object act(ArrayList<Object> args) {
                        return args.get(0);
                    }
                }));
    }

    public void parse() {
        List<Symbol> tokens = new ArrayList<>();
        Symbol token;
        try {
            while ((token = this.lexer.next_token()).value != null) tokens.add(token);
            tokens.add(new Symbol(sym.EOF, 0, 0, null));
        } catch (IOException ex) {
            ex.printStackTrace();
        }
        System.out.println(this.tryParse(tokens.toArray(new Symbol[0]), 0, root).second);
    }

    private Pair<Integer, Boolean> parseExp(Symbol[] tokens, int pointer, Point point) {
        return new Pair<>(0, false);
    }

    private Pair<Integer, Boolean> tryParse(Symbol[] tokens, int pointer, Point point) {
        return parseExp(tokens, pointer, point);
    }
}
