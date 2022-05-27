package tra.v5;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import tra.helpers.JsonHelper;
import tra.models.*;
import tra.models.temp.Path;
import tra.models.temp.Point;
import tra.v5.ElpisLexer;

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
    private String[] tokens;
    private Node tree;
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
        @JsonIgnore
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
    private Ast rootAst;
    private Ast astTree;
    private Ast tempPoint;
    public ElpisParser() {

        this.tokens = new String[] {sym.terminalNames[sym.IDENTIFIER], sym.terminalNames[sym.ASSIGN], sym.terminalNames[sym.NUMBER], sym.terminalNames[sym.NEWLINE],
                sym.terminalNames[sym.IDENTIFIER], sym.terminalNames[sym.ASSIGN], sym.terminalNames[sym.NUMBER], sym.terminalNames[sym.NEWLINE],
                sym.terminalNames[sym.IF], sym.terminalNames[sym.NUMBER], sym.terminalNames[sym.THEN],
                "exit"};

        this.astTree = new Ast("root", null);
        this.astTree.tree.put("codes", new ArrayList<>());
        this.rootAst = this.astTree;

        tree = new Node("root");
        Node expression = new Node("expression");
        Node expOperand = new Node("expOperand");
        Node expOperator = new Node("expOperator")
                .addRule(sym.terminalNames[sym.SUM], StackActions.PUSH, new Action() {
                    @Override
                    public Object act(Object arg) {
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
                    public Object act(Object arg) {
                        return null;
                    }
                }, expOperand);
        expression.addRule(sym.terminalNames[sym.NUMBER], StackActions.PUSH, new Action() {
                    @Override
                    public Object act(Object arg) {
                        astTree.memorizeTemp("$operand1", arg);
                        return null;
                    }
                }, expOperator);
        expOperand.addRule(sym.terminalNames[sym.NUMBER], StackActions.PUSH, new Action() {
            @Override
            public Object act(Object arg) {
                Ast astOperand2 = new Ast("expression", arg);
                astTree.putChildAst("operand2", astOperand2);
                return null;
            }
        }, expOperator);
        Node ifExp = cloneNode(expression);
        Node assignExp = cloneNode(expression);
        Node assignment = new Node("assignment")
                .addRule(sym.terminalNames[sym.ASSIGN], StackActions.PUSH, new Action() {
                    @Override
                    public Object act(Object arg) {
                        Ast ast = new Ast("expression", null);
                        astTree.putChildAst("value", ast);
                        astTree = ast;
                        return null;
                    }
                }, assignExp);
        ifExp.nextTable.get(sym.terminalNames[sym.NUMBER]).c.addRule(sym.terminalNames[sym.THEN], StackActions.POP, new Action() {
            @Override
            public Object act(Object arg) {
                tempPoint.putChildAst("expression", tempPoint);
                astTree = tempPoint;
                return null;
            }
        }, tree);
        assignExp.nextTable.get(sym.terminalNames[sym.NUMBER]).c.addRule(sym.terminalNames[sym.NEWLINE], StackActions.POP, new Action() {
            @Override
            public Object act(Object arg) {
                tempPoint.putChildAst("expression", tempPoint);
                astTree = tempPoint;
                return null;
            }
        }, tree);
        tree
                .addRule(sym.terminalNames[sym.IF], StackActions.PUSH, new Action() {
                    @Override
                    public Object act(Object arg) {
                        Ast ast = new Ast("if", null);
                        //((ArrayList<Ast>)astTree.tree.get("codes")).add(ast);
                        astTree = ast;
                        tempPoint = ast;
                        return null;
                    }
                }, ifExp)
                .addRule(sym.terminalNames[sym.IDENTIFIER], StackActions.PUSH, new Action() {
                    @Override
                    public Object act(Object arg) {
                        Ast ast = new Ast("assignment", null);
                        ast.tree.put("var", arg);
                        //((ArrayList<Ast>)astTree.tree.get("codes")).add(ast);
                        astTree = ast;
                        tempPoint = ast;
                        return null;
                    }
                }, assignment)
                .addRule("exit", StackActions.POP, new Action() {
                    @Override
                    public Object act(Object arg) {
                        return null;
                    }
                }, new Node("end"));
    }
    boolean matched = false;
    void iterate(Node node, int index, Tuple<StackActions, Action, Node, Node> prev) {
        if (node.name.equals("end") && index >= tokens.length - 1) {
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
            } else if (next.getKey().equals(tokens[index])) {
                System.out.println("matched " + tokens[index]);
                next.getValue().b.act(tokens[index]);
                if (index + 1 < tokens.length) {
                    iterate(next.getValue().c, index + 1, next.getValue());
                } else {
                    iterate(next.getValue().c, index, next.getValue());
                }
                if (matched) return;
            } else {
                System.out.println("did not match " + tokens[index]);
            }
        }
    }
    public void parse() {
        /*for (int counter = 0; counter < sym.terminalNames.length; counter++) {
            if (sym.terminalNames[counter].toLowerCase().equals("empty")) continue;
            if (sym.terminalNames[counter].toLowerCase().equals("end")) continue;
            if (sym.terminalNames[counter].toLowerCase().equals("start")) continue;
            sym.terminalNames[counter] = sym.terminalNames[counter].toLowerCase();
        }

        this.filePath = filePath;
        this.fileName = fileName;
        this.lexer = lexer;*/

        //path = new Path();
        Node current = this.tree;
        iterate(current, 0, null);
        //print(rootAst);
    }
}
