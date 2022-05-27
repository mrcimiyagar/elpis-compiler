//package tra.v3;
//
//import com.google.gson.Gson;
//import com.google.gson.GsonBuilder;
//import com.google.gson.JsonElement;
//import com.google.gson.JsonParser;
//import java_cup.runtime.Symbol;
//import tra.helpers.JsonHelper;
//import tra.models.*;
//import tra.models.Action;
//import tra.models.temp.Point;
//
//import java.io.*;
//import java.lang.ref.Reference;
//import java.nio.ByteBuffer;
//import java.util.*;
//import java.util.List;
//import java.util.concurrent.ConcurrentLinkedQueue;
//
//public class TraParser {
//
//    private TraLexer lexer;
//    private Node mainNode = new Node("mainNode");
//    Node epsilon = new Node("epsilon");
//    static List<String> dependencies = new ArrayList<>();
//
//    private byte[] convertCodeToBytes(List<Codes.Code> codes) {
//        ByteArrayOutputStream stream = new ByteArrayOutputStream();
//        try {
//            for (Codes.Code code : codes) {
//                if (code instanceof Codes.Function) {
//                    stream.write(new byte[]{0x51});
//                    Codes.Function func = (Codes.Function) code;
//                    stream.write(new byte[]{0x01});
//                    byte[] name = func.getName().getBytes();
//                    stream.write(convertIntegerToBytes(name.length + 1));
//                    stream.write(name);
//                    stream.write('\0');
//                    stream.write(new byte[]{0x02});
//                    byte[] level = func.getLevel().toString().getBytes();
//                    stream.write(convertIntegerToBytes(level.length + 1));
//                    stream.write(level);
//                    stream.write('\0');
//                    stream.write(new byte[]{0x03});
//                    stream.write(convertIntegerToBytes(func.getParams().size()));
//                    for (Codes.Identifier id : func.getParams()) {
//                        byte[] idName = id.getName().getBytes();
//                        stream.write(convertIntegerToBytes(idName.length + 1));
//                        stream.write(idName);
//                        stream.write('\0');
//                    }
//                    stream.write(new byte[]{0x04});
//                    stream.write(new byte[]{0x6f});
//                    byte[] cs = convertCodeToBytes(func.getCodes());
//                    stream.write(convertIntegerToBytes(cs.length));
//                    stream.write(cs);
//                    stream.write(new byte[]{0x6e});
//                } else if (code instanceof Codes.Break) {
//                    stream.write(0x4c);
//                } else if (code instanceof Codes.If) {
//                    stream.write(new byte[]{0x52});
//                    Codes.If ifCode = (Codes.If) code;
//                    stream.write(new byte[]{0x01});
//                    stream.write(convertExpressionToBytes(ifCode.getCondition()));
//                    stream.write(new byte[]{0x02});
//                    stream.write(new byte[]{0x6f});
//                    byte[] cs = convertCodeToBytes(ifCode.getCodes());
//                    stream.write(convertIntegerToBytes(cs.length));
//                    stream.write(cs);
//                    stream.write(new byte[]{0x6e});
//                    if (ifCode.getExtras() != null) {
//                        stream.write(new byte[] {0x03});
//                        stream.write(convertIntegerToBytes(ifCode.getExtras().size()));
//                        for (Codes.Code elseCode : ifCode.getExtras()) {
//                            if (elseCode instanceof Codes.ElseIf) {
//                                stream.write(new byte[]{0x53});
//                                Codes.ElseIf elseIfCode = (Codes.ElseIf) elseCode;
//                                stream.write(new byte[]{0x01});
//                                stream.write(convertExpressionToBytes(elseIfCode.getCondition()));
//                                stream.write(new byte[]{0x02});
//                                stream.write(new byte[]{0x6f});
//                                cs = convertCodeToBytes(elseIfCode.getCodes());
//                                stream.write(convertIntegerToBytes(cs.length));
//                                stream.write(cs);
//                                stream.write(new byte[]{0x6e});
//                            } else if (elseCode instanceof Codes.Else) {
//                                stream.write(new byte[]{0x54});
//                                Codes.Else lastElseCode = (Codes.Else) elseCode;
//                                stream.write(new byte[]{0x01});
//                                stream.write(new byte[]{0x6f});
//                                cs = convertCodeToBytes(lastElseCode.getCodes());
//                                stream.write(convertIntegerToBytes(cs.length));
//                                stream.write(cs);
//                                stream.write(new byte[]{0x6e});
//                            }
//                        }
//                    }
//                } else if (code instanceof Codes.CounterFor) {
//                    stream.write(new byte[]{0x53});
//                    stream.write(new byte[]{0x01});
//                    stream.write(convertExpressionToBytes(((Codes.CounterFor) code).getLimit()));
//                    stream.write(new byte[]{0x02});
//                    stream.write(convertExpressionToBytes(((Codes.CounterFor) code).getStep()));
//                    stream.write(new byte[]{0x03});
//                    stream.write(new byte[]{0x6f});
//                    byte[] cs = convertCodeToBytes(((Codes.CounterFor) code).getCodes());
//                    stream.write(convertIntegerToBytes(cs.length));
//                    stream.write(cs);
//                    stream.write(new byte[]{0x6e});
//                } else if (code instanceof Codes.While) {
//                    stream.write(new byte[]{0x54});
//                    stream.write(new byte[]{0x01});
//                    stream.write(convertExpressionToBytes(((Codes.While) code).getCondition()));
//                    stream.write(new byte[]{0x02});
//                    stream.write(new byte[]{0x6f});
//                    byte[] cs = convertCodeToBytes(((Codes.While) code).getCodes());
//                    stream.write(convertIntegerToBytes(cs.length));
//                    stream.write(cs);
//                    stream.write(new byte[]{0x6e});
//                } else if (code instanceof Codes.Foreach) {
//                    stream.write(new byte[]{0x5a});
//                    stream.write(new byte[]{0x01});
//                    stream.write(convertExpressionToBytes(((Codes.Foreach) code).getCollection()));
//                    stream.write(new byte[]{0x02});
//                    stream.write(convertExpressionToBytes(((Codes.Foreach) code).getTemp()));
//                    stream.write(new byte[]{0x03});
//                    stream.write(new byte[]{0x6f});
//                    byte[] cs = convertCodeToBytes(((Codes.Foreach) code).getCodes());
//                    stream.write(convertIntegerToBytes(cs.length));
//                    stream.write(cs);
//                    stream.write(new byte[]{0x6e});
//                } else if (code instanceof Codes.Call) {
//                    stream.write(new byte[]{0x55});
//                    stream.write(new byte[]{0x01});
//                    stream.write(convertExpressionToBytes(((Codes.Call) code).getFuncReference()));
//                    stream.write(new byte[]{0x02});
//                    stream.write(convertIntegerToBytes(((Codes.Call) code).getEntries().size()));
//                    for (Map.Entry<String, Codes.Code> entry : ((Codes.Call) code).getEntries().entrySet()) {
//                        stream.write(new byte[]{0x03});
//                        byte[] keyBytes = entry.getKey().getBytes();
//                        stream.write(convertIntegerToBytes(keyBytes.length + 1));
//                        stream.write(keyBytes);
//                        stream.write('\0');
//                        byte[] valueBytes = convertExpressionToBytes(entry.getValue());
//                        stream.write(convertIntegerToBytes(valueBytes.length));
//                        stream.write(valueBytes);
//                    }
//                } else if (code instanceof Codes.Assignment) {
//                    stream.write(new byte[]{0x56});
//                    stream.write(new byte[]{0x01});
//                    stream.write(convertExpressionToBytes(((Codes.Assignment) code).getVar()));
//                    stream.write(new byte[]{0x02});
//                    stream.write(convertExpressionToBytes(((Codes.Assignment) code).getValue()));
//                } else if (code instanceof Codes.Instantiate) {
//                    stream.write(new byte[]{0x57});
//                    stream.write(new byte[]{0x01});
//                    stream.write(convertExpressionToBytes(((Codes.Instantiate) code).getClassReference()));
//                    stream.write(new byte[]{0x02});
//                    stream.write(convertIntegerToBytes(((Codes.Instantiate) code).getEntries().size()));
//                    for (Map.Entry<String, Codes.Code> entry : ((Codes.Instantiate) code).getEntries().entrySet()) {
//                        stream.write(new byte[]{0x03});
//                        byte[] keyBytes = entry.getKey().getBytes();
//                        stream.write(convertIntegerToBytes(keyBytes.length + 1));
//                        stream.write(keyBytes);
//                        stream.write('\0');
//                        byte[] valueBytes = convertExpressionToBytes(entry.getValue());
//                        stream.write(convertIntegerToBytes(valueBytes.length));
//                        stream.write(valueBytes);
//                    }
//                } else if (code instanceof Codes.Class) {
//                    stream.write(new byte[]{0x58});
//                    stream.write(new byte[]{0x01});
//                    byte[] nameBytes = ((Codes.Class) code).getName().getBytes();
//                    stream.write(convertIntegerToBytes(nameBytes.length + 1));
//                    stream.write(nameBytes);
//                    stream.write('\0');
//                    stream.write(new byte[]{0x02});
//                    stream.write(convertIntegerToBytes(((Codes.Class) code).getInheritance().size()));
//                    for (Codes.Identifier entry : ((Codes.Class) code).getInheritance()) {
//                        byte[] keyBytes = entry.getName().getBytes();
//                        stream.write(convertIntegerToBytes(keyBytes.length + 1));
//                        stream.write(keyBytes);
//                        stream.write('\0');
//                    }
//                    stream.write(new byte[]{0x03});
//                    stream.write(convertIntegerToBytes(((Codes.Class) code).getBehavior().size()));
//                    for (Codes.Identifier entry : ((Codes.Class) code).getBehavior()) {
//                        byte[] keyBytes = entry.getName().getBytes();
//                        stream.write(convertIntegerToBytes(keyBytes.length + 1));
//                        stream.write(keyBytes);
//                        stream.write('\0');
//                    }
//                    stream.write(new byte[]{0x04});
//                    stream.write(convertIntegerToBytes(((Codes.Class) code).getProperties().size()));
//                    for (Codes.Prop prop : ((Codes.Class) code).getProperties()) {
//                        byte[] propName = prop.getId().getName().getBytes();
//                        stream.write(convertIntegerToBytes(propName.length + 1));
//                        stream.write(propName);
//                        stream.write('\0');
//                        byte[] propValue = convertExpressionToBytes(prop.getValue());
//                        stream.write(convertIntegerToBytes(propValue.length));
//                        stream.write(propValue);
//                    }
//                    stream.write(new byte[]{0x05});
//                    stream.write(convertIntegerToBytes(((Codes.Class) code).getFunctions().size()));
//                    for (Codes.Function func : ((Codes.Class) code).getFunctions()) {
//                        stream.write(new byte[]{0x51});
//                        stream.write(new byte[]{0x01});
//                        byte[] name = func.getName().getBytes();
//                        stream.write(convertIntegerToBytes(name.length + 1));
//                        stream.write(name);
//                        stream.write('\0');
//                        stream.write(new byte[]{0x02});
//                        byte[] level = func.getLevel().toString().getBytes();
//                        stream.write(convertIntegerToBytes(level.length + 1));
//                        stream.write(level);
//                        stream.write('\0');
//                        stream.write(new byte[]{0x03});
//                        stream.write(convertIntegerToBytes(func.getParams().size()));
//                        for (Codes.Identifier id : func.getParams()) {
//                            byte[] idName = id.getName().getBytes();
//                            stream.write(convertIntegerToBytes(idName.length));
//                            stream.write(idName);
//                        }
//                        stream.write(new byte[]{0x04});
//                        stream.write(new byte[]{0x6f});
//                        byte[] cs = convertCodeToBytes(func.getCodes());
//                        stream.write(convertIntegerToBytes(cs.length));
//                        stream.write(cs);
//                        stream.write(new byte[]{0x6e});
//                    }
//                    stream.write(new byte[]{0x05});
//                    Codes.Constructor constructor = ((Codes.Class) code).getConstructor();
//                    stream.write(convertIntegerToBytes(constructor.getParams().size()));
//                    for (Codes.Identifier id : constructor.getParams()) {
//                        byte[] idNameBytes = id.getName().getBytes();
//                        stream.write(convertIntegerToBytes(idNameBytes.length + 1));
//                        stream.write(idNameBytes);
//                        stream.write('\0');
//                    }
//                    stream.write(new byte[]{0x06});
//                    stream.write(new byte[]{0x6f});
//                    byte[] cs = convertCodeToBytes(constructor.getBody());
//                    stream.write(convertIntegerToBytes(cs.length));
//                    stream.write(cs);
//                    stream.write(new byte[]{0x6e});
//                } else if (code instanceof Codes.Return) {
//                    stream.write(new byte[]{0x59});
//                    stream.write(new byte[]{0x01});
//                    stream.write(convertExpressionToBytes(((Codes.Return) code).getValue()));
//                }
//            }
//        } catch (IOException ex) {
//            ex.printStackTrace();
//        }
//        return stream.toByteArray();
//    }
//
//    private byte[] convertExpressionToBytes(Codes.Code exp) {
//        ByteArrayOutputStream stream = new ByteArrayOutputStream();
//        try {
//            if (exp instanceof Codes.MathExpSum) {
//                stream.write(new byte[]{0x71});
//                stream.write(new byte[]{0x01});
//                stream.write(convertExpressionToBytes(((Codes.MathExpSum) exp).getValue1()));
//                stream.write(new byte[]{0x02});
//                stream.write(convertExpressionToBytes(((Codes.MathExpSum) exp).getValue2()));
//                return stream.toByteArray();
//            } else if (exp instanceof Codes.MathExpSubstract) {
//                stream.write(new byte[]{0x72});
//                stream.write(new byte[]{0x01});
//                stream.write(convertExpressionToBytes(((Codes.MathExpSubstract) exp).getValue1()));
//                stream.write(new byte[]{0x02});
//                stream.write(convertExpressionToBytes(((Codes.MathExpSubstract) exp).getValue2()));
//                return stream.toByteArray();
//            } else if (exp instanceof Codes.MathExpMultiply) {
//                stream.write(new byte[]{0x73});
//                stream.write(new byte[]{0x01});
//                stream.write(convertExpressionToBytes(((Codes.MathExpMultiply) exp).getValue1()));
//                stream.write(new byte[]{0x02});
//                stream.write(convertExpressionToBytes(((Codes.MathExpMultiply) exp).getValue2()));
//                return stream.toByteArray();
//            } else if (exp instanceof Codes.MathExpDivide) {
//                stream.write(new byte[]{0x74});
//                stream.write(new byte[]{0x01});
//                stream.write(convertExpressionToBytes(((Codes.MathExpDivide) exp).getValue1()));
//                stream.write(new byte[]{0x02});
//                stream.write(convertExpressionToBytes(((Codes.MathExpDivide) exp).getValue2()));
//                return stream.toByteArray();
//            } else if (exp instanceof Codes.MathExpMod) {
//                stream.write(new byte[]{0x75});
//                stream.write(new byte[]{0x01});
//                stream.write(convertExpressionToBytes(((Codes.MathExpMod) exp).getValue1()));
//                stream.write(new byte[]{0x02});
//                stream.write(convertExpressionToBytes(((Codes.MathExpMod) exp).getValue2()));
//                return stream.toByteArray();
//            } else if (exp instanceof Codes.MathExpPower) {
//                stream.write(new byte[]{0x76});
//                stream.write(new byte[]{0x01});
//                stream.write(convertExpressionToBytes(((Codes.MathExpPower) exp).getValue1()));
//                stream.write(new byte[]{0x02});
//                stream.write(convertExpressionToBytes(((Codes.MathExpPower) exp).getValue2()));
//                return stream.toByteArray();
//            } else if (exp instanceof Codes.MathExpAnd) {
//                stream.write(new byte[]{0x77});
//                stream.write(new byte[]{0x01});
//                stream.write(convertExpressionToBytes(((Codes.MathExpAnd) exp).getValue1()));
//                stream.write(new byte[]{0x02});
//                stream.write(convertExpressionToBytes(((Codes.MathExpAnd) exp).getValue2()));
//                return stream.toByteArray();
//            } else if (exp instanceof Codes.MathExpOr) {
//                stream.write(new byte[]{0x78});
//                stream.write(new byte[]{0x01});
//                stream.write(convertExpressionToBytes(((Codes.MathExpOr) exp).getValue1()));
//                stream.write(new byte[]{0x02});
//                stream.write(convertExpressionToBytes(((Codes.MathExpOr) exp).getValue2()));
//                return stream.toByteArray();
//            } else if (exp instanceof Codes.MathExpEqual) {
//                stream.write(new byte[]{0x79});
//                stream.write(new byte[]{0x01});
//                stream.write(convertExpressionToBytes(((Codes.MathExpEqual) exp).getValue1()));
//                stream.write(new byte[]{0x02});
//                stream.write(convertExpressionToBytes(((Codes.MathExpEqual) exp).getValue2()));
//                return stream.toByteArray();
//            } else if (exp instanceof Codes.MathExpGT) {
//                stream.write(new byte[]{0x7a});
//                stream.write(new byte[]{0x01});
//                stream.write(convertExpressionToBytes(((Codes.MathExpGT) exp).getValue1()));
//                stream.write(new byte[]{0x02});
//                stream.write(convertExpressionToBytes(((Codes.MathExpGT) exp).getValue2()));
//                return stream.toByteArray();
//            } else if (exp instanceof Codes.MathExpGE) {
//                stream.write(new byte[]{0x7b});
//                stream.write(new byte[]{0x01});
//                stream.write(convertExpressionToBytes(((Codes.MathExpGE) exp).getValue1()));
//                stream.write(new byte[]{0x02});
//                stream.write(convertExpressionToBytes(((Codes.MathExpGE) exp).getValue2()));
//                return stream.toByteArray();
//            } else if (exp instanceof Codes.MathExpNE) {
//                stream.write(new byte[]{0x7c});
//                stream.write(new byte[]{0x01});
//                stream.write(convertExpressionToBytes(((Codes.MathExpNE) exp).getValue1()));
//                stream.write(new byte[]{0x02});
//                stream.write(convertExpressionToBytes(((Codes.MathExpNE) exp).getValue2()));
//                return stream.toByteArray();
//            } else if (exp instanceof Codes.MathExpLE) {
//                stream.write(new byte[]{0x7d});
//                stream.write(new byte[]{0x01});
//                stream.write(convertExpressionToBytes(((Codes.MathExpLE) exp).getValue1()));
//                stream.write(new byte[]{0x02});
//                stream.write(convertExpressionToBytes(((Codes.MathExpLE) exp).getValue2()));
//                return stream.toByteArray();
//            } else if (exp instanceof Codes.MathExpLT) {
//                stream.write(new byte[]{0x7e});
//                stream.write(new byte[]{0x01});
//                stream.write(convertExpressionToBytes(((Codes.MathExpLT) exp).getValue1()));
//                stream.write(new byte[]{0x02});
//                stream.write(convertExpressionToBytes(((Codes.MathExpLT) exp).getValue2()));
//                return stream.toByteArray();
//            } else if (exp instanceof Codes.MathExpNot) {
//                stream.write(new byte[]{0x4f});
//                stream.write(new byte[]{0x01});
//                stream.write(convertExpressionToBytes(((Codes.MathExpNot) exp).getValue()));
//            } else if (exp instanceof Codes.Call) {
//                stream.write(new byte[]{0x55});
//                stream.write(new byte[]{0x01});
//                stream.write(convertExpressionToBytes(((Codes.Call) exp).getFuncReference()));
//                stream.write(new byte[]{0x02});
//                stream.write(convertIntegerToBytes(((Codes.Call) exp).getEntries().size()));
//                for (Map.Entry<String, Codes.Code> entry : ((Codes.Call) exp).getEntries().entrySet()) {
//                    stream.write(new byte[]{0x03});
//                    byte[] keyBytes = entry.getKey().getBytes();
//                    stream.write(convertIntegerToBytes(keyBytes.length + 1));
//                    stream.write(keyBytes);
//                    stream.write('\0');
//                    byte[] valueBytes = convertExpressionToBytes(entry.getValue());
//                    stream.write(convertIntegerToBytes(valueBytes.length));
//                    stream.write(valueBytes);
//                }
//            } else if (exp instanceof Codes.Instantiate) {
//                stream.write(new byte[]{0x57});
//                stream.write(new byte[]{0x01});
//                stream.write(convertExpressionToBytes(((Codes.Instantiate) exp).getClassReference()));
//                stream.write(new byte[]{0x02});
//                stream.write(convertIntegerToBytes(((Codes.Instantiate) exp).getEntries().size()));
//                for (Map.Entry<String, Codes.Code> entry : ((Codes.Instantiate) exp).getEntries().entrySet()) {
//                    stream.write(new byte[]{0x03});
//                    byte[] keyBytes = entry.getKey().getBytes();
//                    stream.write(convertIntegerToBytes(keyBytes.length + 1));
//                    stream.write(keyBytes);
//                    stream.write('\0');
//                    byte[] valueBytes = convertExpressionToBytes(entry.getValue());
//                    stream.write(convertIntegerToBytes(valueBytes.length));
//                    stream.write(valueBytes);
//                }
//            } else if (exp instanceof Codes.Identifier) {
//                stream.write(new byte[]{0x61});
//                byte[] idName = ((Codes.Identifier) exp).getName().getBytes();
//                stream.write(convertIntegerToBytes(idName.length + 1));
//                stream.write(idName);
//                stream.write('\0');
//                return stream.toByteArray();
//            } else if (exp instanceof Codes.Array) {
//                stream.write(new byte[]{0x4b});
//                stream.write(new byte[]{0x1});
//                stream.write(convertIntegerToBytes(((Codes.Array) exp).getItems().size()));
//                for (Codes.Code item : ((Codes.Array) exp).getItems()) {
//                    stream.write(new byte[]{0x02});
//                    stream.write(convertExpressionToBytes(item));
//                }
//                return stream.toByteArray();
//            } else if (exp instanceof Codes.Reference) {
//                stream.write(new byte[]{0x7f});
//                stream.write(new byte[]{0x01});
//                stream.write(convertExpressionToBytes(((Codes.Reference) exp).getCurrentChain()));
//                if (((Codes.Reference) exp).getRestOfChains() != null) {
//                    stream.write(new byte[]{0x02});
//                    stream.write(convertExpressionToBytes(((Codes.Reference) exp).getRestOfChains()));
//                }
//                stream.write(new byte[]{0x6d});
//                return stream.toByteArray();
//            } else if (exp instanceof Codes.Index) {
//                stream.write(new byte[]{0x6c});
//                stream.write(new byte[]{0x01});
//                stream.write(convertExpressionToBytes(((Codes.Index) exp).getVar()));
//                stream.write(new byte[]{0x02});
//                stream.write(convertIntegerToBytes(((Codes.Index) exp).getIndex().size()));
//                for (Codes.Code id : ((Codes.Index) exp).getIndex()) {
//                    stream.write(new byte[]{0x03});
//                    stream.write(convertExpressionToBytes(id));
//                }
//                if (((Codes.Index) exp).getRestOfChains() != null) {
//                    stream.write(new byte[]{0x04});
//                    stream.write(convertExpressionToBytes(((Codes.Index) exp).getRestOfChains()));
//                }
//                stream.write(new byte[]{0x6b});
//                return stream.toByteArray();
//            } else if (exp instanceof Codes.Period) {
//                stream.write(new byte[]{0x6a});
//                stream.write(new byte[]{0x01});
//                stream.write(convertExpressionToBytes(((Codes.Period) exp).getStart()));
//                stream.write(new byte[]{0x02});
//                stream.write(convertExpressionToBytes(((Codes.Period) exp).getEnd()));
//                stream.write(new byte[]{0x69});
//                return stream.toByteArray();
//            } else if (exp instanceof Codes.Value) {
//                if (((Codes.Value) exp).getValue() instanceof String) {
//                    stream.write(new byte[]{0x62});
//                    byte[] value = ((String) ((Codes.Value) exp).getValue()).getBytes();
//                    stream.write(convertIntegerToBytes(value.length + 1));
//                    stream.write(value);
//                    stream.write('\0');
//                } else if (((Codes.Value) exp).getValue() instanceof Double) {
//                    stream.write(new byte[]{0x63});
//                    stream.write(convertDoubleToBytes((Double)((Codes.Value) exp).getValue()));
//                } else if (((Codes.Value) exp).getValue() instanceof Float) {
//                    stream.write(new byte[]{0x64});
//                    stream.write(convertFloatToBytes((Float)((Codes.Value) exp).getValue()));
//                } else if (((Codes.Value) exp).getValue() instanceof Short) {
//                    stream.write(new byte[]{0x65});
//                    stream.write(convertShortToBytes((Short)((Codes.Value) exp).getValue()));
//                } else if (((Codes.Value) exp).getValue() instanceof Integer) {
//                    stream.write(new byte[]{0x66});
//                    stream.write(convertIntegerToBytes((Integer)((Codes.Value) exp).getValue()));
//                } else if (((Codes.Value) exp).getValue() instanceof Long) {
//                    stream.write(new byte[]{0x67});
//                    stream.write(convertLongToBytes((Long)((Codes.Value) exp).getValue()));
//                } else if (((Codes.Value) exp).getValue() instanceof Boolean) {
//                    stream.write(new byte[]{0x68});
//                    stream.write(convertBooleanToBytes((Boolean) ((Codes.Value) exp).getValue()));
//                }
//                return stream.toByteArray();
//            } else if (exp instanceof Codes.AnonymousObject) {
//                stream.write(new byte[]{0x4e});
//                stream.write(convertIntegerToBytes(((Codes.AnonymousObject) exp).getContent().size()));
//                for (Map.Entry<String, Codes.Code> entry : ((Codes.AnonymousObject) exp).getContent().entrySet()) {
//                    stream.write(new byte[]{0x01});
//                    byte[] keyBytes = entry.getKey().getBytes();
//                    stream.write(convertIntegerToBytes(keyBytes.length + 1));
//                    stream.write(keyBytes);
//                    stream.write('\0');
//                    stream.write(convertExpressionToBytes(entry.getValue()));
//                }
//            } else if (exp instanceof Codes.Empty) {
//                stream.write(0x4d);
//            }
//        } catch (Exception ex) {
//            ex.printStackTrace();
//        }
//        return stream.toByteArray();
//    }
//
//    private byte[] convertDoubleToBytes(double number) {
//        ByteBuffer bb = ByteBuffer.allocate(8);
//        bb.putDouble(number);
//        return bb.array();
//    }
//
//    private byte[] convertFloatToBytes(float number) {
//        ByteBuffer bb = ByteBuffer.allocate(4);
//        bb.putFloat(number);
//        return bb.array();
//    }
//
//    private byte[] convertShortToBytes(short number) {
//        ByteBuffer bb = ByteBuffer.allocate(2);
//        bb.putShort(number);
//        return bb.array();
//    }
//
//    private byte[] convertIntegerToBytes(int number) {
//        ByteBuffer bb = ByteBuffer.allocate(4);
//        bb.putInt(number);
//        return bb.array();
//    }
//
//    private byte[] convertLongToBytes(long number) {
//        ByteBuffer bb = ByteBuffer.allocate(8);
//        bb.putLong(number);
//        return bb.array();
//    }
//
//    private byte[] convertBooleanToBytes(boolean number) {
//        return new byte[]{(byte) (number?1:0)};
//    }
//
//    public TraParser(TraLexer lexer) {
//
//        this.lexer = lexer;
//
//        Node rootNode = new Node("rootNode");
//        mainNode.next(Collections.singletonList(rootNode),
//                prevResults -> {
//                    List<Codes.Code> codes = (List<Codes.Code>) prevResults.get(0).second;
//                    String uglyJSONString = JsonHelper.toJson(codes);
//                    Gson gson = new GsonBuilder().setPrettyPrinting().create();
//                    JsonParser jp = new JsonParser();
//                    JsonElement je = jp.parse(uglyJSONString);
//                    System.out.println(gson.toJson(je));
//                    return convertCodeToBytes(codes);
//                });
//        Node expNode = new Node("expNode");
//        rootNode.next(Arrays.asList(expNode, rootNode), new Action() {
//            @Override
//            public Object act(List<Pair<Symbol, Object>> prevResults) {
//                List<Codes.Code> codes = (List<Codes.Code>) prevResults.get(1).second;
//                codes.add((Codes.Code)prevResults.get(0).second);
//                return codes;
//            }
//        });
//        rootNode.next(Collections.singletonList(epsilon), new Action() {
//            @Override
//            public Object act(List<Pair<Symbol, Object>> prevResults) {
//                return new ArrayList<Codes.Code>();
//            }
//        });
//
//        expNode.next(Collections.singletonList(sym.terminalNames[sym.NUMBER]),
//                prevResults -> prevResults.get(0).second);
//        expNode.next(Collections.singletonList(sym.terminalNames[sym.STRING]),
//                prevResults -> prevResults.get(0).second);
//        expNode.next(Collections.singletonList(sym.terminalNames[sym.EMPTY]),
//                prevResults -> new Codes.Empty());
//        expNode.next(Collections.singletonList(sym.terminalNames[sym.TRUE]),
//                prevResults -> {
//                    Codes.Value value = new Codes.Value();
//                    value.setValue(true);
//                    return value;
//                });
//        expNode.next(Collections.singletonList(sym.terminalNames[sym.FALSE]),
//                prevResults -> {
//                    Codes.Value value = new Codes.Value();
//                    value.setValue(false);
//                    return value;
//                });
//
//        expNode.next(Arrays.asList(sym.terminalNames[sym.LPAREN], expNode, sym.terminalNames[sym.RPAREN]),
//                prevResults -> prevResults.get(0).second);
//        Node inputsExtraNode = new Node("inputsExtraNode");
//        Node inputsNode = new Node("inputsNode");
//        inputsNode.next(Arrays.asList(sym.terminalNames[sym.IDENTIFIER], sym.terminalNames[sym.COLON],
//                expNode, inputsExtraNode),
//                prevResults -> {
//                    Hashtable<String, Codes.Code> ids = (Hashtable<String, Codes.Code>) prevResults.get(2).second;
//                    Codes.Code exp = (Codes.Code) prevResults.get(1).second;
//                    String inputName = ((Codes.Identifier) prevResults.get(0).second).getName();
//                    ids.put(inputName, exp);
//                    return ids;
//                });
//        inputsNode.next(Collections.singletonList(epsilon),
//                prevResults -> new Hashtable<>());
//        inputsExtraNode.next(Arrays.asList(sym.terminalNames[sym.COMMA], sym.terminalNames[sym.IDENTIFIER],
//                sym.terminalNames[sym.COLON], expNode, inputsExtraNode),
//                prevResults -> {
//                    Hashtable<String, Codes.Code> ids = (Hashtable<String, Codes.Code>) prevResults.get(2).second;
//                    Codes.Code exp = (Codes.Code) prevResults.get(1).second;
//                    String inputName = ((Codes.Identifier) prevResults.get(0).second).getName();
//                    ids.put(inputName, exp);
//                    return ids;
//                });
//        inputsExtraNode.next(Collections.singletonList(epsilon),
//                prevResults -> new Hashtable<>());
//        Node nameNode = new Node("nameNode");
//        nameNode.next(Collections.singletonList(sym.terminalNames[sym.IDENTIFIER]),
//                prevResults -> {
//                    Codes.Reference ref = new Codes.Reference();
//                    ref.setCurrentChain((Codes.Code)prevResults.get(0).second);
//                    ref.setRestOfChains(null);
//                    return ref;
//                });
//        nameNode.next(Arrays.asList(nameNode, sym.terminalNames[sym.DOT], sym.terminalNames[sym.IDENTIFIER]),
//                prevResults -> {
//                    Codes.Reference ref = new Codes.Reference();
//                    ref.setCurrentChain((Codes.Code)prevResults.get(0).second);
//                    ref.setRestOfChains((Codes.Code)prevResults.get(1).second);
//                    return ref;
//                });
//        Node chains = new Node("chains");
//        Node extra = new Node("extra");
//        Node temp = new Node("temp");
//        Node expChainNode = new Node("expChainNode");
//        expNode.next(Collections.singletonList(chains),
//                prevResults -> prevResults.get(0).second);
////        chains.next(Arrays.asList(nameNode, extra),
////                new Action() {
////                    @Override
////                    public Object act(List<Pair<Symbol, Object>> prevResults) {
////                        if (prevResults.get(1).second instanceof Codes.Reference) {
////                            Codes.Reference ref = (Codes.Reference) prevResults.get(1).second;
////                            ref.setCurrentChain((Codes.Code)prevResults.get(0).second);
////                            return ref;
////                        }
////                        else if (prevResults.get(1).second instanceof Codes.Index) {
////                            Codes.Index index = (Codes.Index) prevResults.get(1).second;
////                            index.setVar((Codes.Code)prevResults.get(0).second);
////                            return index;
////                        }
////                        else if (prevResults.get(1).second instanceof Codes.Call) {
////                            Codes.Call call = (Codes.Call) prevResults.get(1).second;
////                            call.setFuncReference((Codes.Code)prevResults.get(0).second);
////                            Codes.Reference ref = new Codes.Reference();
////                            ref.setCurrentChain(call);
////                            ref.setRestOfChains(null);
////                            return ref;
////                        }
////                        return null;
////                    }
////                });
//        chains.next(Collections.singletonList(nameNode),
//                new Action() {
//                    @Override
//                    public Object act(List<Pair<Symbol, Object>> prevResults) {
//                        return prevResults.get(0).second;
//                    }
//                });
//        chains.next(Arrays.asList(chains, extra),
//                prevResults -> {
//                    if (prevResults.get(1).second instanceof Codes.Reference) {
//                        Codes.Reference ref = (Codes.Reference) prevResults.get(1).second;
//                        ref.setCurrentChain((Codes.Code)prevResults.get(0).second);
//                        return ref;
//                    }
//                    else if (prevResults.get(1).second instanceof Codes.Index) {
//                        Codes.Index index = (Codes.Index) prevResults.get(1).second;
//                        index.setVar((Codes.Code)prevResults.get(0).second);
//                        return index;
//                    }
//                    else if (prevResults.get(1).second instanceof Codes.Call) {
//                        Codes.Call call = (Codes.Call) prevResults.get(1).second;
//                        call.setFuncReference((Codes.Code)prevResults.get(0).second);
//                        Codes.Reference ref = new Codes.Reference();
//                        ref.setCurrentChain(call);
//                        ref.setRestOfChains(null);
//                        return ref;
//                    }
//                    return null;
//                });
//        chains.next(Arrays.asList(chains, sym.terminalNames[sym.DOT], chains),
//                prevResults -> {
//                    Codes.Reference ref = new Codes.Reference();
//                    ref.setCurrentChain((Codes.Code)prevResults.get(0).second);
//                    ref.setRestOfChains((Codes.Code)prevResults.get(1).second);
//                    return ref;
//                });
//        extra.next(Arrays.asList(sym.terminalNames[sym.LPAREN], inputsNode, sym.terminalNames[sym.RPAREN]),
//                prevResults -> {
//                    Codes.Call call = new Codes.Call();
//                    call.setFuncReference(null);
//                    call.setEntries((Hashtable<String, Codes.Code>) prevResults.get(0).second);
//                    return call;
//                });
//        extra.next(Arrays.asList(sym.terminalNames[sym.LBRACKET], expChainNode, sym.terminalNames[sym.RBRACKET]),
//                prevResults -> {
//                    Codes.Index index = new Codes.Index();
//                    index.setVar(null);
//                    index.setIndex((List<Codes.Code>)prevResults.get(0).second);
//                    index.setRestOfChains(null);
//                    return index;
//                });
//        Node periodNode = new Node("periodNode");
//        Node expChainExtraNode = new Node("expChainExtraNode");
//        expChainExtraNode.next(Arrays.asList(sym.terminalNames[sym.COMMA], expNode, expChainExtraNode),
//                prevResults -> {
//                    List<Codes.Code> ids = (List<Codes.Code>) prevResults.get(1).second;
//                    Codes.Code exp = (Codes.Code) prevResults.get(0).second;
//                    ids.add(0, exp);
//                    return ids;
//                });
//        expChainExtraNode.next(Collections.singletonList(epsilon),
//                prevResults -> new ArrayList<>());
//        expChainNode.next(Arrays.asList(expNode, periodNode, expChainExtraNode),
//                prevResults -> {
//                    List<Codes.Code> ids = (List<Codes.Code>) prevResults.get(2).second;
//                    if (prevResults.get(1).second != null) {
//                        Codes.Period period = new Codes.Period();
//                        period.setStart((Codes.Code) prevResults.get(0).second);
//                        period.setEnd((Codes.Code)prevResults.get(1).second);
//                        ids.add(0, period);
//                    } else {
//                        Codes.Code exp = (Codes.Code) prevResults.get(0).second;
//                        ids.add(0, exp);
//                    }
//                    return ids;
//                });
//        periodNode.next(Arrays.asList(sym.terminalNames[sym.COLON], expNode),
//                prevResults -> prevResults.get(0).second);
//        periodNode.next(Collections.singletonList(epsilon),
//                prevResults -> null);
//        expChainNode.next(Arrays.asList(expNode, periodNode, expChainExtraNode),
//                prevResults -> {
//                    List<Codes.Code> ids = (List<Codes.Code>) prevResults.get(2).second;
//                    if (prevResults.get(1).second != null) {
//                        Codes.Period period = new Codes.Period();
//                        period.setStart((Codes.Code) prevResults.get(0).second);
//                        period.setEnd((Codes.Code)prevResults.get(1).second);
//                        ids.add(0, period);
//                    } else {
//                        Codes.Code exp = (Codes.Code) prevResults.get(0).second;
//                        ids.add(0, exp);
//                    }
//                    return ids;
//                });
//        expChainNode.next(Collections.singletonList(epsilon),
//                prevResults -> new ArrayList<>());
//        expNode.next(Arrays.asList(expNode, sym.terminalNames[sym.IS], sym.terminalNames[sym.NOT],
//                sym.terminalNames[sym.SATISFIED]),
//                prevResults -> {
//                    Codes.MathExpNot sum = new Codes.MathExpNot();
//                    sum.setValue((Codes.Code)prevResults.get(0).second);
//                    return sum;
//                });
//        expNode.next(Arrays.asList(expNode, sym.terminalNames[sym.IS], sym.terminalNames[sym.NOT],
//                sym.terminalNames[sym.SATISFIED]),
//                prevResults -> {
//                    Codes.MathExpNot sum = new Codes.MathExpNot();
//                    sum.setValue((Codes.Code)prevResults.get(0).second);
//                    return sum;
//                });
//        expNode.next(Arrays.asList(expNode, sym.terminalNames[sym.SUM], expNode),
//                prevResults -> {
//                    Codes.MathExpSum sum = new Codes.MathExpSum();
//                    sum.setValue1((Codes.Code)prevResults.get(0).second);
//                    sum.setValue2((Codes.Code)prevResults.get(1).second);
//                    return sum;
//                });
//        expNode.next(Arrays.asList(expNode, sym.terminalNames[sym.SUBTRACT], expNode),
//                prevResults -> {
//                    Codes.MathExpSubstract sum = new Codes.MathExpSubstract();
//                    sum.setValue1((Codes.Code)prevResults.get(0).second);
//                    sum.setValue2((Codes.Code)prevResults.get(1).second);
//                    return sum;
//                });
//        expNode.next(Arrays.asList(expNode, sym.terminalNames[sym.MULTIPLY], expNode),
//                prevResults -> {
//                    Codes.MathExpMultiply sum = new Codes.MathExpMultiply();
//                    sum.setValue1((Codes.Code)prevResults.get(0).second);
//                    sum.setValue2((Codes.Code)prevResults.get(1).second);
//                    return sum;
//                });
//        expNode.next(Arrays.asList(expNode, sym.terminalNames[sym.DIVISION], expNode),
//                prevResults -> {
//                    Codes.MathExpDivide sum = new Codes.MathExpDivide();
//                    sum.setValue1((Codes.Code)prevResults.get(0).second);
//                    sum.setValue2((Codes.Code)prevResults.get(1).second);
//                    return sum;
//                });
//        expNode.next(Arrays.asList(expNode, sym.terminalNames[sym.AND], expNode),
//                prevResults -> {
//                    Codes.MathExpAnd sum = new Codes.MathExpAnd();
//                    sum.setValue1((Codes.Code)prevResults.get(0).second);
//                    sum.setValue2((Codes.Code)prevResults.get(1).second);
//                    return sum;
//                });
//        expNode.next(Arrays.asList(expNode, sym.terminalNames[sym.OR], expNode),
//                prevResults -> {
//                    Codes.MathExpOr sum = new Codes.MathExpOr();
//                    sum.setValue1((Codes.Code)prevResults.get(0).second);
//                    sum.setValue2((Codes.Code)prevResults.get(1).second);
//                    return sum;
//                });
//        expNode.next(Arrays.asList(expNode, sym.terminalNames[sym.POWER], expNode),
//                prevResults -> {
//                    Codes.MathExpPower sum = new Codes.MathExpPower();
//                    sum.setValue1((Codes.Code)prevResults.get(0).second);
//                    sum.setValue2((Codes.Code)prevResults.get(1).second);
//                    return sum;
//                });
//        expNode.next(Arrays.asList(expNode, sym.terminalNames[sym.MOD], expNode),
//                prevResults -> {
//                    Codes.MathExpMod sum = new Codes.MathExpMod();
//                    sum.setValue1((Codes.Code)prevResults.get(0).second);
//                    sum.setValue2((Codes.Code)prevResults.get(1).second);
//                    return sum;
//                });
//        expNode.next(Arrays.asList(expNode, sym.terminalNames[sym.EQUAL], expNode),
//                prevResults -> {
//                    Codes.MathExpEqual sum = new Codes.MathExpEqual();
//                    sum.setValue1((Codes.Code)prevResults.get(0).second);
//                    sum.setValue2((Codes.Code)prevResults.get(1).second);
//                    return sum;
//                });
//        expNode.next(Arrays.asList(expNode, sym.terminalNames[sym.LT], expNode),
//                prevResults -> {
//                    Codes.MathExpLT sum = new Codes.MathExpLT();
//                    sum.setValue1((Codes.Code)prevResults.get(0).second);
//                    sum.setValue2((Codes.Code)prevResults.get(1).second);
//                    return sum;
//                });
//        expNode.next(Arrays.asList(expNode, sym.terminalNames[sym.LE], expNode),
//                prevResults -> {
//                    Codes.MathExpLE sum = new Codes.MathExpLE();
//                    sum.setValue1((Codes.Code)prevResults.get(0).second);
//                    sum.setValue2((Codes.Code)prevResults.get(1).second);
//                    return sum;
//                });
//        expNode.next(Arrays.asList(expNode, sym.terminalNames[sym.GE], expNode),
//                prevResults -> {
//                    Codes.MathExpGE sum = new Codes.MathExpGE();
//                    sum.setValue1((Codes.Code)prevResults.get(0).second);
//                    sum.setValue2((Codes.Code)prevResults.get(1).second);
//                    return sum;
//                });
//        expNode.next(Arrays.asList(expNode, sym.terminalNames[sym.GT], expNode),
//                prevResults -> {
//                    Codes.MathExpGT sum = new Codes.MathExpGT();
//                    sum.setValue1((Codes.Code)prevResults.get(0).second);
//                    sum.setValue2((Codes.Code)prevResults.get(1).second);
//                    return sum;
//                });
//        expNode.next(Arrays.asList(expNode, sym.terminalNames[sym.NE], expNode),
//                prevResults -> {
//                    Codes.MathExpNE sum = new Codes.MathExpNE();
//                    sum.setValue1((Codes.Code)prevResults.get(0).second);
//                    sum.setValue2((Codes.Code)prevResults.get(1).second);
//                    return sum;
//                });
//    }
//
//    public Object parse() {
//        dependencies = new ArrayList<>();
//        List<Symbol> tokens = new ArrayList<>();
//        Symbol token;
//        try {
//            while ((token = this.lexer.next_token()).value != null) tokens.add(token);
//            tokens.add(new Symbol(sym.EOF, 0, 0, null));
//        } catch (IOException ex) {
//            ex.printStackTrace();
//        }
//        return new Pair<List<String>, Object>(dependencies, this.theThirdParse(tokens.toArray(new Symbol[0])));
//    }
//
//    private boolean investigateLoop(HashSet<String> blacklist, String firstToken, Node node) {
//        for (Pair<List<Object>, Action> subNodesAction : node.subNodes) {
//            Object obj = subNodesAction.first.get(0);
//            if (obj instanceof Node) {
//                if (blacklist.contains(((Node) obj).name)) continue;
//                else blacklist.add(((Node) obj).name);
//                if (investigateLoop(blacklist, firstToken, (Node) obj)) {
//                    return true;
//                }
//            } else if (obj instanceof String) {
//                if (firstToken.equals(obj)) {
//                    return true;
//                }
//            }
//        }
//        return false;
//    }
//
//    private void printStack(NodePointer currentPointer) {
//        NodePointer finalPointer = currentPointer;
//        int rule;
//        while (finalPointer != null) {
//            System.out.println(finalPointer.inputs.size() + " " + finalPointer.id + " " + finalPointer.nodeName() +
//                    " - " + finalPointer.rulePointer + " - " + finalPointer.foundMatch);
//            rule = finalPointer.backedByPos;
//            finalPointer = finalPointer.backedBy;
//            if (finalPointer != null)
//                finalPointer.rulePointer = rule;
//        }
//    }
//
//    public Object theThirdParse(Symbol[] tokens) {
//        NodePointer currentPointer = new NodePointer(UUID.randomUUID().toString(), mainNode, 0, 0,
//                0, 0, null, null);
//        boolean done = false;
//        while (true) {
//            System.out.println("inputs dictionary size : " + currentPointer.inputs.size() + " - " + currentPointer.nodeName() + " - " + currentPointer.rulePointer + " - " + currentPointer.ruleTokenPointer + " - " + currentPointer.tokenPointer);
//            //printStack(currentPointer);
//            if (done) {
//                HashSet<String> seen = new HashSet<>();
//                while (true) {
//                    currentPointer.forwardToken();
//                    if (currentPointer.ruleTokenPointer < currentPointer.currentRuleSize() &&
//                            currentPointer.currentRuleToken() instanceof Node) {
//                        System.out.println(((Node) currentPointer.currentRuleToken()).name + "moving on epsilon...");
//                        for (int i = 0; i < ((Node) currentPointer.currentRuleToken()).subNodes.size(); i++) {
//                            if (((Node) currentPointer.currentRuleToken()).subNodes.get(i).first.get(0) instanceof Node &&
//                                    ((Node) ((Node) currentPointer.currentRuleToken()).subNodes.get(i).first.get(0)).name.equals("epsilon")) {
//                                System.out.println("resolved epsilon.");
//                                Object result = ((Node) currentPointer.currentRuleToken()).subNodes.get(i).second.act(new ArrayList<>());
//                                currentPointer.inputs.add(new Pair<>(null, result));
//                                currentPointer.forwardToken();
//                                break;
//                            }
//                        }
//                    }
//                    Object result = currentPointer.currentAction().act(currentPointer.inputs);
//                    NodePointer temp = currentPointer;
//                    seen.add(currentPointer.id);
//                    //printStack(currentPointer);
//                    currentPointer = currentPointer.backedBy;
//                    while (currentPointer != null && (seen.contains(currentPointer.id) || currentPointer.foundMatch)) {
//                        temp = currentPointer;
//                        seen.add(currentPointer.id);
//                        currentPointer = currentPointer.backedBy;
//                        currentPointer.rulePointer = temp.backedByPos;
//                    }
//                    if (currentPointer == null) {
//                        temp.inputs = Collections.singletonList(new Pair<>(null, result));
//                        return temp.currentAction().act(temp.inputs);
//                    } else if (currentPointer.backedBy == null) {
//                        currentPointer.inputs.add(new Pair<>(null, result));
//                        return currentPointer.currentAction().act(currentPointer.inputs);
//                    } else {
//                        currentPointer.inputs.add(new Pair<>(null, result));
//                    }
//                }
//            }
//            Object ruleToken = currentPointer.currentRuleToken();
//            if (ruleToken instanceof Node) {
//                if (((Node) ruleToken).name.equals("epsilon")) {
//                    System.out.println("removing epsilon...");
//                    NodePointer temp = currentPointer.backedBy;
//                    HashSet<String> seen = new HashSet<>();
//                    seen.add(temp.id);
//                    temp = temp.backedBy;
//                    NodePointer saved;
//                    while (seen.contains(temp.id) || temp.reachedCurrentRuleEnd()) {
//                        saved = temp;
//                        temp = temp.backedBy;
//                        temp.inputs.add(new Pair<>(null, saved.currentAction().act(saved.inputs)));
//                    }
//                    NodePointer next;
//                    if (temp.currentRuleToken() instanceof Node) {
//                        next = new NodePointer(temp.id, temp.node,
//                                temp.tokenPointer, temp.rulePointer,
//                                temp.ruleTokenPointer + 1,
//                                temp.startPoint, temp,
//                                temp);
//                    }
//                    else {
//                        next = new NodePointer(temp.id, temp.node,
//                                temp.tokenPointer, temp.rulePointer,
//                                temp.ruleTokenPointer, temp.startPoint, temp, temp);
//                    }
//                    next.inputs = new ArrayList<>(temp.inputs);
//                    currentPointer = next;
//                }
//                else {
//                    HashSet<String> blacklist = new HashSet<>();
//                    //blacklist.add(currentPointer.nodeName());
//                    if (investigateLoop(blacklist, sym.terminalNames[tokens[currentPointer.tokenPointer].sym], (Node) ruleToken)) {
//                        System.out.println("opening node " + ((Node) ruleToken).name);
//                        NodePointer current = currentPointer;
//                        currentPointer = new NodePointer(UUID.randomUUID().toString(), (Node)ruleToken,
//                                current.tokenPointer, 0, 0, current.tokenPointer,
//                                current, current);
//                    } else {
//                        boolean foundEps = false;
//                        for (Pair<List<Object>, Action> subNodesAction : ((Node)ruleToken).subNodes) {
//                            Object obj = subNodesAction.first.get(0);
//                            if (obj instanceof Node && ((Node) obj).name.equals("epsilon")) {
//                                foundEps = true;
//                                break;
//                            }
//                        }
//                        if (!foundEps) {
//                            System.out.println("no legal way. getting back ....");
//                            String id = currentPointer.id;
//                            System.out.println(id);
//                            NodePointer pointer = currentPointer;
//                            while (pointer.id.equals(id)) {
//                                pointer = pointer.prev;
//                            }
//                            pointer.ruleTokenPointer = 0;
//                            pointer.rulePointer++;
//                            while (true) {
//                                if (pointer.reachedAllRulesEnd()) {
//                                    System.out.println("popping due to node rules storage end...");
//                                    pointer = pointer.prev;
//                                    if (pointer.reachedCurrentRuleEnd()) {
//                                        System.out.println("going to next rule in node...");
//                                        pointer.forwardRule();
//                                    }
//                                } else {
//                                    break;
//                                }
//                            }
//                            currentPointer = pointer;
//                        }
//                        else {
//                            System.out.println("opening node targeting epsilon... " + ((Node) ruleToken).name);
//                            for (int i = 0; i < ((Node) ruleToken).subNodes.size(); i++) {
//                                if (((Node) ruleToken).subNodes.get(i).first.get(0) instanceof Node &&
//                                        ((Node) ((Node) ruleToken).subNodes.get(i).first.get(0)).name.equals("epsilon")) {
//                                    System.out.println("resolved epsilon node action.");
//                                    currentPointer.inputs.add(new Pair<>(null, ((Node) ruleToken).subNodes.get(i).second.act(new ArrayList<>())));
//                                    break;
//                                }
//                            }
//                            NodePointer pointer = currentPointer;
//                            NodePointer temp;
//                            HashSet<String> seen = new HashSet<>();
//                            Object res = null;
//                            boolean found = false;
//                            int tokenPointer = pointer.tokenPointer;
//                            while (true) {
//                                //printStack(pointer);
//                                System.out.println("executing finding match after epsilon resolving... , " + sym.terminalNames[tokens[tokenPointer].sym]);
//                                int ruleIndex = 0;
//                                for (Pair<List<Object>, Action> subNodes : pointer.node.subNodes) {
//                                    if (subNodes.first.get(0) instanceof Node &&
//                                            ((Node) subNodes.first.get(0)).name.equals(pointer.nodeName()) &&
//                                            subNodes.first.get(1) instanceof String &&
//                                            subNodes.first.get(1).equals(sym.terminalNames[tokens[tokenPointer].sym])) {
//                                        System.out.println("found match for expansion.");
//                                        found = true;
//                                        pointer.forwardToken();
//                                        NodePointer pointerP = new NodePointer(pointer.id,
//                                                pointer.node, tokenPointer,
//                                                ruleIndex, 1, pointer.tokenPointer +
//                                                (tokens[pointer.tokenPointer + 1].sym == sym.EOF ? 0 : 1),
//                                                pointer, pointer);
//                                        if (res != null) {
//                                            pointer.inputs.add(new Pair<>(null, res));
//                                            res = null;
//                                        }
//                                        pointerP.inputs.add(new Pair<>(null, pointer.currentAction().act(pointer.inputs)));
//                                        pointer.foundMatch = true;
//                                        pointer = pointerP;
//                                        currentPointer = pointer;
//                                        if (tokens[pointer.tokenPointer].sym == sym.EOF) {
//                                            System.out.println("finished compile.");
//                                            done = true;
//                                            continue;
//                                        }
//                                        break;
//                                    }
//                                    ruleIndex++;
//                                }
//                                //printStack(pointer);
//                                if (found) break;
//                                if (seen.contains(pointer.id)) {
//                                    System.out.println("skipping...");
//                                    temp = pointer;
//                                    pointer = pointer.backedBy;
//                                    pointer.rulePointer = temp.backedByPos;
//                                    if (pointer.id.equals(temp.id))
//                                        pointer.inputs = new ArrayList<>(temp.inputs);
//                                    else {
//                                        pointer.inputs.add(new Pair<>(null, res));
//                                        res = null;
//                                    }
//                                }
//                                else if (pointer.reachedCurrentRuleEnd()) {
//                                    System.out.println("resolving...");
//                                    seen.add(pointer.id);
//                                    temp = pointer;
//                                    if (res != null)
//                                        pointer.inputs.add(new Pair<>(null, res));
//                                    res = pointer.currentAction().act(pointer.inputs);
//                                    pointer = pointer.backedBy;
//                                    pointer.rulePointer = temp.backedByPos;
//                                }
//                                else {
//                                    if (res != null)
//                                        pointer.inputs.add(new Pair<>(null, res));
//                                    break;
//                                }
//                            }
//                            if (!found) {
//                                NodePointer next;
//                                if (pointer.currentRuleToken() instanceof Node) {
//                                    next = new NodePointer(pointer.id, pointer.node,
//                                            currentPointer.tokenPointer, pointer.rulePointer,
//                                            pointer.ruleTokenPointer + 1,
//                                            pointer.startPoint, pointer,
//                                            currentPointer);
//                                } else {
//                                    next = new NodePointer(pointer.id, pointer.node,
//                                            currentPointer.tokenPointer, pointer.rulePointer,
//                                            pointer.ruleTokenPointer, pointer.startPoint, pointer, currentPointer);
//                                }
//                                next.inputs = new ArrayList<>(pointer.inputs);
//                                currentPointer = next;
//                            }
//                        }
//                    }
//                }
//            }
//            else if (ruleToken instanceof String) {
//                Symbol token = tokens[currentPointer.tokenPointer];
//                System.out.println(sym.terminalNames[token.sym] + " " + " line : " + token.left + " , word : " + token.right);
//                System.out.println("comparing " + sym.terminalNames[token.sym] + " with " + ruleToken);
//                if (token.sym == sym.EOF) {
//                    System.out.println("finished compiling.");
//                    done = true;
//                    continue;
//                }
//                if (ruleToken.equals(sym.terminalNames[token.sym])) {
//                    NodePointer tempPointer = currentPointer;
//                    boolean matched = false;
//                    if (currentPointer.readyToMatchRule()) {
//                        System.out.println("matched rule. trying to opening way...");
//                        System.out.println("executing found match...");
//                        findingUpperMatch:
//                        while (currentPointer.ruleTokenPointer == 0) {
//                            int ruleIndex = 0;
//                            boolean found = false;
//                            if (currentPointer.ruleTokenPointer <
//                                    currentPointer.node.subNodes.get(currentPointer.rulePointer).first.size()) {
//                                for (Pair<List<Object>, Action> subNodes : currentPointer.node.subNodes) {
//                                    if (subNodes.first.get(0) instanceof Node &&
//                                            ((Node) subNodes.first.get(0)).name.equals(currentPointer.nodeName()) &&
//                                            subNodes.first.get(1) instanceof String &&
//                                            subNodes.first.get(1).equals(sym.terminalNames[tokens[currentPointer.tokenPointer + 1].sym])) {
//                                        found = true;
//                                        matched = true;
//                                        if (currentPointer.currentRuleToken() instanceof String) {
//                                            System.out.println("found value.");
//                                            if (token.sym == sym.NUMBER) {
//                                                System.out.println("found number.");
//                                                Codes.Value value = new Codes.Value();
//                                                value.setValue(token.value);
//                                                currentPointer.inputs.add(new Pair<>(null, value));
//                                            } else if (token.sym == sym.STRING) {
//                                                System.out.println("found string.");
//                                                Codes.Value value = new Codes.Value();
//                                                value.setValue(token.value);
//                                                currentPointer.inputs.add(new Pair<>(null, value));
//                                            } else if (token.sym == sym.IDENTIFIER) {
//                                                System.out.println("found id.");
//                                                Codes.Identifier identifier = new Codes.Identifier();
//                                                identifier.setName((String) token.value);
//                                                currentPointer.inputs.add(new Pair<>(null, identifier));
//                                            }
//                                        }
//                                        currentPointer.forwardToken();
//                                        NodePointer tempP = new NodePointer(currentPointer.id,
//                                                currentPointer.node, currentPointer.tokenPointer + 1,
//                                                ruleIndex, 1, currentPointer.tokenPointer +
//                                                (tokens[currentPointer.tokenPointer + 1].sym == sym.EOF ? 0 : 1),
//                                                currentPointer, currentPointer);
//                                        tempP.inputs.add(new Pair<>(null, currentPointer.currentAction().act(currentPointer.inputs)));
//                                        currentPointer.foundMatch = true;
//                                        currentPointer = tempP;
//                                        if (tokens[currentPointer.tokenPointer].sym == sym.EOF) {
//                                            System.out.println("finished compile.");
//                                            done = true;
//                                            continue;
//                                        }
//                                        break findingUpperMatch;
//                                    }
//                                    ruleIndex++;
//                                }
//                            }
//                            if (!found) {
//                                currentPointer = currentPointer.backedBy;
//                            }
//                        }
//
//                        if (!matched) {
//                            int ruleIndex = 0;
//                            currentPointer = tempPointer;
//                            System.out.println("finding match failed.");
//                            System.out.println(currentPointer.prev.nodeName());
//                            if (currentPointer.currentRuleToken() instanceof String) {
//                                System.out.println("found value.");
//                                if (token.sym == sym.NUMBER) {
//                                    System.out.println("found number.");
//                                    Codes.Value value = new Codes.Value();
//                                    value.setValue(token.value);
//                                    currentPointer.inputs.add(new Pair<>(null, value));
//                                } else if (token.sym == sym.STRING) {
//                                    System.out.println("found string.");
//                                    Codes.Value value = new Codes.Value();
//                                    value.setValue(token.value);
//                                    currentPointer.inputs.add(new Pair<>(null, value));
//                                } else if (token.sym == sym.IDENTIFIER) {
//                                    System.out.println("found id.");
//                                    Codes.Identifier identifier = new Codes.Identifier();
//                                    identifier.setName((String) token.value);
//                                    currentPointer.inputs.add(new Pair<>(null, identifier));
//                                }
//                            }
//                            currentPointer.forwardToken();
//                            NodePointer pointer = currentPointer;
//                            HashSet<String> seen = new HashSet<>();
//                            NodePointer temp;
//                            Object res = null;
//                            matched = false;
//                            int tokenPointer = pointer.tokenPointer + 1;
//                            while (true) {
//                                System.out.println("executing finding match after epsilon resolving... , " + pointer.nodeName() + " " + sym.terminalNames[tokens[tokenPointer].sym]);
//                                ruleIndex = 0;
//                                for (Pair<List<Object>, Action> subNodes : pointer.node.subNodes) {
//                                    if (subNodes.first.get(0) instanceof Node &&
//                                            ((Node) subNodes.first.get(0)).name.equals(pointer.nodeName()) &&
//                                            subNodes.first.get(1) instanceof String &&
//                                            subNodes.first.get(1).equals(sym.terminalNames[tokens[tokenPointer].sym])) {
//                                        System.out.println("found match for expansion.");
//                                        matched = true;
//                                        pointer.forwardToken();
//                                        NodePointer pointerP = new NodePointer(pointer.id,
//                                                pointer.node, tokenPointer,
//                                                ruleIndex, 1, pointer.tokenPointer +
//                                                (tokens[pointer.tokenPointer + 1].sym == sym.EOF ? 0 : 1),
//                                                pointer, pointer);
//                                        if (res != null) {
//                                            pointer.inputs.add(new Pair<>(null, res));
//                                            res = null;
//                                        }
//                                        pointerP.inputs.add(new Pair<>(null, pointer.currentAction().act(pointer.inputs)));
//                                        pointer.foundMatch = true;
//                                        pointer = pointerP;
//                                        currentPointer = pointer;
//                                        if (tokens[pointer.tokenPointer].sym == sym.EOF) {
//                                            System.out.println("finished compile.");
//                                            done = true;
//                                            continue;
//                                        }
//                                        break;
//                                    }
//                                    ruleIndex++;
//                                }
//                                if (matched) break;
//                                if (seen.contains(pointer.id) || pointer.foundMatch) {
//                                    System.out.println("skipping...");
//                                    seen.add(pointer.id);
//                                    temp = pointer;
//                                    pointer = pointer.backedBy;
//                                    System.out.println(pointer.nodeName() + " " + pointer.rulePointer);
//                                    pointer.rulePointer = temp.backedByPos;
//                                    if (pointer.id.equals(temp.id) || pointer.foundMatch)
//                                        pointer.inputs = new ArrayList<>(temp.inputs);
//                                    else {
//                                        pointer.inputs.add(new Pair<>(null, res));
//                                        res = null;
//                                    }
//                                } else if (pointer.reachedCurrentRuleEnd() || pointer.foundMatch) {
//                                    System.out.println("resolving...");
//                                    seen.add(pointer.id);
//                                    temp = pointer;
//                                    if (res != null)
//                                        pointer.inputs.add(new Pair<>(null, res));
//                                    res = pointer.currentAction().act(pointer.inputs);
//                                    pointer = pointer.backedBy;
//                                    pointer.rulePointer = temp.backedByPos;
//                                } else {
//                                    if (res != null) {
//                                        pointer.inputs.add(new Pair<>(null, res));
//                                    }
//                                    break;
//                                }
//                            }
//                            if (!matched) {
//                                NodePointer tempP = new NodePointer(pointer.id,
//                                        pointer.node, currentPointer.tokenPointer + 1,
//                                        pointer.rulePointer, pointer.ruleTokenPointer +
//                                        (tokens[currentPointer.tokenPointer + 1].sym == sym.EOF ? 0 : 1),
//                                        pointer.startPoint, pointer, pointer);
//                                tempP.inputs = new ArrayList<>(pointer.inputs);
//                                currentPointer = tempP;
//                                if (tokens[currentPointer.tokenPointer].sym == sym.EOF) {
//                                    System.out.println("finished compile.");
//                                    done = true;
//                                }
//                            }
//                        }
//                    }
//                    else {
//                        System.out.println("matched token. advancing...");
//                        Object result = null;
//                        if (currentPointer.currentRuleToken() instanceof String) {
//                            System.out.println("found value.");
//                            if (token.sym == sym.NUMBER) {
//                                System.out.println("found number.");
//                                Codes.Value value = new Codes.Value();
//                                value.setValue(token.value);
//                                result = value;
//                            } else if (token.sym == sym.STRING) {
//                                System.out.println("found string.");
//                                Codes.Value value = new Codes.Value();
//                                value.setValue(token.value);
//                                result = value;
//                            } else if (token.sym == sym.IDENTIFIER) {
//                                System.out.println("found id.");
//                                Codes.Identifier identifier = new Codes.Identifier();
//                                identifier.setName((String) token.value);
//                                result = identifier;
//                            }
//                        } else {
//                            result = currentPointer.currentAction().act(currentPointer.inputs);
//                        }
//                        currentPointer = new NodePointer(currentPointer.id, currentPointer.node,
//                                currentPointer.tokenPointer + 1, currentPointer.rulePointer,
//                                currentPointer.ruleTokenPointer +
//                                        (tokens[currentPointer.tokenPointer + 1].sym == sym.EOF ? 0 : 1),
//                                currentPointer.startPoint, currentPointer.tokenPointer == 0 ? currentPointer :
//                                currentPointer.backedBy, currentPointer);
//                        currentPointer.inputs = new ArrayList<>(currentPointer.prev.inputs);
//                        if (result != null) {
//                            Pair<Symbol, Object> r = new Pair<>(null, result);
//                            currentPointer.inputs.add(r);
//                        }
//                        if (tokens[currentPointer.tokenPointer].sym == sym.EOF) {
//                            System.out.println("finished compile.");
//                            done = true;
//                        }
//                    }
//                }
//                else {
//                    System.out.println("NOT matched rule. getting back...");
//                    NodePointer pointer = currentPointer;
//                    HashSet<String> seen = new HashSet<>();
//                    while (pointer.prevRuleToken() != null &&
//                            pointer.prevRuleToken() instanceof Node) {
//                        System.out.println("current node is not stable. going deeper...");
//                        String id = pointer.id;
//                        while (pointer.id.equals(id)) {
//                            pointer = pointer.prev;
//                        }
//                    }
//                    System.out.println("reached " + pointer.node.name);
//                    boolean movedToNextRule = false;
//                    while (true) {
//                        System.out.println("reached " + pointer.node.name + " " + pointer.id + " " +
//                                pointer.tokenPointer + " " + pointer.rulePointer + " " + pointer.ruleTokenPointer +
//                                " " + pointer.foundMatch);
//                        if (seen.contains(pointer.id)) {
//                            pointer = pointer.prev;
//                            System.out.println("looping...");
//                        }
//                        else if (pointer.configRulePointerToRepair(token)) {
//                            System.out.println("repaired node with new similar rule.");
//                            seen.add(pointer.id);
//                            movedToNextRule = true;
//                            break;
//                        }
//                        else if (!pointer.reachedAllRulesEnd()) {
//                            break;
//                        } else {
//                            seen.add(pointer.id);
//                            pointer = pointer.prev;
//                            System.out.println("looping...");
//                        }
//                    }
//                    if (!movedToNextRule) pointer.forwardRule();
//                    currentPointer = pointer.makeChildOfYourself();
//                    currentPointer.inputs = new ArrayList<>(pointer.inputs);
//                    if (!movedToNextRule) currentPointer.tokenPointer = currentPointer.startPoint;
//                }
//            }
//        }
//    }
//}
