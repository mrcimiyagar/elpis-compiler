package tra.v4;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import java_cup.runtime.Symbol;
import tra.helpers.JsonHelper;
import tra.models.*;
import tra.models.temp.Path;
import tra.models.temp.Point;
import tra.models.temp.Rule;
import tra.models.temp.Switch;
import tra.v4.ElpisLexer;

import java.io.*;
import java.lang.invoke.SwitchPoint;
import java.nio.ByteBuffer;
import java.util.*;

public class ElpisParser {

    public static List<FileDeps> allDependencies = new ArrayList<>();
    public static List<Codes.Code> clientSideFunctions = new ArrayList<>();
    public static HashSet<String> processedFilesSet = new HashSet<>();

    private ElpisLexer lexer;
    private Point root;
    private Path path;
    private List<String> dependencies;
    public List<Rule> extraRules = new ArrayList<>();

    public static final String ANSI_RESET = "\u001B[0m";
    public static final String ANSI_BLACK = "\u001B[30m";
    public static final String ANSI_RED = "\u001B[31m";
    public static final String ANSI_GREEN = "\u001B[32m";
    public static final String ANSI_YELLOW = "\u001B[33m";
    public static final String ANSI_BLUE = "\u001B[34m";
    public static final String ANSI_PURPLE = "\u001B[35m";
    public static final String ANSI_CYAN = "\u001B[36m";
    public static final String ANSI_WHITE = "\u001B[37m";

    private String fileName;
    private String filePath;

