package toyc.ir;

import toyc.ir.exp.CallExp;
import toyc.ir.exp.Literal;
import toyc.ir.stmt.Call;
import toyc.ir.stmt.Stmt;

import java.io.PrintStream;
import java.util.Formatter;
import java.util.stream.Collectors;

public class IRPrinter {

    public static void print(IR ir, PrintStream out) {
        // print method signature
        out.println("---------- " + ir.getFunction() + " ----------");
        // print parameters
        out.print("Parameters: ");
        out.println(ir.getParams()
                .stream()
                .map(p -> p.getType() + " " + p)
                .collect(Collectors.joining(", ")));
        // print all variables
        out.println("Variables:");
        ir.getVars().forEach(v -> out.println(v.getType() + " " + v));
        // print all statements
        out.println("Statements:");
        ir.forEach(s -> out.println(toString(s)));
    }

    public static String toString(Stmt stmt) {
        if (stmt instanceof Call) {
            return toString((Call) stmt);
        } else {
            return String.format("%s %s;", position(stmt), stmt);
        }
    }

    public static String toString(Call call) {
        Formatter formatter = new Formatter();
        formatter.format("%s ", position(call));
        if (call.getResult() != null) {
            // some variable names contain '%', which will be treated as
            // format specifier by formatter, thus we need to escape it
            String lhs = call.getResult().toString().replace("%", "%%");
            formatter.format(lhs + " = ");
        }
        CallExp ie = call.getCallExp();
        formatter.format("%s ", "call");
        formatter.format("<%s %s%s>%s",ie.getType(),
                ie.getFunction().getName(), ie.getFunction().getParamTypes(),
                ie.getArgsString());
        return formatter.toString();
    }

    public static String position(Stmt stmt) {
        return "[" +
                stmt.getIndex() +
                "@L" + stmt.getLineNumber() +
                ']';
    }
}
