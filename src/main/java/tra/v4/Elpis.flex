package tra.v4;

import java.util.*;
import java.util.concurrent.ConcurrentLinkedQueue;
import tra.models.*;
import java_cup.runtime.*;

%%

%class ElpisLexer
%standalone
%column
%line
%cup
%public
%unicode
%char

%{
    public class Tuple {
        public final int line;
        public final int col;
        public final String token;
        public final String type;

        public Tuple(int line, int col, String token, String type) {
            this.line = line;
            this.col = col;
            this.token = token;
            this.type = type;
        }
    }

    Queue<Symbol> symbolPipe = new ConcurrentLinkedQueue<Symbol>();

    int openParenthesis = 0;
    boolean foundString = false;
    int prevLineTabCount = 0;
    StringBuilder string = new StringBuilder();

    public Symbol exportToken(int symNum, Object value, int line, int column) {

        if (value.equals("(")) {
            openParenthesis++;
        }
        else if (value.equals(")")) {
            openParenthesis--;
        }

        if (symNum == sym.TAB) {
            symbolPipe.add(new TabSymbol(symNum, line + 1, column + 1, value));
        }
        else
            symbolPipe.add(new Symbol(symNum, line + 1, column + 1, value));

        if (!symbolPipe.isEmpty()) {
            return symbolPipe.poll();
        } else {
            return new Symbol(sym.EOF);
        }
    }

    public Symbol exportToken(int[] symNums, Object[] values) {

        for (int counter = 0; counter < symNums.length; counter++) {
            if (values[counter].equals("(")) {
                openParenthesis++;
            }
            else if (values[counter].equals(")")) {
                openParenthesis--;
            }

            if (symNums[counter] == sym.TAB) {
                    symbolPipe.add(new TabSymbol(symNums[counter], values[counter]));
            } else {
                symbolPipe.add(new Symbol(symNums[counter], values[counter]));
            }
        }

        if (!symbolPipe.isEmpty()) {
            return symbolPipe.poll();
        } else {
            return new Symbol(sym.EOF);
        }
    }
%}