    private void handleDependencyRules(String pathStr) {
        try {
            File file = new File(pathStr);
            FileReader fr = new FileReader(file);
            ElpisLexer lexer = new ElpisLexer(fr);
            ElpisParser traParser = new ElpisParser(pathStr, file.getName(), lexer);
            List<String> deps = traParser.parse();
            if (traParser.extraRules.size() > 0) {
                root.connections.addAll(0, traParser.extraRules);
                for (Pair<Switch, ArrayList<Object>> sp : path.pathStack) {
                    if (sp.first.point.name.equals(root.name))
                        sp.first.currentRuleIndex += traParser.extraRules.size();
                }
            }
            FileDeps fileDeps = new FileDeps();
            fileDeps.filePath = pathStr;
            fileDeps.depsPath = deps;
            allDependencies.add(fileDeps);
            processedFilesSet.add(pathStr);
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    public ElpisParser(String filePath, String fileName, ElpisLexer lexer) {

        for (int counter = 0; counter < sym.terminalNames.length; counter++) {
            if (sym.terminalNames[counter].toLowerCase().equals("empty")) continue;
            if (sym.terminalNames[counter].toLowerCase().equals("end")) continue;
            if (sym.terminalNames[counter].toLowerCase().equals("start")) continue;
            sym.terminalNames[counter] = sym.terminalNames[counter].toLowerCase();
        }

        this.filePath = filePath;
        this.fileName = fileName;
        this.lexer = lexer;

        path = new Path();

        root = new Point("root");
        Point exp = new Point("exp");
        Point id = new Point("id");
        Point idExtra = new Point("idExtra");
        Point idExtraDot = new Point("idExtraDot");
        Point extra = new Point("extra");
        Point epsilon = new Point("epsilon");
        Point argsPoint = new Point("argsPoint");
        Point extraArgsPoint = new Point("extraArgsPoint");
        Point dependencyPoint = new Point("dependency");
        root.connections.add(new Rule(Arrays.asList(dependencyPoint, root),
                new Action() {
                    @Override
                    public Object act(ArrayList<Object> args) {
                        return args.get(1);
                    }
                }));
        dependencyPoint.connections.add(new Rule(Arrays.asList(sym.terminalNames[sym.USE], sym.terminalNames[sym.STRING]),
                new Action() {
                    @Override
                    public Object act(ArrayList<Object> args) {
                        handleDependencyRules((String) ((Codes.Value)args.get(0)).getValue());
                        dependencies.add(0, (String) ((Codes.Value)args.get(0)).getValue());
                        return null;
                    }
                }));
        exp.connections.add(new Rule(Collections.singletonList(sym.terminalNames[sym.NUMBER]),
                new Action() {
                    @Override
                    public Object act(ArrayList<Object> args) {
                        return args.get(0);
                    }
                }));
        exp.connections.add(new Rule(Collections.singletonList(sym.terminalNames[sym.STRING]),
                new Action() {
                    @Override
                    public Object act(ArrayList<Object> args) {
                        return args.get(0);
                    }
                }));
        exp.connections.add(new Rule(Collections.singletonList(sym.terminalNames[sym.TRUE]),
                new Action() {
                    @Override
                    public Object act(ArrayList<Object> args) {
                        Codes.Value boolValue = new Codes.Value();
                        boolValue.setValue(true);
                        return boolValue;
                    }
                }));
        exp.connections.add(new Rule(Collections.singletonList(sym.terminalNames[sym.FALSE]),
                new Action() {
                    @Override
                    public Object act(ArrayList<Object> args) {
                        Codes.Value boolValue = new Codes.Value();
                        boolValue.setValue(false);
                        return boolValue;
                    }
                }));
        exp.connections.add(new Rule(Collections.singletonList(sym.terminalNames[sym.EMPTY]),
                args -> new Codes.Empty()));
        exp.connections.add(new Rule(
                Arrays.asList(sym.terminalNames[sym.INSTANCE], sym.terminalNames[sym.OF], id,
                        sym.terminalNames[sym.LPAREN], argsPoint, sym.terminalNames[sym.RPAREN]),
                new Action() {
                    @Override
                    public Object act(ArrayList<Object> args) {
                        Codes.Instantiate instantiate = new Codes.Instantiate();
                        instantiate.setClassReference((Codes.Code) args.get(0));
                        instantiate.setEntries((Hashtable<String, Codes.Code>) args.get(1));
                        return instantiate;
                    }
                }));
        exp.connections.add(new Rule(Arrays.asList(sym.terminalNames[sym.NOT], exp),
                new Action() {
                    @Override
                    public Object act(ArrayList<Object> args) {
                        Codes.MathExpNot not = new Codes.MathExpNot();
                        not.setValue((Codes.Code) args.get(0));
                        return not;
                    }
                }));
        exp.connections.add(new Rule(Arrays.asList(sym.terminalNames[sym.LPAREN], exp, sym.terminalNames[sym.RPAREN]),
                new Action() {
                    @Override
                    public Object act(ArrayList<Object> args) {
                        return args.get(0);
                    }
                }));
        exp.connections.add(new Rule(Arrays.asList(sym.terminalNames[sym.LBRACE], argsPoint, sym.terminalNames[sym.RBRACE]),
                new Action() {
                    @Override
                    public Object act(ArrayList<Object> args) {
                        Codes.AnonymousObject anonymousObject = new Codes.AnonymousObject();
                        anonymousObject.setContent((Hashtable<String, Codes.Code>) args.get(0));
                        return anonymousObject;
                    }
                }));
        Point expChainPoint = new Point("expChainPoint");
        Point expChainExtraPoint = new Point("expChainExtraPoint");
        expChainPoint.connections.add(new Rule(Arrays.asList(exp, expChainExtraPoint),
                new Action() {
                    @Override
                    public Object act(ArrayList<Object> args) {
                        List<Codes.Code> list = (List<Codes.Code>) args.get(1);
                        list.add(0, (Codes.Code)args.get(0));
                        return list;
                    }
                }));
        expChainPoint.connections.add(new Rule(Collections.singletonList(epsilon),
                new Action() {
                    @Override
                    public Object act(ArrayList<Object> args) {
                        return new ArrayList<>();
                    }
                }));
        expChainExtraPoint.connections.add(new Rule(Arrays.asList(sym.terminalNames[sym.COMMA], exp, expChainExtraPoint),
                new Action() {
                    @Override
                    public Object act(ArrayList<Object> args) {
                        List<Codes.Code> list = (List<Codes.Code>) args.get(1);
                        list.add(0, (Codes.Code)args.get(0));
                        return list;
                    }
                }));
        expChainExtraPoint.connections.add(new Rule(Collections.singletonList(epsilon),
                new Action() {
                    @Override
                    public Object act(ArrayList<Object> args) {
                        return new ArrayList<>();
                    }
                }));
        exp.connections.add(new Rule(Arrays.asList(sym.terminalNames[sym.LBRACKET], expChainPoint, sym.terminalNames[sym.RBRACKET]),
                new Action() {
                    @Override
                    public Object act(ArrayList<Object> args) {
                        Codes.Array array = new Codes.Array();
                        array.setItems((List<Codes.Code>)args.get(0));
                        return array;
                    }
                }));
        exp.connections.add(new Rule(Collections.singletonList(id),
                args -> {
                    print(ANSI_CYAN + "Collecting exp : id" + ANSI_RESET);
                    return args.get(0);
                }));
        exp.connections.add(new Rule(Arrays.asList(exp, extra),
                args -> {
                    if (args.size() > 1) {
                        if (args.get(1) instanceof Codes.Call) {
                            Codes.Call call = (Codes.Call) args.get(1);
                            call.setFuncReference((Codes.Code) args.get(0));
                            return call;
                        }
                        else if (args.get(1) instanceof Codes.Index) {
                            Codes.Index index = (Codes.Index) args.get(1);
                            index.setVar((Codes.Code)args.get(0));
                            return index;
                        }
                        else {
                            return args.get(0);
                        }
                    }
                    else {
                        return args.get(0);
                    }
                }));
        exp.connections.add(new Rule(Arrays.asList(exp, idExtra),
                args -> {
                    Codes.Reference ref = new Codes.Reference();
                    ref.setCurrentChain((Codes.Code)args.get(0));
                    ref.setRestOfChains((Codes.Code)args.get(1));
                    return ref;
                }));
        Point period = new Point("periodPoint");
        Point periodExtra = new Point("periodExtra");
        Point periodChain = new Point("periodChain");
        periodChain.connections.add(new Rule(Arrays.asList(period, periodChain),
                new Action() {
                    @Override
                    public Object act(ArrayList<Object> args) {
                        List<Codes.Code> list = null;
                        if (args.size() > 1) list = (List<Codes.Code>) args.get(1);
                        else list = new ArrayList<>();
                        list.add((Codes.Code) args.get(0));
                        return list;
                    }
                }));
        periodChain.connections.add(new Rule(Collections.singletonList(epsilon),
                new Action() {
                    @Override
                    public Object act(ArrayList<Object> args) {
                        return new ArrayList<>();
                    }
                }));
        period.connections.add(new Rule(Arrays.asList(exp, periodExtra),
                new Action() {
                    @Override
                    public Object act(ArrayList<Object> args) {
                        if (args.size() > 1 && args.get(1) != null) {
                            Codes.Period p = new Codes.Period();
                            p.setStart((Codes.Code) args.get(0));
                            p.setEnd((Codes.Code) args.get(1));
                            return p;
                        }
                        else {
                            return args.get(0);
                        }
                    }
                }));
        periodExtra.connections.add(new Rule(Arrays.asList(sym.terminalNames[sym.COLON], exp),
                new Action() {
                    @Override
                    public Object act(ArrayList<Object> args) {
                        return args.get(0);
                    }
                }));
        periodExtra.connections.add(new Rule(Collections.singletonList(epsilon),
                new Action() {
                    @Override
                    public Object act(ArrayList<Object> args) {
                        return null;
                    }
                }));
        extra.connections.add(new Rule(Arrays.asList(sym.terminalNames[sym.LBRACKET], periodChain, sym.terminalNames[sym.RBRACKET]),
                new Action() {
                    @Override
                    public Object act(ArrayList<Object> args) {
                        Codes.Index index = new Codes.Index();
                        index.setIndex((List<Codes.Code>)args.get(0));
                        index.setVar(null);
                        index.setRestOfChains(null);
                        return index;
                    }
                }));
        argsPoint.connections.add(new Rule(Arrays.asList(sym.terminalNames[sym.IDENTIFIER], sym.terminalNames[sym.COLON], exp, extraArgsPoint),
                new Action() {
                    @Override
                    public Object act(ArrayList<Object> args) {
                        Hashtable<String, Codes.Code> argsTable = null;
                        if (args.size() > 2) argsTable = (Hashtable<String, Codes.Code>) args.get(2);
                        else argsTable = new Hashtable<>();
                        Codes.Identifier key = (Codes.Identifier) args.get(0);
                        Codes.Code expression = (Codes.Code) args.get(1);
                        argsTable.put(key.getName(), expression);
                        return argsTable;
                    }
                }));
        argsPoint.connections.add(new Rule(Collections.singletonList(epsilon),
                new Action() {
                    @Override
                    public Object act(ArrayList<Object> args) {
                        return new Hashtable<>();
                    }
                }));
        extraArgsPoint.connections.add(new Rule(Arrays.asList(sym.terminalNames[sym.COMMA], sym.terminalNames[sym.IDENTIFIER], sym.terminalNames[sym.COLON], exp, extraArgsPoint),
                new Action() {
                    @Override
                    public Object act(ArrayList<Object> args) {
                        Codes.Identifier key = (Codes.Identifier) args.get(0);
                        Codes.Code expression = (Codes.Code) args.get(1);
                        Hashtable<String, Codes.Code> argsTable = (Hashtable<String, Codes.Code>) args.get(2);
                        argsTable.put(key.getName(), expression);
                        return argsTable;
                    }
                }));
        extraArgsPoint.connections.add(new Rule(Collections.singletonList(epsilon),
                new Action() {
                    @Override
                    public Object act(ArrayList<Object> args) {
                        return new Hashtable<>();
                    }
                }));
        extra.connections.add(new Rule(Arrays.asList(sym.terminalNames[sym.LPAREN], argsPoint, sym.terminalNames[sym.RPAREN]),
                args -> {
                    Codes.Call call = new Codes.Call();
                    call.setFuncReference(null);
                    call.setEntries((Hashtable<String, Codes.Code>) args.get(0));
                    return call;
                }));
        extra.connections.add(new Rule(Collections.singletonList(epsilon),
                args -> null));

        root.connections.add(new Rule(
                Arrays.asList(sym.terminalNames[sym.FOR], exp, sym.terminalNames[sym.TIMES], sym.terminalNames[sym.BY],
                        sym.terminalNames[sym.STEP], exp, sym.terminalNames[sym.DO], sym.terminalNames[sym.START],
                        root, sym.terminalNames[sym.END], root),
                new Action() {
                    @Override
                    public Object act(ArrayList<Object> args) {
                        Codes.CounterFor counterForStmt = new Codes.CounterFor();
                        counterForStmt.setLimit((Codes.Code) args.get(0));
                        counterForStmt.setStep((Codes.Code) args.get(1));
                        counterForStmt.setCodes((List<Codes.Code>) args.get(2));
                        if (args.size() > 3 && args.get(3) != null) {
                            List<Codes.Code> restOfCode = (List<Codes.Code>) args.get(3);
                            restOfCode.add(0, counterForStmt);
                            return restOfCode;
                        }
                        else {
                            List<Codes.Code> restOfCode = new ArrayList<>();
                            restOfCode.add(counterForStmt);
                            return restOfCode;
                        }
                    }
                }));
        root.connections.add(new Rule(
                Arrays.asList(sym.terminalNames[sym.UNTIL], exp, sym.terminalNames[sym.DO], sym.terminalNames[sym.START],
                        root, sym.terminalNames[sym.END], root),
                new Action() {
                    @Override
                    public Object act(ArrayList<Object> args) {
                        Codes.While whileLoopStmt = new Codes.While();
                        whileLoopStmt.setCondition((Codes.Code) args.get(0));
                        whileLoopStmt.setCodes((List<Codes.Code>) args.get(1));
                        if (args.size() > 2 && args.get(2) != null) {
                            List<Codes.Code> restOfCode = (List<Codes.Code>) args.get(2);
                            restOfCode.add(0, whileLoopStmt);
                            return restOfCode;
                        }
                        else {
                            List<Codes.Code> restOfCode = new ArrayList<>();
                            restOfCode.add(whileLoopStmt);
                            return restOfCode;
                        }
                    }
                }));
        Point elseIfPoint = new Point("elseIfPoint");
        elseIfPoint.connections.add(new Rule(Arrays.asList(sym.terminalNames[sym.ELSE], sym.terminalNames[sym.IF], exp,
                sym.terminalNames[sym.THEN], sym.terminalNames[sym.DO], sym.terminalNames[sym.START], root,
                sym.terminalNames[sym.END], elseIfPoint),
                        new Action() {
                            @Override
                            public Object act(ArrayList<Object> args) {
                                Codes.ElseIf elseIfStmt = new Codes.ElseIf();
                                elseIfStmt.setCondition((Codes.Code)args.get(0));
                                elseIfStmt.setCodes((List<Codes.Code>) args.get(1));
                                List<Codes.Code> restOfElses = (List<Codes.Code>) args.get(2);
                                restOfElses.add(0, elseIfStmt);
                                return restOfElses;
                            }
                        }));
        elseIfPoint.connections.add(new Rule(Collections.singletonList(epsilon),
                new Action() {
                    @Override
                    public Object act(ArrayList<Object> args) {
                        print(ANSI_CYAN + "Collecting else if : epsilon" + ANSI_RESET);
                        return new ArrayList<>();
                    }
                }));
        Point elsePoint = new Point("elsePoint");
        elsePoint.connections.add(new Rule(Arrays.asList(sym.terminalNames[sym.ELSE], sym.terminalNames[sym.DO], sym.terminalNames[sym.START], root, sym.terminalNames[sym.END]),
                new Action() {
                    @Override
                    public Object act(ArrayList<Object> args) {
                        Codes.Else elseStmt = new Codes.Else();
                        elseStmt.setCodes((List<Codes.Code>) args.get(0));
                        List<Codes.Code> elseCodes = new ArrayList<>();
                        elseCodes.add(elseStmt);
                        return elseCodes;
                    }
                }));
        elsePoint.connections.add(new Rule(Collections.singletonList(epsilon),
                new Action() {
                    @Override
                    public Object act(ArrayList<Object> args) {
                        print(ANSI_CYAN + "Collecting else : epsilon" + ANSI_RESET);
                        return new ArrayList<>();
                    }
                }));
        root.connections.add(new Rule(
                Arrays.asList(sym.terminalNames[sym.IF], exp, sym.terminalNames[sym.THEN], sym.terminalNames[sym.DO], sym.terminalNames[sym.START],
                        root, sym.terminalNames[sym.END], elseIfPoint, elsePoint, root),
                new Action() {
                    @Override
                    public Object act(ArrayList<Object> args) {
                        Codes.If ifStmt = new Codes.If();
                        ifStmt.setCondition((Codes.Code) args.get(0));
                        ifStmt.setCodes((List<Codes.Code>) args.get(1));
                        List<Codes.Code> elseIfCodes = (List<Codes.Code>) args.get(2);
                        List<Codes.Code> elseCode = (List<Codes.Code>) args.get(3);
                        List<Codes.Code> restOfCode = (List<Codes.Code>) args.get(4);
                        elseIfCodes.addAll(elseCode);
                        ifStmt.setExtras(elseIfCodes);
                        restOfCode.add(0, ifStmt);
                        return restOfCode;
                    }
                }));
        Point classPropsPoint = new Point("classPropsPoint");
        classPropsPoint.connections.add(new Rule(Arrays.asList(sym.terminalNames[sym.DEFINE], sym.terminalNames[sym.PROP],
                sym.terminalNames[sym.IDENTIFIER], classPropsPoint),
                new Action() {
                    @Override
                    public Object act(ArrayList<Object> args) {
                        Codes.Prop prop = new Codes.Prop();
                        prop.setId((Codes.Identifier) args.get(0));
                        List<Codes.Prop> restOfProps = (List<Codes.Prop>) args.get(1);
                        restOfProps.add(0, prop);
                        return restOfProps;
                    }
                }));
        classPropsPoint.connections.add(new Rule(Collections.singletonList(epsilon),
                new Action() {
                    @Override
                    public Object act(ArrayList<Object> args) {
                        return new ArrayList<>();
                    }
                }));
        Point paramsExtraPoint = new Point("paramsExtraPoint");
        Point paramsPoint = new Point("paramsPoint");
        paramsPoint.connections.add(new Rule(Arrays.asList(sym.terminalNames[sym.LPAREN],
                sym.terminalNames[sym.IDENTIFIER], paramsExtraPoint, sym.terminalNames[sym.PARAMS]),
                new Action() {
                    @Override
                    public Object act(ArrayList<Object> args) {
                        Codes.Identifier id = (Codes.Identifier) args.get(0);
                        List<Codes.Identifier> ids = (List<Codes.Identifier>) args.get(1);
                        ids.add(0, id);
                        return ids;
                    }
                }));
        paramsPoint.connections.add(new Rule(Collections.singletonList(epsilon),
                new Action() {
                    @Override
                    public Object act(ArrayList<Object> args) {
                        return new ArrayList<>();
                    }
                }));
        paramsExtraPoint.connections.add(new Rule(Arrays.asList(sym.terminalNames[sym.COMMA], sym.terminalNames[sym.IDENTIFIER],
                paramsExtraPoint),
                new Action() {
                    @Override
                    public Object act(ArrayList<Object> args) {
                        Codes.Identifier id = (Codes.Identifier) args.get(0);
                        List<Codes.Identifier> ids = (List<Codes.Identifier>) args.get(1);
                        ids.add(0, id);
                        return ids;
                    }
                }));
        paramsExtraPoint.connections.add(new Rule(Collections.singletonList(epsilon),
                new Action() {
                    @Override
                    public Object act(ArrayList<Object> args) {
                        return new ArrayList<>();
                    }
                }));
        Point classFuncsPoint = new Point("classFuncsPoint");
        Point annot = new Point("annotPoint");
        annot.connections.add(new Rule(Arrays.asList(sym.terminalNames[sym.ANNOT], sym.terminalNames[sym.IDENTIFIER]),
                new Action() {
                    @Override
                    public Object act(ArrayList<Object> args) {
                        return args.get(0);
                    }
                }));
        annot.connections.add(new Rule(Collections.singletonList(epsilon),
                new Action() {
                    @Override
                    public Object act(ArrayList<Object> args) {
                        return new Object();
                    }
                }));
        classFuncsPoint.connections.add(new Rule(Arrays.asList(
                sym.terminalNames[sym.DEFINE], sym.terminalNames[sym.FUNCTION], sym.terminalNames[sym.IDENTIFIER], paramsPoint,
                sym.terminalNames[sym.START], root, sym.terminalNames[sym.END],
                classFuncsPoint),
                new Action() {
                    @Override
                    public Object act(ArrayList<Object> args) {
                        Codes.Identifier funcName = (Codes.Identifier) args.get(0);
                        List<Codes.Identifier> params = (List<Codes.Identifier>) args.get(1);
                        List<Codes.Code> funcBody = (List<Codes.Code>) args.get(2);
                        List<Codes.Function> restOfFuncs = (List<Codes.Function>) args.get(3);
                        Codes.Function function = new Codes.Function();
                        function.setName(funcName.getName());
                        function.setParams(params);
                        function.setCodes(funcBody);
                        function.setLevel(Codes.DataLevel.InstanceLevel);
                        restOfFuncs.add(0, function);
                        return restOfFuncs;
                    }
                }));
        classFuncsPoint.connections.add(new Rule(Collections.singletonList(epsilon),
                new Action() {
                    @Override
                    public Object act(ArrayList<Object> args) {
                        return new ArrayList<>();
                    }
                }));
        root.connections.add(new Rule(
                Arrays.asList(sym.terminalNames[sym.DEFINE], sym.terminalNames[sym.CLASS], sym.terminalNames[sym.IDENTIFIER],
                        sym.terminalNames[sym.START], sym.terminalNames[sym.ON], sym.terminalNames[sym.CREATED], paramsPoint,
                        sym.terminalNames[sym.START], root, sym.terminalNames[sym.END], classPropsPoint, classFuncsPoint,
                        sym.terminalNames[sym.END], root),
                new Action() {
                    @Override
                    public Object act(ArrayList<Object> args) {
                        String className = ((Codes.Identifier)args.get(0)).getName();
                        List<Codes.Identifier> constructorParams = (List<Codes.Identifier>) args.get(1);
                        List<Codes.Code> constructorBody = (List<Codes.Code>) args.get(2);
                        List<Codes.Prop> classProps = (List<Codes.Prop>) args.get(3);
                        List<Codes.Function> classFuncs = (List<Codes.Function>) args.get(4);
                        List<Codes.Code> restOfCodes = (List<Codes.Code>) args.get(5);
                        Codes.Class classStmt = new Codes.Class();
                        Codes.Constructor constructor = new Codes.Constructor();
                        constructor.setParams(constructorParams);
                        constructor.setBody(constructorBody);
                        classStmt.setConstructor(constructor);
                        classStmt.setFunctions(classFuncs);
                        classStmt.setProperties(classProps);
                        classStmt.setInheritance(new ArrayList<>());
                        classStmt.setBehavior(new ArrayList<>());
                        classStmt.setName(className);
                        restOfCodes.add(0, classStmt);
                        return restOfCodes;
                    }
                }));
        root.connections.add(new Rule(Arrays.asList(annot, sym.terminalNames[sym.DEFINE], sym.terminalNames[sym.FUNCTION],
                sym.terminalNames[sym.IDENTIFIER], paramsPoint, sym.terminalNames[sym.START], root, sym.terminalNames[sym.END],
                root),
                new Action() {
                    @Override
                    public Object act(ArrayList<Object> args) {
                        Object annotation = args.get(0);
                        Codes.Identifier funcName = (Codes.Identifier) args.get(1);
                        List<Codes.Identifier> params = (List<Codes.Identifier>) args.get(2);
                        List<Codes.Code> funcBody = (List<Codes.Code>) args.get(3);
                        List<Codes.Code> restOfCodes = (List<Codes.Code>) args.get(4);
                        Codes.Function function = new Codes.Function();
                        function.setName(funcName.getName());
                        function.setParams(params);
                        function.setCodes(funcBody);
                        function.setLevel(Codes.DataLevel.InstanceLevel);
                        restOfCodes.add(0, function);
                        if (annotation instanceof Codes.Identifier) {
                            if (((Codes.Identifier) annotation).getName().equals("ClientSide")) {
                                clientSideFunctions.add(0, function);
                            }
                        }
                        return restOfCodes;
                    }
                }));
        root.connections.add(new Rule(
                Arrays.asList(sym.terminalNames[sym.CREATE], sym.terminalNames[sym.INSTANCE], sym.terminalNames[sym.OF], id,
                        sym.terminalNames[sym.LPAREN], argsPoint, sym.terminalNames[sym.RPAREN],
                        root),
                new Action() {
                    @Override
                    public Object act(ArrayList<Object> args) {
                        Codes.Instantiate instantiate = new Codes.Instantiate();
                        instantiate.setClassReference((Codes.Code) args.get(0));
                        instantiate.setEntries((Hashtable<String, Codes.Code>) args.get(1));
                        List<Codes.Code> restOfCodes = (List<Codes.Code>) args.get(2);
                        restOfCodes.add(0, instantiate);
                        return restOfCodes;
                    }
                }));
        root.connections.add(new Rule(
                Collections.singletonList(sym.terminalNames[sym.EMPTY]),
                new Action() {
                    @Override
                    public Object act(ArrayList<Object> args) {
                        return new ArrayList<>();
                    }
                }));
        root.connections.add(new Rule(
                Arrays.asList(sym.terminalNames[sym.RETURN], exp),
                new Action() {
                    @Override
                    public Object act(ArrayList<Object> args) {
                        Codes.Return returnStmt = new Codes.Return();
                        returnStmt.setValue((Codes.Code)args.get(0));
                        List<Codes.Code> codes = new ArrayList<>();
                        codes.add(returnStmt);
                        return codes;
                    }
                }));
        root.connections.add(new Rule(
                Arrays.asList(sym.terminalNames[sym.EXIT], sym.terminalNames[sym.LOOP]),
                new Action() {
                    @Override
                    public Object act(ArrayList<Object> args) {
                        Codes.Break breakStmt = new Codes.Break();
                        List<Codes.Code> codes = new ArrayList<>();
                        codes.add(breakStmt);
                        return codes;
                    }
                }));

        exp.connections.add(new Rule(Arrays.asList(exp, sym.terminalNames[sym.SUM], exp),
                args -> {
                    print(ANSI_CYAN + "Collecting exp : exp sum exp" + ANSI_RESET);
                    Codes.MathExpSum sum = new Codes.MathExpSum();
                    sum.setValue1((Codes.Code)args.get(0));
                    sum.setValue2((Codes.Code)args.get(1));
                    return sum;
                }));
        exp.connections.add(new Rule(Arrays.asList(exp, sym.terminalNames[sym.SUBTRACT], exp),
                args -> {
                    Codes.MathExpSubstract sum = new Codes.MathExpSubstract();
                    sum.setValue1((Codes.Code)args.get(0));
                    sum.setValue2((Codes.Code)args.get(1));
                    return sum;
                }));
        exp.connections.add(new Rule(Arrays.asList(exp, sym.terminalNames[sym.MULTIPLY], exp),
                args -> {
                    Codes.MathExpMultiply sum = new Codes.MathExpMultiply();
                    sum.setValue1((Codes.Code)args.get(0));
                    sum.setValue2((Codes.Code)args.get(1));
                    return sum;
                }));
        exp.connections.add(new Rule(Arrays.asList(exp, sym.terminalNames[sym.DIVISION], exp),
                args -> {
                    Codes.MathExpDivide sum = new Codes.MathExpDivide();
                    sum.setValue1((Codes.Code)args.get(0));
                    sum.setValue2((Codes.Code)args.get(1));
                    return sum;
                }));
        exp.connections.add(new Rule(Arrays.asList(exp, sym.terminalNames[sym.AND], exp),
                args -> {
                    Codes.MathExpAnd sum = new Codes.MathExpAnd();
                    sum.setValue1((Codes.Code)args.get(0));
                    sum.setValue2((Codes.Code)args.get(1));
                    return sum;
                }));
        exp.connections.add(new Rule(Arrays.asList(exp, sym.terminalNames[sym.OR], exp),
                args -> {
                    Codes.MathExpOr sum = new Codes.MathExpOr();
                    sum.setValue1((Codes.Code)args.get(0));
                    sum.setValue2((Codes.Code)args.get(1));
                    return sum;
                }));
        exp.connections.add(new Rule(Arrays.asList(exp, sym.terminalNames[sym.POWER], exp),
                args -> {
                    Codes.MathExpPower sum = new Codes.MathExpPower();
                    sum.setValue1((Codes.Code)args.get(0));
                    sum.setValue2((Codes.Code)args.get(1));
                    return sum;
                }));
        exp.connections.add(new Rule(Arrays.asList(exp, sym.terminalNames[sym.MOD], exp),
                args -> {
                    Codes.MathExpMod sum = new Codes.MathExpMod();
                    sum.setValue1((Codes.Code)args.get(0));
                    sum.setValue2((Codes.Code)args.get(1));
                    return sum;
                }));
        exp.connections.add(new Rule(Arrays.asList(exp, sym.terminalNames[sym.EQUAL], exp),
                args -> {
                    Codes.MathExpEqual sum = new Codes.MathExpEqual();
                    sum.setValue1((Codes.Code)args.get(0));
                    sum.setValue2((Codes.Code)args.get(1));
                    return sum;
                }));
        exp.connections.add(new Rule(Arrays.asList(exp, sym.terminalNames[sym.LT], exp),
                args -> {
                    Codes.MathExpLT sum = new Codes.MathExpLT();
                    sum.setValue1((Codes.Code)args.get(0));
                    sum.setValue2((Codes.Code)args.get(1));
                    return sum;
                }));
        exp.connections.add(new Rule(Arrays.asList(exp, sym.terminalNames[sym.LE], exp),
                args -> {
                    Codes.MathExpLE sum = new Codes.MathExpLE();
                    sum.setValue1((Codes.Code)args.get(0));
                    sum.setValue2((Codes.Code)args.get(1));
                    return sum;
                }));
        exp.connections.add(new Rule(Arrays.asList(exp, sym.terminalNames[sym.GE], exp),
                args -> {
                    Codes.MathExpGE sum = new Codes.MathExpGE();
                    sum.setValue1((Codes.Code)args.get(0));
                    sum.setValue2((Codes.Code)args.get(1));
                    return sum;
                }));
        exp.connections.add(new Rule(Arrays.asList(exp, sym.terminalNames[sym.GT], exp),
                args -> {
                    Codes.MathExpGT sum = new Codes.MathExpGT();
                    sum.setValue1((Codes.Code)args.get(0));
                    sum.setValue2((Codes.Code)args.get(1));
                    return sum;
                }));
        exp.connections.add(new Rule(Arrays.asList(exp, sym.terminalNames[sym.NE], exp),
                args -> {
                    Codes.MathExpNE sum = new Codes.MathExpNE();
                    sum.setValue1((Codes.Code)args.get(0));
                    sum.setValue2((Codes.Code)args.get(1));
                    return sum;
                }));
        id.connections.add(new Rule(Arrays.asList(sym.terminalNames[sym.IDENTIFIER], idExtra),
                args -> {
                    print(ANSI_CYAN + "Collecting id : id idExtra" + ANSI_RESET);
                    Codes.Reference ref = new Codes.Reference();
                    ref.setCurrentChain((Codes.Code) args.get(0));
                    if (args.size() > 1)
                        ref.setRestOfChains((Codes.Code) args.get(1));
                    else
                        ref.setRestOfChains(null);
                    return ref;
                }));
        idExtra.connections.add(new Rule(Arrays.asList(sym.terminalNames[sym.DOT], idExtraDot),
                args -> {
                    print(ANSI_CYAN + "Collecting idExtra : dot idExtraDot" + ANSI_RESET);
                    return args.get(0);
                }));
        idExtra.connections.add(new Rule(Collections.singletonList(epsilon),
                args -> {
                    print(ANSI_CYAN + "Collecting idExtra : epsilon" + ANSI_RESET);
                    return null;
                }));
        idExtraDot.connections.add(new Rule(Arrays.asList(sym.terminalNames[sym.IDENTIFIER], idExtra),
                args -> {
                    print(ANSI_CYAN + "Collecting idExtraDot : id idExtra" + ANSI_RESET);
                    Codes.Reference ref = new Codes.Reference();
                    ref.setCurrentChain((Codes.Code) args.get(0));
                    if (args.size() > 1)
                        ref.setRestOfChains((Codes.Code) args.get(1));
                    else
                        ref.setRestOfChains(null);
                    return ref;
                }));
        Point command = new Point("commandPoint");
        command.connections.add(new Rule(Arrays.asList(sym.terminalNames[sym.IDENTIFIER],
                sym.terminalNames[sym.COLON], sym.terminalNames[sym.IDENTIFIER], command),
                new Action() {
                    @Override
                    public Object act(ArrayList<Object> args) {
                        List<Object> commandList = (List<Object>)args.get(2);
                        commandList.add(0, ((Codes.Identifier)args.get(0)).getName() + ":" + ((Codes.Identifier)args.get(1)).getName());
                        return commandList;
                    }
                }));
        command.connections.add(new Rule(Arrays.asList(sym.terminalNames[sym.IDENTIFIER], command),
                new Action() {
                    @Override
                    public Object act(ArrayList<Object> args) {
                        List<Object> commandList = (List<Object>)args.get(1);
                        commandList.add(0, ((Codes.Identifier)args.get(0)).getName());
                        return commandList;
                    }
                }));
        command.connections.add(new Rule(Collections.singletonList(epsilon),
                new Action() {
                    @Override
                    public Object act(ArrayList<Object> args) {
                        return new ArrayList<Object>();
                    }
                }));
        Point commandContainer = new Point("commandContainer");
        root.connections.add(new Rule(Arrays.asList(commandContainer, root),
                new Action() {
                    @Override
                    public Object act(ArrayList<Object> args) {
                        return args.get(1);
                    }
                }));
        commandContainer.connections.add(new Rule(Arrays.asList(sym.terminalNames[sym.DEFINE], sym.terminalNames[sym.COMMAND],
                sym.terminalNames[sym.LBRACE], command, sym.terminalNames[sym.RBRACE], sym.terminalNames[sym.START],
                root, sym.terminalNames[sym.END]),
                        new Action() {
                            @Override
                            public Object act(ArrayList<Object> args) {
                                List<Object> commandList = (List<Object>) args.get(0);
                                final List<Codes.Code> meaning = (List<Codes.Code>) args.get(1);
                                commandList.add(root);
                                final List<String> varNames = new ArrayList<>();
                                int cuCounter = 0;
                                for (Object commandUnit : commandList) {
                                    if (commandUnit.toString().endsWith(":$exp")) {
                                        varNames.add(commandUnit.toString().substring(0, commandUnit.toString().length() - ":$exp".length()));
                                        commandList.set(cuCounter, exp);
                                    }
                                    cuCounter++;
                                }
                                for (Pair<Switch, ArrayList<Object>> sp : path.pathStack) {
                                    if (sp.first.point.name.equals(root.name))
                                        sp.first.currentRuleIndex++;
                                }
                                Rule rule = new Rule(commandList, new Action() {
                                    @Override
                                    public Object act(ArrayList<Object> args) {
                                        List<Codes.Code> roc = (List<Codes.Code>) args.get(args.size() - 1);
                                        roc.addAll(0, meaning);
                                        int counter = 0;
                                        Collections.reverse(varNames);
                                        for (String varName : varNames) {
                                            Codes.Assignment assignment = new Codes.Assignment();
                                            assignment.setValue((Codes.Code) args.get(counter));
                                            Codes.Identifier cuId = new Codes.Identifier();
                                            cuId.setName(varName);
                                            Codes.Reference cuRef = new Codes.Reference();
                                            cuRef.setCurrentChain(cuId);
                                            cuRef.setRestOfChains(null);
                                            assignment.setVar(cuRef);
                                            roc.add(0, assignment);
                                            counter++;
                                        }
                                        return roc;
                                    }
                                });
                                root.connections.add(0, rule);
                                extraRules.add(rule);
                                return null;
                            }
                        }));

        root.connections.add(new Rule(Arrays.asList(id, sym.terminalNames[sym.ASSIGN], exp, root),
                new Action() {
                    @Override
                    public Object act(ArrayList<Object> args) {
                        Codes.Assignment assignment = new Codes.Assignment();
                        assignment.setVar((Codes.Code)args.get(0));
                        assignment.setValue((Codes.Code)args.get(1));
                        List<Codes.Code> restOfCodes = (List<Codes.Code>) args.get(2);
                        restOfCodes.add(0, assignment);
                        return restOfCodes;
                    }
                }));
        root.connections.add(new Rule(Arrays.asList(exp, root),
                args -> {
                    print(ANSI_CYAN + "Collecting root : exp" + ANSI_RESET);
                    List<Codes.Code> restOfCode = (List<Codes.Code>) args.get(1);
                    restOfCode.add(0, (Codes.Code)args.get(0));
                    return restOfCode;
                }));
        root.connections.add(new Rule(Collections.singletonList(epsilon),
                new Action() {
                    @Override
                    public Object act(ArrayList<Object> args) {
                        return new ArrayList<>();
                    }
                }));
    }

    private static byte[] convertDoubleToBytes(double number) {
        ByteBuffer bb = ByteBuffer.allocate(8);
        bb.putDouble(number);
        return bb.array();
    }

    private static byte[] convertFloatToBytes(float number) {
        ByteBuffer bb = ByteBuffer.allocate(4);
        bb.putFloat(number);
        return bb.array();
    }

    private static byte[] convertShortToBytes(short number) {
        ByteBuffer bb = ByteBuffer.allocate(2);
        bb.putShort(number);
        return bb.array();
    }

    private static byte[] convertIntegerToBytes(int number) {
        ByteBuffer bb = ByteBuffer.allocate(4);
        bb.putInt(number);
        return bb.array();
    }

    private static byte[] convertLongToBytes(long number) {
        ByteBuffer bb = ByteBuffer.allocate(8);
        bb.putLong(number);
        return bb.array();
    }

    private static byte[] convertBooleanToBytes(boolean number) {
        return new byte[]{(byte) (number?1:0)};
    }

    public static byte[] convertCodeToBytes(List<Codes.Code> codes) {
        ByteArrayOutputStream stream = new ByteArrayOutputStream();
        try {
            for (Codes.Code code : codes) {
                if (code instanceof Codes.Function) {
                    stream.write(new byte[]{0x51});
                    Codes.Function func = (Codes.Function) code;
                    stream.write(new byte[]{0x01});
                    byte[] name = func.getName().getBytes();
                    stream.write(convertIntegerToBytes(name.length + 1));
                    stream.write(name);
                    stream.write('\0');
                    stream.write(new byte[]{0x02});
                    byte[] level = func.getLevel().toString().getBytes();
                    stream.write(convertIntegerToBytes(level.length + 1));
                    stream.write(level);
                    stream.write('\0');
                    stream.write(new byte[]{0x03});
                    stream.write(convertIntegerToBytes(func.getParams().size()));
                    for (Codes.Identifier id : func.getParams()) {
                        byte[] idName = id.getName().getBytes();
                        stream.write(convertIntegerToBytes(idName.length + 1));
                        stream.write(idName);
                        stream.write('\0');
                    }
                    stream.write(new byte[]{0x04});
                    stream.write(new byte[]{0x6f});
                    byte[] cs = convertCodeToBytes(func.getCodes());
                    stream.write(convertIntegerToBytes(cs.length));
                    stream.write(cs);
                    stream.write(new byte[]{0x6e});
                } else if (code instanceof Codes.Break) {
                    stream.write(0x4c);
                } else if (code instanceof Codes.If) {
                    stream.write(new byte[]{0x52});
                    Codes.If ifCode = (Codes.If) code;
                    stream.write(new byte[]{0x01});
                    stream.write(convertExpressionToBytes(ifCode.getCondition()));
                    stream.write(new byte[]{0x02});
                    stream.write(new byte[]{0x6f});
                    byte[] cs = convertCodeToBytes(ifCode.getCodes());
                    stream.write(convertIntegerToBytes(cs.length));
                    stream.write(cs);
                    stream.write(new byte[]{0x6e});
                    if (ifCode.getExtras() != null) {
                        stream.write(new byte[] {0x03});
                        stream.write(convertIntegerToBytes(ifCode.getExtras().size()));
                        for (Codes.Code elseCode : ifCode.getExtras()) {
                            if (elseCode instanceof Codes.ElseIf) {
                                stream.write(new byte[]{0x53});
                                Codes.ElseIf elseIfCode = (Codes.ElseIf) elseCode;
                                stream.write(new byte[]{0x01});
                                stream.write(convertExpressionToBytes(elseIfCode.getCondition()));
                                stream.write(new byte[]{0x02});
                                stream.write(new byte[]{0x6f});
                                cs = convertCodeToBytes(elseIfCode.getCodes());
                                stream.write(convertIntegerToBytes(cs.length));
                                stream.write(cs);
                                stream.write(new byte[]{0x6e});
                            } else if (elseCode instanceof Codes.Else) {
                                stream.write(new byte[]{0x54});
                                Codes.Else lastElseCode = (Codes.Else) elseCode;
                                stream.write(new byte[]{0x01});
                                stream.write(new byte[]{0x6f});
                                cs = convertCodeToBytes(lastElseCode.getCodes());
                                stream.write(convertIntegerToBytes(cs.length));
                                stream.write(cs);
                                stream.write(new byte[]{0x6e});
                            }
                        }
                    }
                } else if (code instanceof Codes.CounterFor) {
                    stream.write(new byte[]{0x53});
                    stream.write(new byte[]{0x01});
                    stream.write(convertExpressionToBytes(((Codes.CounterFor) code).getLimit()));
                    stream.write(new byte[]{0x02});
                    stream.write(convertExpressionToBytes(((Codes.CounterFor) code).getStep()));
                    stream.write(new byte[]{0x03});
                    stream.write(new byte[]{0x6f});
                    byte[] cs = convertCodeToBytes(((Codes.CounterFor) code).getCodes());
                    stream.write(convertIntegerToBytes(cs.length));
                    stream.write(cs);
                    stream.write(new byte[]{0x6e});
                } else if (code instanceof Codes.While) {
                    stream.write(new byte[]{0x54});
                    stream.write(new byte[]{0x01});
                    stream.write(convertExpressionToBytes(((Codes.While) code).getCondition()));
                    stream.write(new byte[]{0x02});
                    stream.write(new byte[]{0x6f});
                    byte[] cs = convertCodeToBytes(((Codes.While) code).getCodes());
                    stream.write(convertIntegerToBytes(cs.length));
                    stream.write(cs);
                    stream.write(new byte[]{0x6e});
                } else if (code instanceof Codes.Foreach) {
                    stream.write(new byte[]{0x5a});
                    stream.write(new byte[]{0x01});
                    stream.write(convertExpressionToBytes(((Codes.Foreach) code).getCollection()));
                    stream.write(new byte[]{0x02});
                    stream.write(convertExpressionToBytes(((Codes.Foreach) code).getTemp()));
                    stream.write(new byte[]{0x03});
                    stream.write(new byte[]{0x6f});
                    byte[] cs = convertCodeToBytes(((Codes.Foreach) code).getCodes());
                    stream.write(convertIntegerToBytes(cs.length));
                    stream.write(cs);
                    stream.write(new byte[]{0x6e});
                } else if (code instanceof Codes.Call) {
                    stream.write(new byte[]{0x55});
                    stream.write(new byte[]{0x01});
                    stream.write(convertExpressionToBytes(((Codes.Call) code).getFuncReference()));
                    stream.write(new byte[]{0x02});
                    stream.write(convertIntegerToBytes(((Codes.Call) code).getEntries().size()));
                    for (Map.Entry<String, Codes.Code> entry : ((Codes.Call) code).getEntries().entrySet()) {
                        stream.write(new byte[]{0x03});
                        byte[] keyBytes = entry.getKey().getBytes();
                        stream.write(convertIntegerToBytes(keyBytes.length + 1));
                        stream.write(keyBytes);
                        stream.write('\0');
                        byte[] valueBytes = convertExpressionToBytes(entry.getValue());
                        stream.write(convertIntegerToBytes(valueBytes.length));
                        stream.write(valueBytes);
                    }
                } else if (code instanceof Codes.Assignment) {
                    stream.write(new byte[]{0x56});
                    stream.write(new byte[]{0x01});
                    stream.write(convertExpressionToBytes(((Codes.Assignment) code).getVar()));
                    stream.write(new byte[]{0x02});
                    stream.write(convertExpressionToBytes(((Codes.Assignment) code).getValue()));
                } else if (code instanceof Codes.Instantiate) {
                    stream.write(new byte[]{0x57});
                    stream.write(new byte[]{0x01});
                    stream.write(convertExpressionToBytes(((Codes.Instantiate) code).getClassReference()));
                    stream.write(new byte[]{0x02});
                    stream.write(convertIntegerToBytes(((Codes.Instantiate) code).getEntries().size()));
                    for (Map.Entry<String, Codes.Code> entry : ((Codes.Instantiate) code).getEntries().entrySet()) {
                        stream.write(new byte[]{0x03});
                        byte[] keyBytes = entry.getKey().getBytes();
                        stream.write(convertIntegerToBytes(keyBytes.length + 1));
                        stream.write(keyBytes);
                        stream.write('\0');
                        byte[] valueBytes = convertExpressionToBytes(entry.getValue());
                        stream.write(convertIntegerToBytes(valueBytes.length));
                        stream.write(valueBytes);
                    }
                } else if (code instanceof Codes.Class) {
                    stream.write(new byte[]{0x58});
                    stream.write(new byte[]{0x01});
                    byte[] nameBytes = ((Codes.Class) code).getName().getBytes();
                    stream.write(convertIntegerToBytes(nameBytes.length + 1));
                    stream.write(nameBytes);
                    stream.write('\0');
                    stream.write(new byte[]{0x02});
                    stream.write(convertIntegerToBytes(((Codes.Class) code).getInheritance().size()));
                    for (Codes.Identifier entry : ((Codes.Class) code).getInheritance()) {
                        byte[] keyBytes = entry.getName().getBytes();
                        stream.write(convertIntegerToBytes(keyBytes.length + 1));
                        stream.write(keyBytes);
                        stream.write('\0');
                    }
                    stream.write(new byte[]{0x03});
                    stream.write(convertIntegerToBytes(((Codes.Class) code).getBehavior().size()));
                    for (Codes.Identifier entry : ((Codes.Class) code).getBehavior()) {
                        byte[] keyBytes = entry.getName().getBytes();
                        stream.write(convertIntegerToBytes(keyBytes.length + 1));
                        stream.write(keyBytes);
                        stream.write('\0');
                    }
                    stream.write(new byte[]{0x04});
                    stream.write(convertIntegerToBytes(((Codes.Class) code).getProperties().size()));
                    for (Codes.Prop prop : ((Codes.Class) code).getProperties()) {
                        byte[] propName = prop.getId().getName().getBytes();
                        stream.write(convertIntegerToBytes(propName.length + 1));
                        stream.write(propName);
                        stream.write('\0');
                        byte[] propValue = convertExpressionToBytes(prop.getValue());
                        stream.write(convertIntegerToBytes(propValue.length));
                        stream.write(propValue);
                    }
                    stream.write(new byte[]{0x05});
                    stream.write(convertIntegerToBytes(((Codes.Class) code).getFunctions().size()));
                    for (Codes.Function func : ((Codes.Class) code).getFunctions()) {
                        stream.write(new byte[]{0x51});
                        stream.write(new byte[]{0x01});
                        byte[] name = func.getName().getBytes();
                        stream.write(convertIntegerToBytes(name.length + 1));
                        stream.write(name);
                        stream.write('\0');
                        stream.write(new byte[]{0x02});
                        byte[] level = func.getLevel().toString().getBytes();
                        stream.write(convertIntegerToBytes(level.length + 1));
                        stream.write(level);
                        stream.write('\0');
                        stream.write(new byte[]{0x03});
                        stream.write(convertIntegerToBytes(func.getParams().size()));
                        for (Codes.Identifier id : func.getParams()) {
                            byte[] idName = id.getName().getBytes();
                            stream.write(convertIntegerToBytes(idName.length));
                            stream.write(idName);
                        }
                        stream.write(new byte[]{0x04});
                        stream.write(new byte[]{0x6f});
                        byte[] cs = convertCodeToBytes(func.getCodes());
                        stream.write(convertIntegerToBytes(cs.length));
                        stream.write(cs);
                        stream.write(new byte[]{0x6e});
                    }
                    stream.write(new byte[]{0x05});
                    Codes.Constructor constructor = ((Codes.Class) code).getConstructor();
                    stream.write(convertIntegerToBytes(constructor.getParams().size()));
                    for (Codes.Identifier id : constructor.getParams()) {
                        byte[] idNameBytes = id.getName().getBytes();
                        stream.write(convertIntegerToBytes(idNameBytes.length + 1));
                        stream.write(idNameBytes);
                        stream.write('\0');
                    }
                    stream.write(new byte[]{0x06});
                    stream.write(new byte[]{0x6f});
                    byte[] cs = convertCodeToBytes(constructor.getBody());
                    stream.write(convertIntegerToBytes(cs.length));
                    stream.write(cs);
                    stream.write(new byte[]{0x6e});
                } else if (code instanceof Codes.Return) {
                    stream.write(new byte[]{0x59});
                    stream.write(new byte[]{0x01});
                    stream.write(convertExpressionToBytes(((Codes.Return) code).getValue()));
                }
            }
        } catch (IOException ex) {
            ex.printStackTrace();
        }
        return stream.toByteArray();
    }

    private static byte[] convertExpressionToBytes(Codes.Code exp) {
        ByteArrayOutputStream stream = new ByteArrayOutputStream();
        try {
            if (exp instanceof Codes.MathExpSum) {
                stream.write(new byte[]{0x71});
                stream.write(new byte[]{0x01});
                stream.write(convertExpressionToBytes(((Codes.MathExpSum) exp).getValue1()));
                stream.write(new byte[]{0x02});
                stream.write(convertExpressionToBytes(((Codes.MathExpSum) exp).getValue2()));
                return stream.toByteArray();
            } else if (exp instanceof Codes.MathExpSubstract) {
                stream.write(new byte[]{0x72});
                stream.write(new byte[]{0x01});
                stream.write(convertExpressionToBytes(((Codes.MathExpSubstract) exp).getValue1()));
                stream.write(new byte[]{0x02});
                stream.write(convertExpressionToBytes(((Codes.MathExpSubstract) exp).getValue2()));
                return stream.toByteArray();
            } else if (exp instanceof Codes.MathExpMultiply) {
                stream.write(new byte[]{0x73});
                stream.write(new byte[]{0x01});
                stream.write(convertExpressionToBytes(((Codes.MathExpMultiply) exp).getValue1()));
                stream.write(new byte[]{0x02});
                stream.write(convertExpressionToBytes(((Codes.MathExpMultiply) exp).getValue2()));
                return stream.toByteArray();
            } else if (exp instanceof Codes.MathExpDivide) {
                stream.write(new byte[]{0x74});
                stream.write(new byte[]{0x01});
                stream.write(convertExpressionToBytes(((Codes.MathExpDivide) exp).getValue1()));
                stream.write(new byte[]{0x02});
                stream.write(convertExpressionToBytes(((Codes.MathExpDivide) exp).getValue2()));
                return stream.toByteArray();
            } else if (exp instanceof Codes.MathExpMod) {
                stream.write(new byte[]{0x75});
                stream.write(new byte[]{0x01});
                stream.write(convertExpressionToBytes(((Codes.MathExpMod) exp).getValue1()));
                stream.write(new byte[]{0x02});
                stream.write(convertExpressionToBytes(((Codes.MathExpMod) exp).getValue2()));
                return stream.toByteArray();
            } else if (exp instanceof Codes.MathExpPower) {
                stream.write(new byte[]{0x76});
                stream.write(new byte[]{0x01});
                stream.write(convertExpressionToBytes(((Codes.MathExpPower) exp).getValue1()));
                stream.write(new byte[]{0x02});
                stream.write(convertExpressionToBytes(((Codes.MathExpPower) exp).getValue2()));
                return stream.toByteArray();
            } else if (exp instanceof Codes.MathExpAnd) {
                stream.write(new byte[]{0x77});
                stream.write(new byte[]{0x01});
                stream.write(convertExpressionToBytes(((Codes.MathExpAnd) exp).getValue1()));
                stream.write(new byte[]{0x02});
                stream.write(convertExpressionToBytes(((Codes.MathExpAnd) exp).getValue2()));
                return stream.toByteArray();
            } else if (exp instanceof Codes.MathExpOr) {
                stream.write(new byte[]{0x78});
                stream.write(new byte[]{0x01});
                stream.write(convertExpressionToBytes(((Codes.MathExpOr) exp).getValue1()));
                stream.write(new byte[]{0x02});
                stream.write(convertExpressionToBytes(((Codes.MathExpOr) exp).getValue2()));
                return stream.toByteArray();
            } else if (exp instanceof Codes.MathExpEqual) {
                stream.write(new byte[]{0x79});
                stream.write(new byte[]{0x01});
                stream.write(convertExpressionToBytes(((Codes.MathExpEqual) exp).getValue1()));
                stream.write(new byte[]{0x02});
                stream.write(convertExpressionToBytes(((Codes.MathExpEqual) exp).getValue2()));
                return stream.toByteArray();
            } else if (exp instanceof Codes.MathExpGT) {
                stream.write(new byte[]{0x7a});
                stream.write(new byte[]{0x01});
                stream.write(convertExpressionToBytes(((Codes.MathExpGT) exp).getValue1()));
                stream.write(new byte[]{0x02});
                stream.write(convertExpressionToBytes(((Codes.MathExpGT) exp).getValue2()));
                return stream.toByteArray();
            } else if (exp instanceof Codes.MathExpGE) {
                stream.write(new byte[]{0x7b});
                stream.write(new byte[]{0x01});
                stream.write(convertExpressionToBytes(((Codes.MathExpGE) exp).getValue1()));
                stream.write(new byte[]{0x02});
                stream.write(convertExpressionToBytes(((Codes.MathExpGE) exp).getValue2()));
                return stream.toByteArray();
            } else if (exp instanceof Codes.MathExpNE) {
                stream.write(new byte[]{0x7c});
                stream.write(new byte[]{0x01});
                stream.write(convertExpressionToBytes(((Codes.MathExpNE) exp).getValue1()));
                stream.write(new byte[]{0x02});
                stream.write(convertExpressionToBytes(((Codes.MathExpNE) exp).getValue2()));
                return stream.toByteArray();
            } else if (exp instanceof Codes.MathExpLE) {
                stream.write(new byte[]{0x7d});
                stream.write(new byte[]{0x01});
                stream.write(convertExpressionToBytes(((Codes.MathExpLE) exp).getValue1()));
                stream.write(new byte[]{0x02});
                stream.write(convertExpressionToBytes(((Codes.MathExpLE) exp).getValue2()));
                return stream.toByteArray();
            } else if (exp instanceof Codes.MathExpLT) {
                stream.write(new byte[]{0x7e});
                stream.write(new byte[]{0x01});
                stream.write(convertExpressionToBytes(((Codes.MathExpLT) exp).getValue1()));
                stream.write(new byte[]{0x02});
                stream.write(convertExpressionToBytes(((Codes.MathExpLT) exp).getValue2()));
                return stream.toByteArray();
            } else if (exp instanceof Codes.MathExpNot) {
                stream.write(new byte[]{0x4f});
                stream.write(new byte[]{0x01});
                stream.write(convertExpressionToBytes(((Codes.MathExpNot) exp).getValue()));
            } else if (exp instanceof Codes.Call) {
                stream.write(new byte[]{0x55});
                stream.write(new byte[]{0x01});
                stream.write(convertExpressionToBytes(((Codes.Call) exp).getFuncReference()));
                stream.write(new byte[]{0x02});
                stream.write(convertIntegerToBytes(((Codes.Call) exp).getEntries().size()));
                for (Map.Entry<String, Codes.Code> entry : ((Codes.Call) exp).getEntries().entrySet()) {
                    stream.write(new byte[]{0x03});
                    byte[] keyBytes = entry.getKey().getBytes();
                    stream.write(convertIntegerToBytes(keyBytes.length + 1));
                    stream.write(keyBytes);
                    stream.write('\0');
                    byte[] valueBytes = convertExpressionToBytes(entry.getValue());
                    stream.write(convertIntegerToBytes(valueBytes.length));
                    stream.write(valueBytes);
                }
            } else if (exp instanceof Codes.Instantiate) {
                stream.write(new byte[]{0x57});
                stream.write(new byte[]{0x01});
                stream.write(convertExpressionToBytes(((Codes.Instantiate) exp).getClassReference()));
                stream.write(new byte[]{0x02});
                stream.write(convertIntegerToBytes(((Codes.Instantiate) exp).getEntries().size()));
                for (Map.Entry<String, Codes.Code> entry : ((Codes.Instantiate) exp).getEntries().entrySet()) {
                    stream.write(new byte[]{0x03});
                    byte[] keyBytes = entry.getKey().getBytes();
                    stream.write(convertIntegerToBytes(keyBytes.length + 1));
                    stream.write(keyBytes);
                    stream.write('\0');
                    byte[] valueBytes = convertExpressionToBytes(entry.getValue());
                    stream.write(convertIntegerToBytes(valueBytes.length));
                    stream.write(valueBytes);
                }
            } else if (exp instanceof Codes.Identifier) {
                stream.write(new byte[]{0x61});
                byte[] idName = ((Codes.Identifier) exp).getName().getBytes();
                stream.write(convertIntegerToBytes(idName.length + 1));
                stream.write(idName);
                stream.write('\0');
                return stream.toByteArray();
            } else if (exp instanceof Codes.Array) {
                stream.write(new byte[]{0x4b});
                stream.write(new byte[]{0x1});
                stream.write(convertIntegerToBytes(((Codes.Array) exp).getItems().size()));
                for (Codes.Code item : ((Codes.Array) exp).getItems()) {
                    stream.write(new byte[]{0x02});
                    stream.write(convertExpressionToBytes(item));
                }
                return stream.toByteArray();
            } else if (exp instanceof Codes.Reference) {
                stream.write(new byte[]{0x7f});
                stream.write(new byte[]{0x01});
                stream.write(convertExpressionToBytes(((Codes.Reference) exp).getCurrentChain()));
                if (((Codes.Reference) exp).getRestOfChains() != null) {
                    stream.write(new byte[]{0x02});
                    stream.write(convertExpressionToBytes(((Codes.Reference) exp).getRestOfChains()));
                }
                stream.write(new byte[]{0x6d});
                return stream.toByteArray();
            } else if (exp instanceof Codes.Index) {
                stream.write(new byte[]{0x6c});
                stream.write(new byte[]{0x01});
                stream.write(convertExpressionToBytes(((Codes.Index) exp).getVar()));
                stream.write(new byte[]{0x02});
                stream.write(convertIntegerToBytes(((Codes.Index) exp).getIndex().size()));
                for (Codes.Code id : ((Codes.Index) exp).getIndex()) {
                    stream.write(new byte[]{0x03});
                    stream.write(convertExpressionToBytes(id));
                }
                if (((Codes.Index) exp).getRestOfChains() != null) {
                    stream.write(new byte[]{0x04});
                    stream.write(convertExpressionToBytes(((Codes.Index) exp).getRestOfChains()));
                }
                stream.write(new byte[]{0x6b});
                return stream.toByteArray();
            } else if (exp instanceof Codes.Period) {
                stream.write(new byte[]{0x6a});
                stream.write(new byte[]{0x01});
                stream.write(convertExpressionToBytes(((Codes.Period) exp).getStart()));
                stream.write(new byte[]{0x02});
                stream.write(convertExpressionToBytes(((Codes.Period) exp).getEnd()));
                stream.write(new byte[]{0x69});
                return stream.toByteArray();
            } else if (exp instanceof Codes.Value) {
                if (((Codes.Value) exp).getValue() instanceof String) {
                    stream.write(new byte[]{0x62});
                    byte[] value = ((String) ((Codes.Value) exp).getValue()).getBytes();
                    stream.write(convertIntegerToBytes(value.length + 1));
                    stream.write(value);
                    stream.write('\0');
                } else if (((Codes.Value) exp).getValue() instanceof Double) {
                    stream.write(new byte[]{0x63});
                    stream.write(convertDoubleToBytes((Double)((Codes.Value) exp).getValue()));
                } else if (((Codes.Value) exp).getValue() instanceof Float) {
                    stream.write(new byte[]{0x64});
                    stream.write(convertFloatToBytes((Float)((Codes.Value) exp).getValue()));
                } else if (((Codes.Value) exp).getValue() instanceof Short) {
                    stream.write(new byte[]{0x65});
                    stream.write(convertShortToBytes((Short)((Codes.Value) exp).getValue()));
                } else if (((Codes.Value) exp).getValue() instanceof Integer) {
                    stream.write(new byte[]{0x66});
                    stream.write(convertIntegerToBytes((Integer)((Codes.Value) exp).getValue()));
                } else if (((Codes.Value) exp).getValue() instanceof Long) {
                    stream.write(new byte[]{0x67});
                    stream.write(convertLongToBytes((Long)((Codes.Value) exp).getValue()));
                } else if (((Codes.Value) exp).getValue() instanceof Boolean) {
                    stream.write(new byte[]{0x68});
                    stream.write(convertBooleanToBytes((Boolean) ((Codes.Value) exp).getValue()));
                }
                return stream.toByteArray();
            } else if (exp instanceof Codes.AnonymousObject) {
                stream.write(new byte[]{0x4e});
                stream.write(convertIntegerToBytes(((Codes.AnonymousObject) exp).getContent().size()));
                for (Map.Entry<String, Codes.Code> entry : ((Codes.AnonymousObject) exp).getContent().entrySet()) {
                    stream.write(new byte[]{0x01});
                    byte[] keyBytes = entry.getKey().getBytes();
                    stream.write(convertIntegerToBytes(keyBytes.length + 1));
                    stream.write(keyBytes);
                    stream.write('\0');
                    stream.write(convertExpressionToBytes(entry.getValue()));
                }
            } else if (exp instanceof Codes.Empty) {
                stream.write(0x4d);
            }
        } catch (Exception ex) {
            ex.printStackTrace();
        }
        return stream.toByteArray();
    }

    private HashSet<String> waysCache = new HashSet<>();

    public List<String> parse() throws Exception {
        dependencies = new ArrayList<>();
        path.pathStack.clear();
        ArrayList<Symbol> tokenList = new ArrayList<>();
        Symbol token;
        try {
            while ((token = this.lexer.next_token()).value != null) {
                if (token.sym != sym.STRING && token.value instanceof String) {
                    token.value = ((String) token.value).trim();
                }
                tokenList.add(token);
                print(token.value);
            }
            tokenList.add(new Symbol(sym.EOF, 0, 0, null));
        } catch (IOException ex) {
            ex.printStackTrace();
        }
        Switch switchPoint = new Switch();
        switchPoint.point = root;
        this.path.addSwitch(switchPoint);
        this.parseCode(tokenList.toArray(new Symbol[0]));
        List<Codes.Code> codes = new ArrayList<>();
        while (!path.pathStack.isEmpty()) {
            Switch sw = path.pathStack.peek().first;
            while (!sw.reachedRuleEnd()) {
                if (sw.currentRuleToken() instanceof Point &&
                        isConnectedToEpsilon((Point) sw.currentRuleToken())) {
                    sw.currentRuleTokenIndex++;
                }
                else {
                    break;
                }
            }
            Object result = path.pathStack.peek().first.currentRule().action.act(path.pathStack.peek().second);
            path.pathStack.pop();
            if (!path.pathStack.isEmpty()) {
                path.pathStack.peek().second.add(result);
                path.pathStack.peek().first.currentRuleTokenIndex++;
            } else {
                codes = (List<Codes.Code>)result;
                notifyResultFetched(result);
            }
        }
        try {
            File finalFile = new File("output/" + filePath.substring(0, filePath.length() - ".elpis".length()) + ".elp");
            finalFile.getParentFile().mkdirs();
            finalFile.delete();
            finalFile.createNewFile();
            FileOutputStream stream = new FileOutputStream(finalFile);
            stream.write(convertCodeToBytes(codes));
            stream.flush();
            stream.close();
        } catch (Exception ex) {
            ex.printStackTrace();
        }
        return dependencies;
    }

    public void notifyResultFetched(Object result) {
        compiled = true;
        String uglyJSONString = JsonHelper.toJson(result);
        Gson gson = new GsonBuilder().setPrettyPrinting().create();
        JsonParser jp = new JsonParser();
        JsonElement je = jp.parse(uglyJSONString);
        System.out.println(gson.toJson(je));
    }

    boolean isConnectedToEpsilon(Point point) {
        if (point.name.equals("epsilon")) return true;
        for (Rule rule : point.connections) {
            if (rule.sentence.size() == 1 && rule.sentence.get(0) instanceof Point) {
                if (((Point) rule.sentence.get(0)).name.equals("epsilon")) {
                    path.pathStack.peek().second.add(rule.action.act(new ArrayList<>()));
                    return true;
                }
                else if (isConnectedToEpsilon((Point) rule.sentence.get(0))) {
                    return true;
                }
            }
        }
        return false;
    }

    private void print(Object text) {
        System.out.println(text);
    }

    boolean foundInConnections(Symbol token, HashSet<String> blacklist, ArrayList<Rule> rules) {
        for (Rule rule : rules) {
            if (rule.sentence.get(0) instanceof String) {
                print(rule.sentence.get(0) + " " + token.value);
                if ((token.sym == sym.WORD && rule.sentence.get(0).equals("identifier") && ((String) token.value).matches("[A-Za-z0-9$]+")) ||
                        symbolOfToken(token).equals(rule.sentence.get(0))) {
                    return true;
                }
            }
            else if (rule.sentence.get(0) instanceof Point) {
                blacklist.add(((Point)rule.sentence.get(0)).name);
                if (foundInConnections(token, blacklist, ((Point)rule.sentence.get(0)).connections)) {
                    return true;
                }
            }
        }
        return false;
    }

    boolean matchedUpperRule(Symbol[] tokens, int pointer, Switch switchPoint) {
        print("checking " + switchPoint.point.name + "... " + sym.terminalNames[tokens[pointer].sym]);
        if (switchPoint.reachedRuleStorageEnd() || switchPoint.reachedRuleEnd()) {
            for (int ruleCounter = 0; ruleCounter < switchPoint.point.connections.size(); ruleCounter++) {
                if (switchPoint.point.connections.get(ruleCounter).sentence.size() > 1 &&
                        switchPoint.point.connections.get(ruleCounter).sentence.get(1) instanceof Point)
                    print(((Point)switchPoint.point.connections.get(ruleCounter).sentence.get(1)).name + " " +
                        sym.terminalNames[tokens[pointer].sym]);
                if (switchPoint.point.connections.get(ruleCounter).sentence.get(0) instanceof Point &&
                        ((Point) switchPoint.point.connections.get(ruleCounter).sentence.get(0)).name.equals(switchPoint.point.name) &&
                        ((switchPoint.point.connections.get(ruleCounter).sentence.get(1) instanceof String &&
                                ((switchPoint.point.connections.get(ruleCounter).sentence.get(1).equals(sym.terminalNames[sym.IDENTIFIER]) &&
                                        ((String) tokens[pointer].value).matches("[A-Za-z0-9$]+]") &&
                                        tokens[pointer].sym == sym.WORD) ||
                                        switchPoint.point.connections.get(ruleCounter).sentence.get(1).equals(tokens[pointer].value) ||
                                        switchPoint.point.connections.get(ruleCounter).sentence.get(1).equals(sym.terminalNames[tokens[pointer].sym]))) ||
                                ((switchPoint.point.connections.get(ruleCounter).sentence.get(1) instanceof Point &&
                                        (foundInConnections(tokens[pointer], new HashSet<>(),
                                                ((Point) switchPoint.point.connections.get(ruleCounter)
                                                        .sentence.get(1)).connections)))))) {
                    Object result = path.pathStack.peek().first.currentRule().action.act(path.pathStack.peek().second);
                    path.pathStack.pop();
                    print("found upper rule match.");
                    Switch sp = new Switch();
                    sp.point = switchPoint.point;
                    sp.currentRuleIndex = ruleCounter;
                    sp.currentRuleTokenIndex = 1;
                    sp.startedAt = pointer;
                    path.addSwitch(sp);
                    path.pathStack.peek().second.add(result);
                    return true;
                }
            }
        }
        return false;
    }

    private boolean investigateLoop(HashSet<String> blacklist, Symbol firstToken, Point node) {
        for (Rule subNodesAction : node.connections) {
            Object obj = subNodesAction.sentence.get(0);
            if (obj instanceof Point) {
                if (blacklist.contains(((Point) obj).name)) continue;
                else blacklist.add(((Point) obj).name);
                if (investigateLoop(blacklist, firstToken, (Point) obj)) {
                    return true;
                }
            } else if (obj instanceof String) {
                if ((firstToken.sym == sym.WORD && obj.equals("identifier") && ((String) firstToken.value).matches("[A-Za-z0-9$]+")) ||
                    symbolOfToken(firstToken).equals(obj)) {
                    return true;
                }
            }
        }
        return false;
    }

    boolean tempFailure = false;

    void handleRuleEnd(Symbol[] tokens, int pointer, Switch switchPoint) throws Exception {
        while ((switchPoint.reachedRuleStorageEnd() || switchPoint.reachedRuleEnd()) && !path.pathStack.isEmpty()) {
            if (matchedUpperRule(tokens, pointer, switchPoint)) return;
            if (switchPoint.reachedRuleStorageEnd()) {
                Pair<Switch, ArrayList<Object>> prev = path.pathStack.pop();
                if (!path.pathStack.isEmpty()) {
                    tempFailure = true;
                    print("A. getting back from " + prev.first.point.name + " to " + path.pathStack.peek().first.point.name);
                    switchPoint = path.pathStack.peek().first;
                    switchPoint.currentRuleTokenIndex++;
                }
            }
            if (matchedUpperRule(tokens, pointer, switchPoint)) return;
            if (!switchPoint.reachedRuleStorageEnd() && switchPoint.reachedRuleEnd()) {
                if (!path.pathStack.isEmpty()) {
                    if (tempFailure) {
                        print("covering temp failure...");
                        switchPoint = path.pathStack.peek().first;
                        switchPoint.currentRuleTokenIndex = 0;
                        switchPoint.currentRuleIndex++;
                        path.pathStack.peek().second.clear();
                        tempFailure = false;
                    }
                    else {
                        Pair<Switch, ArrayList<Object>> prev = path.pathStack.pop();
                        print("B. getting back from " + prev.first.point.name + " to " + path.pathStack.peek().first.point.name);
                        switchPoint = path.pathStack.peek().first;
                        switchPoint.currentRuleTokenIndex++;
                        Object result = prev.first.currentRule().action.act(prev.second);
                        path.pathStack.peek().second.add(result);
                    }
                }
                else {
                    Pair<Switch, ArrayList<Object>> prev = path.pathStack.pop();
                    Object result = prev.first.currentRule().action.act(prev.second);
                    notifyResultFetched(result);
                }
            }
            if (matchedUpperRule(tokens, pointer, switchPoint)) return;
        }
    }

    boolean compiled = false;

    private boolean parseCode(Symbol[] tokens) throws Exception {
        int pointer = 0;
        int counter = 0;
        clearScreen();
        while (true) {
            counter++;
            Switch switchPoint = path.pathStack.peek().first;
            print("using " + switchPoint.point.name);
            handleRuleEnd(tokens, pointer, switchPoint);
            if (tokens[pointer].sym == sym.EOF) {
                print("EOF seen.");
                while (switchPoint.currentRuleTokenIndex < switchPoint.currentRule().sentence.size()) {
                    if (switchPoint.currentRuleToken() instanceof Point &&
                            isConnectedToEpsilon((Point) switchPoint.currentRuleToken())) {
                        switchPoint.currentRuleTokenIndex++;
                    }
                    else {
                        break;
                    }
                }
                if (switchPoint.reachedRuleEnd()) {
                    compiled = true;
                    return true;
                }
                else {
                    return false;
                }
            }
            switchPoint = path.pathStack.peek().first;
            print("using " + switchPoint.point.name);
            Object ruleToken = switchPoint.currentRuleToken();
            if (ruleToken instanceof String) {
                print("comparing " + ruleToken + " with " + tokens[pointer].value + " : " + sym.terminalNames[tokens[pointer].sym]);
                print("fileName : " + fileName + ", line and column : " + tokens[pointer].left + " : " + tokens[pointer].right);
                if ((ruleToken.equals("string") && tokens[pointer].sym == sym.STRING) ||
                        (ruleToken.equals("number") && tokens[pointer].sym == sym.NUMBER) ||
                        (ruleToken.equals("true") && tokens[pointer].sym == sym.TRUE) ||
                        (ruleToken.equals("false") && tokens[pointer].sym == sym.FALSE) ||
                        (ruleToken.equals("identifier") && tokens[pointer].sym == sym.IDENTIFIER) ||
                        (ruleToken.equals("start") && tokens[pointer].sym == sym.START) ||
                        (ruleToken.equals("end") && tokens[pointer].sym == sym.END) ||
                        (ruleToken.equals("annot") && tokens[pointer].sym == sym.ANNOT) ||
                        (ruleToken.equals("empty") && tokens[pointer].sym == sym.EMPTY) ||
                        (ruleToken.equals("word") && tokens[pointer].sym == sym.WORD) ||
                        (tokens[pointer].sym == sym.WORD && ruleToken.equals("identifier") &&
                                ((String) tokens[pointer].value).matches("[A-Za-z0-9$]+")) ||
                        symbolOfToken(tokens[pointer]).equals(ruleToken)) {
                    print("matched.");
                    if (ruleToken.equals("word") && tokens[pointer].sym == sym.WORD) {
                        path.pathStack.peek().second.add(tokens[pointer].value);
                        print("found word");
                    }
                    else if (ruleToken.equals("identifier") && tokens[pointer].sym == sym.WORD) {
                        Codes.Identifier id = new Codes.Identifier();
                        id.setName((String) tokens[pointer].value);
                        path.pathStack.peek().second.add(id);
                        print("found identifier");
                    }
                    else if (tokens[pointer].sym == sym.NUMBER ||
                            tokens[pointer].sym == sym.STRING ||
                            tokens[pointer].sym == sym.TRUE ||
                            tokens[pointer].sym == sym.FALSE) {
                        Codes.Value value = new Codes.Value();
                        value.setValue(tokens[pointer].value);
                        path.pathStack.peek().second.add(value);
                        print("found value");
                    }
                    pointer++;
                    switchPoint.currentRuleTokenIndex++;
                } else {
                    print("not matched.");

                    String wayId = pointer + "_" + switchPoint.currentRule().id;
                    print(wayId);
                    if (waysCache.contains(wayId)) {
                        throw new Exception("syntax error. " + "line: " + tokens[pointer].left + ", column: " + tokens[pointer].right);
                    }
                    else {
                        waysCache.add(wayId);
                    }

                    pointer = switchPoint.startedAt;
                    switchPoint.currentRuleTokenIndex = 0;
                    switchPoint.currentRuleIndex++;
                    path.pathStack.peek().second.clear();
                    print("switched rule of " + switchPoint.point.name + " from " + (switchPoint.currentRuleIndex - 1) + " to " + switchPoint.currentRuleIndex);
                }
            } else if (ruleToken instanceof Point) {
                HashSet<String> blacklist = new HashSet<>();
                boolean selected = false;
                while (!switchPoint.reachedRuleEnd()) {
                    ruleToken = switchPoint.currentRuleToken();
                    if (ruleToken instanceof Point &&
                            !investigateLoop(blacklist, tokens[pointer], (Point) ruleToken) &&
                            isConnectedToEpsilon((Point)ruleToken)) {
                        selected = true;
                        switchPoint.currentRuleTokenIndex++;
                    }
                    else {
                        break;
                    }
                }
                if (!selected) {
                    blacklist.clear();
                    if (investigateLoop(blacklist, tokens[pointer], (Point) ruleToken)) {
                        if (((Point) ruleToken).name.equals("epsilon")) {
                            Pair<Switch, ArrayList<Object>> prev = path.pathStack.pop();
                            Object result = prev.first.currentRule().action.act(prev.second);
                            path.pathStack.peek().second.add(result);
                            print("D. getting back from " + prev.first.point.name + " to " + path.pathStack.peek().first.point.name);
                            switchPoint = path.pathStack.peek().first;
                            switchPoint.currentRuleTokenIndex++;
                        } else {
                            print("opening " + ((Point) ruleToken).name);
                            Switch sp = new Switch();
                            sp.point = (Point) ruleToken;
                            sp.startedAt = pointer;
                            path.addSwitch(sp);
                        }
                    }
                    else {
                        print("not matched.");
                        pointer = switchPoint.startedAt;
                        switchPoint.currentRuleTokenIndex = 0;
                        switchPoint.currentRuleIndex++;
                        path.pathStack.peek().second.clear();
                        print("switched rule of " + switchPoint.point.name + " from " + (switchPoint.currentRuleIndex - 1) + " to " + switchPoint.currentRuleIndex);
                    }
                }
            }
        }
    }

    public static void clearScreen() {
        System. out. print("\033[H\033[2J");
        System. out. flush();
    }

    private String symbolOfToken(Symbol token) {
        if ((token.sym == sym.STRING) ||
                (token.sym == sym.NUMBER) ||
                (token.sym == sym.TRUE) ||
                (token.sym == sym.FALSE) ||
                (token.sym == sym.IDENTIFIER) ||
                (token.sym == sym.START) ||
                (token.sym == sym.END) ||
                (token.sym == sym.ANNOT) ||
                (token.sym == sym.EMPTY) ||
                (token.sym == sym.EOF)) {
            return sym.terminalNames[token.sym];
        }
        else {
            return token.value.toString();
        }
    }
}
