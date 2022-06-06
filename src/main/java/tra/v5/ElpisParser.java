package tra.v5;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import java_cup.runtime.Symbol;
import tra.helpers.JsonHelper;
import tra.models.*;
import tra.models.temp.Path;
import tra.models.temp.Point;

import java.io.*;
import java.util.*;

public class ElpisParser implements Serializable {
    public static List<FileDeps> allDependencies = new ArrayList<>();
    public static List<Codes.Code> clientSideFunctions = new ArrayList<>();
    public static HashSet<String> processedFilesSet = new HashSet<>();
    private ElpisLexer lexer;
    private Point root;
    private Path path;
    private List<String> dependencies;
    private String fileName;
    private String filePath;
    private Symbol[] tokens;
    private final Node tree;
    private static byte[] objToByte(Node tcpPacket) throws IOException {
        ByteArrayOutputStream byteStream = new ByteArrayOutputStream();
        ObjectOutputStream objStream = new ObjectOutputStream(byteStream);
        objStream.writeObject(tcpPacket);

        return byteStream.toByteArray();
    }
    private static Object byteToObj(byte[] bytes) throws IOException, ClassNotFoundException {
        ByteArrayInputStream byteStream = new ByteArrayInputStream(bytes);
        try (ObjectInputStream objStream = new ObjectInputStream(byteStream)) {
            return objStream.readObject();
        }
    }
    private static Node cloneNode(Node node) {
        byte[] jsonTemp = new byte[0];
        try {
            jsonTemp = objToByte(node);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        Node clonedNode = null;
        try {
            clonedNode = (Node) byteToObj(jsonTemp);
        } catch (IOException | ClassNotFoundException e) {
            throw new RuntimeException(e);
        }
        return clonedNode;
    }
    private void print(Object obj) {
        Gson gson = new GsonBuilder().setPrettyPrinting().create();
        JsonParser jp = new JsonParser();
        JsonElement je = jp.parse(JsonHelper.toJson(obj));
        String buildTree = gson.toJson(je);
        System.out.println(buildTree);
    }
    List<List<Object>> dataStack = new ArrayList<>();
    static class Ast implements Serializable {
        public String key;
        public String title;
        @JsonIgnore
        public Ast parent;
        public Object value;
        //@JsonIgnore
        public LinkedHashMap<String, Object> tempMem;
        public LinkedHashMap<String, Object> tree;
        Ast(String title, Object value) {
            this.title = title;
            this.value = value;
            this.tree = new LinkedHashMap<>();
            this.tempMem = new LinkedHashMap<>();
        }
        public void putChildAst(String key, Ast ast) {
            ast.key = key;
            ast.parent = this;
            this.tree.put(key, ast);
        }
        public void detachChild(String key) {
            Ast detached = (Ast) this.tree.remove(key);
            detached.parent = null;
        }
        public void memorizeTemp(String key, Object temp) {
            this.tempMem.put(key, temp);
        }
    }
    private final Ast rootAst;
    private Ast astTree;
    private Ast tempPoint;
    private final Stack<Pair<ArrayList<Ast>, Ast>> scopeStack;
    private Node constructExpression(String namePrefix) {
        Node expression = new Node(namePrefix + "-expression");
        Node expOperand = new Node(namePrefix + "-expOperand");
        Node expOperator = new Node(namePrefix + "-expOperator")
                .addRule(sym.terminalNames[sym.SUM], StackActions.PUSH, new Action() {
                    @Override
                    public Object act(Symbol arg) {
                        Ast astSum = new Ast("sum", null);
                        Object op1 = astTree.tempMem.remove("$operand1");
                        if (op1 == null) {
                            Ast parent = astTree.parent;
                            astTree.parent.detachChild(astTree.key);
                            astSum.putChildAst("operand1", astTree);
                            parent.putChildAst("expression", astSum);
                        }
                        else {
                            Ast astOperand1 = new Ast("expression", op1);
                            astSum.putChildAst("operand1", astOperand1);
                            astTree.putChildAst("expression", astSum);
                        }
                        astTree = astSum;
                        return null;
                    }
                }, expOperand)
                .addRule(sym.terminalNames[sym.EQUAL], StackActions.PUSH, new Action() {
                    @Override
                    public Object act(Symbol arg) {
                        return null;
                    }
                }, expOperand);
        expression.addRule(sym.terminalNames[sym.NUMBER], StackActions.PUSH, new Action() {
            @Override
            public Object act(Symbol arg) {
                astTree.memorizeTemp("$operand1", arg);
                return null;
            }
        }, expOperator);
        expOperand.addRule(sym.terminalNames[sym.NUMBER], StackActions.PUSH, new Action() {
            @Override
            public Object act(Symbol arg) {
                Ast astOperand2 = new Ast("expression", arg);
                astTree.putChildAst("operand2", astOperand2);
                return null;
            }
        }, expOperator);
        expression.addRule(sym.terminalNames[sym.NUMBER], StackActions.PUSH, new Action() {
            @Override
            public Object act(Symbol arg) {
                astTree.memorizeTemp("$operand1", arg);
                return null;
            }
        }, expOperator);
        return expression;
    }
    public ElpisParser(String entryPoint, String fileName, ElpisLexer lexer) {
        HashSet<String> reservedWords = new HashSet<>();
        for (int counter = 0; counter < sym.terminalNames.length; counter++) {
            if (sym.terminalNames[counter].toLowerCase().equals("empty")) continue;
            if (sym.terminalNames[counter].toLowerCase().equals("end")) continue;
            if (sym.terminalNames[counter].toLowerCase().equals("start")) continue;
            sym.terminalNames[counter] = sym.terminalNames[counter].toLowerCase();
            reservedWords.add(sym.terminalNames[counter]);
        }
        //this.filePath = filePath;
        this.fileName = fileName;
        this.lexer = lexer;

        ArrayList<Symbol> tokenList = new ArrayList<>();
        Symbol token;
        try {
            while ((token = this.lexer.next_token()).value != null) {
                if (token.sym != sym.STRING && token.value instanceof String) {
                    token.value = ((String) token.value).trim();
                }
                if (token.value instanceof String) {
                    try {
                        token.value = Short.parseShort((String) token.value);
                        token.sym = sym.NUMBER;
                    } catch (Exception ex1) {
                        try {
                            token.value = Integer.parseInt((String) token.value);
                            token.sym = sym.NUMBER;
                        } catch (Exception ex2) {
                            try {
                                token.value = Long.parseLong((String) token.value);
                                token.sym = sym.NUMBER;
                            } catch (Exception ex3) {
                                try {
                                    token.value = Float.parseFloat((String) token.value);
                                    token.sym = sym.NUMBER;
                                } catch (Exception ex4) {
                                    try {
                                        token.value = Double.parseDouble((String) token.value);
                                        token.sym = sym.NUMBER;
                                    } catch (Exception ex5) {
                                        if (token.sym != sym.START && token.sym != sym.END) {
                                            if (!reservedWords.contains((String) token.value)) {
                                                token.sym = sym.IDENTIFIER;
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
                tokenList.add(token);
                System.out.println(sym.terminalNames[token.sym] + " " + token.value);
            }
            tokenList.add(new Symbol(sym.EOF, 0, 0, null));
        } catch (IOException ex) {
            ex.printStackTrace();
        }

        this.tokens = tokenList.toArray(new Symbol[0]);

        this.scopeStack = new Stack<>();
        this.astTree = new Ast("root", null);
        this.astTree.tree.put("codes", new ArrayList<>());
        scopeStack.push(new Pair<>((ArrayList<Ast>) this.astTree.tree.get("codes"), null));
        this.rootAst = this.astTree;

        this.tree = new Node("root");
        Node ifExp = constructExpression("if");
        Node whileLoopExp = constructExpression("while");
        Node forLoopExp = constructExpression("for");
        Node forStartExp = constructExpression("for-start");
        Node forEndExp = constructExpression("for-end");
        Node assignExp = constructExpression("assign");
        Node assignment = new Node("assignment");
        Node forCounter = new Node("for-counter");
        forCounter.addRule(sym.terminalNames[sym.IDENTIFIER], StackActions.PUSH, new Action() {
            @Override
            public Object act(Symbol arg) {
                astTree.tree.put("counter", arg);
                scopeStack.push(new Pair<>(new ArrayList<>(), astTree));
                return null;
            }
        }, forStartExp);
        forStartExp.nextTable.get(sym.terminalNames[sym.NUMBER]).c.addRule(sym.terminalNames[sym.ARROW], StackActions.POP, new Action() {
            @Override
            public Object act(Symbol arg) {
                Object op1 = astTree.tempMem.remove("$operand1");
                if (op1 != null) {
                    Ast astOperand1 = new Ast("expression", op1);
                    astTree.putChildAst("for-from", astOperand1);
                }
                scopeStack.pop();
                astTree = scopeStack.peek().second;
                scopeStack.push(new Pair<>(new ArrayList<>(), null));
                return null;
            }
        }, forEndExp);
        forEndExp.nextTable.get(sym.terminalNames[sym.NUMBER]).c.addRule(sym.terminalNames[sym.START], StackActions.POP, new Action() {
            @Override
            public Object act(Symbol arg) {
                Object op1 = astTree.tempMem.remove("$operand1");
                if (op1 != null) {
                    Ast astOperand1 = new Ast("expression", op1);
                    astTree.putChildAst("for-to", astOperand1);
                }
                scopeStack.pop();
                astTree = scopeStack.peek().second;
                astTree.tree.put("codes", new ArrayList<>());
                scopeStack.push(new Pair<>((ArrayList<Ast>) astTree.tree.get("codes"), null));
                return null;
            }
        }, tree);
        assignment.addRule(sym.terminalNames[sym.ASSIGN], StackActions.PUSH, new Action() {
                    @Override
                    public Object act(Symbol arg) {
                        Ast ast = new Ast("expression", null);
                        astTree.putChildAst("value", ast);
                        astTree = ast;
                        return null;
                    }
                }, assignExp);
        assignExp.nextTable.get(sym.terminalNames[sym.NUMBER]).c.addRule(sym.terminalNames[sym.END], StackActions.POP, new Action() {
            @Override
            public Object act(Symbol arg) {
                Object op1 = astTree.tempMem.remove("$operand1");
                if (op1 != null) {
                    Ast astOperand1 = new Ast("expression", op1);
                    astTree.putChildAst("expression", astOperand1);
                }
                scopeStack.peek().second.putChildAst("value", astTree);
                astTree = scopeStack.peek().second;
                scopeStack.pop();
                scopeStack.pop();
                return null;
            }
        }, tree);
        assignExp.nextTable.get(sym.terminalNames[sym.NUMBER]).c.addRule(sym.terminalNames[sym.IF], StackActions.POP, new Action() {
            @Override
            public Object act(Symbol arg) {
                Object op1 = astTree.tempMem.remove("$operand1");
                if (op1 != null) {
                    Ast astOperand1 = new Ast("expression", op1);
                    astTree.putChildAst("expression", astOperand1);
                }
                scopeStack.peek().second.putChildAst("value", astTree);
                astTree = scopeStack.peek().second;
                scopeStack.pop();

                Ast ast = new Ast("if", null);
                scopeStack.peek().first.add(ast);
                astTree = ast;
                scopeStack.peek().second = ast;
                return null;
            }
        }, ifExp);
        assignExp.nextTable.get(sym.terminalNames[sym.NUMBER]).c.addRule(sym.terminalNames[sym.COUNT], StackActions.POP, new Action() {
            @Override
            public Object act(Symbol arg) {
                Object op1 = astTree.tempMem.remove("$operand1");
                if (op1 != null) {
                    Ast astOperand1 = new Ast("expression", op1);
                    astTree.putChildAst("expression", astOperand1);
                }
                scopeStack.peek().second.putChildAst("value", astTree);
                astTree = scopeStack.peek().second;
                scopeStack.pop();

                Ast ast = new Ast("fpr", null);
                scopeStack.peek().first.add(ast);
                astTree = ast;
                scopeStack.peek().second = ast;
                return null;
            }
        }, forLoopExp);
        assignExp.nextTable.get(sym.terminalNames[sym.NUMBER]).c.addRule(sym.terminalNames[sym.WHILE], StackActions.POP, new Action() {
            @Override
            public Object act(Symbol arg) {
                Object op1 = astTree.tempMem.remove("$operand1");
                if (op1 != null) {
                    Ast astOperand1 = new Ast("expression", op1);
                    astTree.putChildAst("expression", astOperand1);
                }
                scopeStack.peek().second.putChildAst("value", astTree);
                astTree = scopeStack.peek().second;
                scopeStack.pop();

                Ast ast = new Ast("while", null);
                scopeStack.peek().first.add(ast);
                astTree = ast;
                scopeStack.peek().second = ast;
                return null;
            }
        }, whileLoopExp);
        assignExp.nextTable.get(sym.terminalNames[sym.NUMBER]).c.addRule(sym.terminalNames[sym.IDENTIFIER], StackActions.POP, new Action() {
            @Override
            public Object act(Symbol arg) {
                Object op1 = astTree.tempMem.remove("$operand1");
                if (op1 != null) {
                    Ast astOperand1 = new Ast("expression", op1);
                    astTree.putChildAst("expression", astOperand1);
                }
                scopeStack.peek().second.putChildAst("value", astTree);
                astTree = scopeStack.peek().second;
                scopeStack.pop();

                Ast ast = new Ast("assignment", null);
                ast.tree.put("var", arg);
                scopeStack.peek().first.add(ast);
                scopeStack.push(new Pair<>(new ArrayList<>(), null));
                astTree = ast;
                scopeStack.peek().second = ast;
                return null;
            }
        }, assignment);
        assignExp.nextTable.get(sym.terminalNames[sym.NUMBER]).c.addRule(sym.terminalNames[sym.EOF], StackActions.POP, new Action() {
            @Override
            public Object act(Symbol arg) {
                Object op1 = astTree.tempMem.remove("$operand1");
                if (op1 != null) {
                    Ast astOperand1 = new Ast("expression", op1);
                    astTree.putChildAst("expression", astOperand1);
                }
                scopeStack.peek().second.putChildAst("value", astTree);
                astTree = scopeStack.peek().second;
                scopeStack.pop();
                scopeStack.pop();
                return null;
            }
        }, new Node("eof"));
        ifExp.nextTable.get(sym.terminalNames[sym.NUMBER]).c.addRule(sym.terminalNames[sym.START], StackActions.POP, new Action() {
            @Override
            public Object act(Symbol arg) {
                Object op1 = astTree.tempMem.remove("$operand1");
                if (op1 != null) {
                    Ast astOperand1 = new Ast("expression", op1);
                    astTree.putChildAst("condition", astOperand1);
                }
                astTree = scopeStack.peek().second;
                scopeStack.peek().second = null;
                astTree.tree.put("codes", new ArrayList<>());
                scopeStack.push(new Pair<>((ArrayList<Ast>) astTree.tree.get("codes"), null));
                return null;
            }
        }, tree);
        whileLoopExp.nextTable.get(sym.terminalNames[sym.NUMBER]).c.addRule(sym.terminalNames[sym.START], StackActions.POP, new Action() {
            @Override
            public Object act(Symbol arg) {
                Object op1 = astTree.tempMem.remove("$operand1");
                if (op1 != null) {
                    Ast astOperand1 = new Ast("expression", op1);
                    astTree.putChildAst("condition", astOperand1);
                }
                astTree = scopeStack.peek().second;
                scopeStack.peek().second = null;
                astTree.tree.put("codes", new ArrayList<>());
                scopeStack.push(new Pair<>((ArrayList<Ast>) astTree.tree.get("codes"), null));
                return null;
            }
        }, tree);
        forLoopExp.nextTable.get(sym.terminalNames[sym.NUMBER]).c.addRule(sym.terminalNames[sym.START], StackActions.POP, new Action() {
            @Override
            public Object act(Symbol arg) {
                Object op1 = astTree.tempMem.remove("$operand1");
                if (op1 != null) {
                    Ast astOperand1 = new Ast("expression", op1);
                    astTree.putChildAst("expression", astOperand1);
                }
                astTree = scopeStack.peek().second;
                scopeStack.peek().second = null;
                astTree.tree.put("codes", new ArrayList<>());
                scopeStack.push(new Pair<>((ArrayList<Ast>) astTree.tree.get("codes"), null));
                return null;
            }
        }, tree);
        Node afterId = new Node("afterId");
        Node paramsList = new Node("paramsList");
        paramsList.addRule(sym.terminalNames[sym.IDENTIFIER], StackActions.PUSH, new Action() {
            @Override
            public Object act(Symbol arg) {
                Ast ast = new Ast("identifier", arg);
                ((ArrayList<Ast>)scopeStack.peek().second.tree.get("paramsList")).add(ast);
                return null;
            }
        }, paramsList);
        paramsList.addRule(sym.terminalNames[sym.START], StackActions.PUSH, new Action() {
            @Override
            public Object act(Symbol arg) {
                return null;
            }
        }, tree);
        afterId.addRule(sym.terminalNames[sym.ASSIGN], StackActions.PUSH, new Action() {
            @Override
            public Object act(Symbol arg) {
                Ast ast = new Ast("assignment", null);
                ast.tree.put("var", scopeStack.peek().second.tempMem.get("identifier"));
                scopeStack.peek().first.add(ast);
                scopeStack.push(new Pair<>(new ArrayList<>(), null));
                astTree = ast;
                scopeStack.peek().second = ast;
                return null;
            }
        }, assignment);
        afterId.addRule(sym.terminalNames[sym.ON], StackActions.PUSH, new Action() {
                    @Override
                    public Object act(Symbol arg) {
                        Ast ast = new Ast("function", null);
                        ast.tree.put("functionName", scopeStack.peek().second.tempMem.get("identifier"));
                        ast.tree.put("paramsList", new ArrayList<>());
                        scopeStack.peek().first.add(ast);
                        scopeStack.push(new Pair<>(new ArrayList<>(), null));
                        astTree = ast;
                        scopeStack.peek().second = ast;
                        return null;
                    }
                }, paramsList);
        Node declaration = new Node("declaration");
        declaration
                .addRule(sym.terminalNames[sym.IDENTIFIER], StackActions.PUSH, new Action() {
                    @Override
                    public Object act(Symbol arg) {
                        scopeStack.peek().second.memorizeTemp("identifier", arg);
                        return null;
                    }
                }, afterId);
        tree
                .addRule(sym.terminalNames[sym.DEFINE], StackActions.PUSH, new Action() {
                    @Override
                    public Object act(Symbol arg) {
                        return null;
                    }
                }, declaration)
                .addRule(sym.terminalNames[sym.IF], StackActions.PUSH, new Action() {
                            @Override
                            public Object act(Symbol arg) {
                                Ast ast = new Ast("if", null);
                                scopeStack.peek().first.add(ast);
                                astTree = ast;
                                scopeStack.peek().second = ast;
                                return null;
                            }
                        }, ifExp)
                .addRule(sym.terminalNames[sym.WHILE], StackActions.PUSH, new Action() {
                            @Override
                            public Object act(Symbol arg) {
                                Ast ast = new Ast("while", null);
                                scopeStack.peek().first.add(ast);
                                astTree = ast;
                                scopeStack.peek().second = ast;
                                return null;
                            }
                        }, whileLoopExp)
                .addRule(sym.terminalNames[sym.COUNT], StackActions.PUSH, new Action() {
                            @Override
                            public Object act(Symbol arg) {
                                Ast ast = new Ast("for", null);
                                scopeStack.peek().first.add(ast);
                                astTree = ast;
                                scopeStack.peek().second = ast;
                                return null;
                            }
                        }, forCounter)
                .addRule(sym.terminalNames[sym.EOF], StackActions.POP, new Action() {
                            @Override
                            public Object act(Symbol arg) {
                                scopeStack.pop();
                                return null;
                            }
                        }, new Node("eof"))
                .addRule(sym.terminalNames[sym.END], StackActions.POP, new Action() {
                            @Override
                            public Object act(Symbol arg) {
                                scopeStack.pop();
                                return null;
                            }
                        }, tree);
    }
    boolean matched = false;
    void iterate(Node node, int index, Tuple<StackActions, Action, Node, Node> prev) {
        System.out.println("using " + node.name);
        if (node.name.equals(sym.terminalNames[sym.EOF]) && index >= tokens.length - 1) {
            matched = true;
            System.out.println("matched.");
            return;
        }
        for(Map.Entry<String, Tuple<StackActions, Action, Node, Node>> next : node.nextTable.entrySet()) {
            if (next.getKey().equals("")) {
                if (index + 1 < tokens.length) {
                    iterate(next.getValue().c, index + 1, next.getValue());
                } else {
                    iterate(next.getValue().c, index, next.getValue());
                }
            } else if ((tokens[index].sym == sym.WORD && next.getKey().equals(tokens[index].value)) ||
                    (tokens[index].sym == sym.NUMBER && next.getKey().equals(sym.terminalNames[sym.NUMBER])) ||
                    (tokens[index].sym == sym.IDENTIFIER && next.getKey().equals(sym.terminalNames[sym.IDENTIFIER])) ||
                    (tokens[index].sym == sym.START && next.getKey().equals(sym.terminalNames[sym.START])) ||
                    (tokens[index].sym == sym.END && next.getKey().equals(sym.terminalNames[sym.END])) ||
                    (tokens[index].sym == sym.EOF && next.getKey().equals(sym.terminalNames[sym.EOF]))) {
                System.out.println("matched value : " + tokens[index].value + " , id : " + sym.terminalNames[tokens[index].sym]);
                next.getValue().b.act(tokens[index]);
                if (index + 1 < tokens.length) {
                    iterate(next.getValue().c, index + 1, next.getValue());
                } else {
                    iterate(next.getValue().c, index, next.getValue());
                }
                if (matched) return;
            } else {
                System.out.println("did not match value : " + tokens[index].value + " , id : " + sym.terminalNames[tokens[index].sym]);
            }
        }
    }
    public void parse() {

        //path = new Path();
        Node current = this.tree;
        iterate(current, 0, null);
        if (matched) {
            print(rootAst);
        } else {
            System.out.println("failure.");
        }
    }
}