LineTerminator = \r|\n|\r\n
InputCharacter = [^\r\n]
WhiteSpace = [ \t\f]
Comment = {TraditionalComment} | {EndOfLineComment} | {DocumentationComment}
TraditionalComment = "/*" [^*] ~"*/" | "/*" "*"+ "/"
EndOfLineComment = "//" {InputCharacter}* {LineTerminator}?
DocumentationComment = "/*" "*"+ [^/*] ~"*/"
DecNegIntegerLiteral = 0 | [1-9][0-9]*
DecIntegerLiteral = 0 | -[1-9][0-9]*
HexIntegerLiteral = 0 [xX] 0* {HexDigit} {1,8}
HexDigit          = [0-9a-fA-F]
OctIntegerLiteral = 0+ [1-3]? {OctDigit} {1,15}
OctDigit          = [0-7]
DoubleLiteral = ({FLit1}|{FLit2}|{FLit3}) {Exponent}?
FLit1    = [0-9]+ \. [0-9]*
FLit2    = \. [0-9]+
FLit3    = [0-9]+
Exponent = [eE] [+-]? [0-9]+
NUMBER = {NumberString}
NumberString = {DecIntegerLiteral}|{DecNegIntegerLiteral}|{HexIntegerLiteral}|{OctIntegerLiteral}|{DoubleLiteral}
Identifier = [:jletter:][:jletterdigit:]*
LBRACKET = "["
RBRACKET = "]"
Assign = "="
Equal = "=="
Sum = "+"
Minus = "-"
Multiply = "*"
Divide = "/"
Power = "^"
LParen = "("
RParen = ")"
LBRACE = "{"
RBRACE = "}"
Semicolon = ";"
Colon = ":"
Comma = ","
DOT = "."
Gt = ">"
Lt = "<"
Ge = ">="
Le = "<="
Ne = "!="
Arrow = "->"
WORD = ("."|" "|"("|")"|":"|","|";"|[^"@""[""]""{""}""."" ""("")"":"","";""\n"]*)

STRING = "\""([^\\\"]|\\([^]|\n))*"\""

%%

<YYINITIAL> {
{STRING}                       {String text = yytext(); text = text.replace("\\\"", "\""); return exportToken(sym.STRING, text.substring(1, text.length() - 1), yyline, yycolumn);}
{LBRACE}                       {return exportToken(sym.WORD, yytext(), yyline, yycolumn);}
{RBRACE}                       {return exportToken(sym.WORD, yytext(), yyline, yycolumn);}
{LBRACKET}                     {return exportToken(sym.WORD, yytext(), yyline, yycolumn);}
{RBRACKET}                     {return exportToken(sym.WORD, yytext(), yyline, yycolumn);}
"@"                            {return exportToken(sym.ANNOT, yytext(), yyline, yycolumn);}
<<EOF>>
{
    if (!symbolPipe.isEmpty()) {
        return symbolPipe.poll();
    } else {
        return new Symbol(sym.EOF);
    }
}
{LineTerminator}({WhiteSpace})*
{
    String text = yytext();

    if (text.contains(" ")) {
        if (text.length() > prevLineTabCount) {
            if (openParenthesis == 0)
            if ((text.length() - prevLineTabCount) / 2 == 1) {
                prevLineTabCount = text.length();
                return exportToken(sym.START, yytext(), yyline, yycolumn);
            } else {
                int[] syms = new int[(prevLineTabCount - text.length()) / 2];
                for (int counter = 0; counter < (prevLineTabCount - text.length()) / 2; counter++) {
                    syms[counter] = sym.START;
                }
                Object[] values = new Object[(prevLineTabCount - text.length()) / 2];
                for (int counter = 0; counter < values.length; counter++)
                    values[counter] = new Object();
                prevLineTabCount = text.length();
                return exportToken(syms, values);
            }
        } else if (text.length() < prevLineTabCount) {
            if (openParenthesis == 0)
            if ((prevLineTabCount - text.length()) / 2 == 1) {
                prevLineTabCount = text.length();
                return exportToken(sym.END, yytext(), yyline, yycolumn);
            } else {
                int[] syms = new int[(prevLineTabCount - text.length()) / 2];
                for (int counter = 0; counter < (prevLineTabCount - text.length()) / 2; counter++) {
                    syms[counter] = sym.END;
                }
                Object[] values = new Object[(prevLineTabCount - text.length()) / 2];
                for (int counter = 0; counter < values.length; counter++)
                    values[counter] = new Object();
                prevLineTabCount = text.length();
                return exportToken(syms, values);
            }
        }
    }
    else {
        if (text.length() < prevLineTabCount) {
            if ((prevLineTabCount - text.length()) / 2 == 1) {
                prevLineTabCount = text.length();
                return exportToken(sym.END, yytext(), yyline, yycolumn);
            } else {
                int[] syms = new int[(prevLineTabCount - text.length()) / 2];
                for (int counter = 0; counter < (prevLineTabCount - text.length()) / 2; counter++) {
                    syms[counter] = sym.END;
                }
                Object[] values = new Object[(prevLineTabCount - text.length()) / 2];

                for (int counter = 0; counter < values.length; counter++)
                    values[counter] = new Object();
                prevLineTabCount = text.length();
                return exportToken(syms, values);
            }
        }
    }
}
EMPTY                          {if (!foundString) return exportToken(sym.EMPTY, yytext(), yyline, yycolumn); else string.append(yytext());}
true                           {if (!foundString) return exportToken(sym.TRUE, yytext(), yyline, yycolumn); else string.append(yytext());}
false                          {if (!foundString) return exportToken(sym.FALSE, yytext(), yyline, yycolumn); else string.append(yytext());}
{NUMBER}                       {
    if (!foundString) {
        try {
                            return exportToken(sym.NUMBER, Short.parseShort(yytext()), yyline, yycolumn);
        } catch(Exception ex1) {
            try {
                        return exportToken(sym.NUMBER, Integer.parseInt(yytext()), yyline, yycolumn);
            } catch(Exception ex2) {
                try {
                    return exportToken(sym.NUMBER, Long.parseLong(yytext()), yyline, yycolumn);
                } catch(Exception ex3) {
                    try {
                return exportToken(sym.NUMBER, Float.parseFloat(yytext()), yyline, yycolumn);
                    } catch(Exception ex4) {
                        try {
            return exportToken(sym.NUMBER, Double.parseDouble(yytext()), yyline, yycolumn);
                        } catch(Exception ex5) {
                            return exportToken(sym.NUMBER, Boolean.parseBoolean(yytext()), yyline, yycolumn);
                        }
                    }
                }
            }
        }
    }
    else string.append(yytext());
}
{WhiteSpace}                   {if (foundString) string.append(yytext());}
{WORD}                         {if (!foundString) return exportToken(sym.WORD, yytext(), yyline, yycolumn); else string.append(yytext());}
}