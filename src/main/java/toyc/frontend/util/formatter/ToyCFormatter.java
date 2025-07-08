package toyc.frontend.util.formatter;

import org.antlr.v4.runtime.tree.TerminalNode;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import org.antlr.v4.runtime.tree.ParseTree;
import toyc.ToyCParserBaseVisitor;
import toyc.ToyCParser;
import toyc.ToyCLexer;

public class ToyCFormatter extends ToyCParserBaseVisitor<Void> {
    private static final Map<String, String> SYMBOLS = new HashMap<>();
    private static final String RESET = SGR.Reset.getAnsiCode();
    private static final String KEYWORD = SGR.LightCyan.getAnsiCode();
    private static final String OPERATOR = SGR.LightRed.getAnsiCode();
    private static final String INT_CONST = SGR.Magenta.getAnsiCode();
    private static final String FUNC_NAME = SGR.LightYellow.getAnsiCode();
    private static final String STMT = SGR.White.getAnsiCode();
    private static final String DEF = SGR.LightMagenta.getAnsiCode();

    private static final Set<String> L_BRACKETS = Set.of("L_PAREN", "L_BRACE");
    private static final Set<String> R_BRACKETS = Set.of("R_PAREN", "R_BRACE");
    private static final Set<String> WS_AFTER = Set.of("INT", "VOID", "IF", "ELSE", "WHILE", "PLUS", "MINUS", "MUL", "DIV", "MOD", "ASSIGN", "EQ", "NEQ", "LT", "GT", "LE", "GE", "NOT", "AND", "OR", "COMMA");
    private static final Set<String> WS_BEFORE = Set.of("PLUS", "MINUS", "MUL", "DIV", "MOD", "ASSIGN", "EQ", "NEQ", "LT", "GT", "LE", "GE", "NOT", "AND", "OR");

    private String baseStyle = RESET;
    private boolean inDecl = false;
    private boolean fileStart = true;
    private int parenLevel = 0;
    private int indentLevel = 0;
    private final StringBuilder output = new StringBuilder();

    static {
        // keywords
        SYMBOLS.put("INT", KEYWORD);
        SYMBOLS.put("VOID", KEYWORD);
        SYMBOLS.put("IF", KEYWORD);
        SYMBOLS.put("ELSE", KEYWORD);
        SYMBOLS.put("WHILE", KEYWORD);
        SYMBOLS.put("BREAK", KEYWORD);
        SYMBOLS.put("CONTINUE", KEYWORD);
        SYMBOLS.put("RETURN", KEYWORD);

        // operators
        SYMBOLS.put("PLUS", OPERATOR);
        SYMBOLS.put("MINUS", OPERATOR);
        SYMBOLS.put("MUL", OPERATOR);
        SYMBOLS.put("DIV", OPERATOR);
        SYMBOLS.put("MOD", OPERATOR);
        SYMBOLS.put("ASSIGN", OPERATOR);
        SYMBOLS.put("EQ", OPERATOR);
        SYMBOLS.put("NEQ", OPERATOR);
        SYMBOLS.put("LT", OPERATOR);
        SYMBOLS.put("GT", OPERATOR);
        SYMBOLS.put("LE", OPERATOR);
        SYMBOLS.put("GE", OPERATOR);
        SYMBOLS.put("NOT", OPERATOR);
        SYMBOLS.put("AND", OPERATOR);
        SYMBOLS.put("OR", OPERATOR);
        SYMBOLS.put("COMMA", OPERATOR);
        SYMBOLS.put("SEMICOLON", OPERATOR);

        SYMBOLS.put("INTEGER_CONST", INT_CONST);
    }

    public String getFormattedCode() {
        return output.toString();
    }

    @Override
    public Void visitFuncDef(ToyCParser.FuncDefContext ctx) {

        if (!fileStart) {
            addNewline();
            addNewline();
        } else {
            fileStart = false;
        }

        visitChildren(ctx);
        return null;
    }

    @Override
    public Void visitBlock(ToyCParser.BlockContext ctx) {
        String oldStyle = baseStyle;
        boolean oldInDecl = inDecl;
        baseStyle = RESET;
        inDecl = false;

        boolean isControlBlock = isIsControlBlock(ctx);

        if (!isControlBlock && ctx.getParent() instanceof ToyCParser.StmtContext) {
            addNewline();
        } else {
            if (output.toString().endsWith(" ")) {
                output.deleteCharAt(output.length() - 1);
            }
            output.append(" ");
        }

        indentLevel++;

        visitChildren(ctx);

        inDecl = oldInDecl;
        baseStyle = oldStyle;
        return null;
    }

    @Override
    public Void visitStmt(ToyCParser.StmtContext ctx) {
        String oldStyle = baseStyle;
        boolean oldInDecl = inDecl;
        baseStyle = STMT;
        inDecl = false;

        boolean isElseIf = false;
        if (ctx.IF() != null) {
            ParseTree parent = ctx.getParent();
            if (parent instanceof ToyCParser.StmtContext) {
                ToyCParser.StmtContext stmtCtx = (ToyCParser.StmtContext) parent;
                if (stmtCtx.ELSE() != null && stmtCtx.stmt(1) == ctx) {
                    isElseIf = true;
                }
            }
        }

        boolean needExtraIndent = extraIndent(ctx);

        boolean isEmptySemicolon = ctx.getChildCount() == 2 &&
                ctx.getChild(0) == null &&
                ctx.SEMICOLON() != null;

        if (!isElseIf &&
                !(ctx.getChild(0) instanceof ToyCParser.BlockContext) &&
                !isEmptySemicolon) {
            addNewline();
        }

        if (needExtraIndent && !isElseIf) {
            indentLevel++;
            output.append("    ");
        }

        visitChildren(ctx);

        if (needExtraIndent && !isElseIf) {
            indentLevel--;
        }

        inDecl = oldInDecl;
        baseStyle = oldStyle;
        return null;
    }

    @Override
    public Void visitVarDef(ToyCParser.VarDefContext ctx) {
        String oldStyle = baseStyle;
        boolean oldInDecl = inDecl;
        baseStyle = DEF;
        inDecl = true;

        visitChildren(ctx);

        baseStyle = oldStyle;
        inDecl = oldInDecl;
        return null;
    }

    @Override
    public Void visitTerminal(TerminalNode node) {
        addWSBefore(node);
        addNewlineBefore(node);
        addColoredText(node);
        addWSAfter(node);

        return null;
    }

    private void addColoredText(TerminalNode node) {
        String tokenType = ToyCLexer.VOCABULARY.getSymbolicName(node.getSymbol().getType());
        if (tokenType.equals("EOF")) {
            return;
        }

        String text = node.getText();

        if (R_BRACKETS.contains(tokenType)) {
            parenLevel--;
        }

        String style = determineStyle(tokenType);

        if (L_BRACKETS.contains(tokenType)) {
            parenLevel++;
        }

        if (node.getParent() instanceof ToyCParser.FuncNameContext) {
            style = FUNC_NAME;
        }

        if (inDecl) {
            style = SGR.addUnderline(style);
        }
        output.append(style).append(text).append(RESET);
    }

    private void addNewlineBefore(TerminalNode node) {
        String tokenType = ToyCLexer.VOCABULARY.getSymbolicName(node.getSymbol().getType());
        if (tokenType.equals("R_BRACE") && !inDecl) {
            indentLevel--;
            addNewline();
        }
        if (tokenType.equals("ELSE")) {
            addNewline();
        }
    }

    // Add whitespace before the token if needed (pay attention to unary operators)
    private void addWSBefore(TerminalNode node) {
        String tokenType = ToyCLexer.VOCABULARY.getSymbolicName(node.getSymbol().getType());
        if (WS_BEFORE.contains(tokenType) && !(node.getParent() instanceof ToyCParser.UnaryOpContext)) {
            output.append(" ");
        }
    }

    // Add whitespace after the token if needed (pay attention to unary operators)
    private void addWSAfter(TerminalNode node) {
        String tokenType = ToyCLexer.VOCABULARY.getSymbolicName(node.getSymbol().getType());
        if (WS_AFTER.contains(tokenType) &&  !(node.getParent() instanceof ToyCParser.UnaryOpContext)) {
            output.append(" ");
        }

        if (tokenType.equals("RETURN")) {
            ParseTree nextChild = (node.getParent()).getChild(1);
            if (!(nextChild instanceof TerminalNode && ((TerminalNode) nextChild).getSymbol().getType() == ToyCLexer.SEMICOLON)) {
                output.append(" ");
            }
        }
    }

    private void addNewline() {
        if (output.toString().endsWith(" "))
            output.deleteCharAt(output.length() - 1);
        output.append("\n");
        output.append("    ".repeat(Math.max(0, indentLevel)));
    }

    // Determine the color style of the token
    private String determineStyle(String tokenType) {
        if (L_BRACKETS.contains(tokenType) || R_BRACKETS.contains(tokenType)) {
            return SGR.combine(SGR.values()[SGR.LightRed.ordinal() + (parenLevel % 6)]);
        }
        return SYMBOLS.getOrDefault(tokenType, baseStyle);
    }

    private static boolean isIsControlBlock(ToyCParser.BlockContext ctx) {
        boolean isControlBlock = false;
        if (ctx.getParent() instanceof ToyCParser.StmtContext) {
            ToyCParser.StmtContext stmtParent = (ToyCParser.StmtContext) ctx.getParent();
            if (stmtParent.getParent() instanceof ToyCParser.StmtContext) {
                ToyCParser.StmtContext grandParent = (ToyCParser.StmtContext) stmtParent.getParent();
                isControlBlock = (grandParent.IF() != null || grandParent.WHILE() != null) &&
                        (grandParent.stmt(0) == stmtParent || grandParent.stmt(1) == stmtParent);
            }
        }
        return isControlBlock;
    }

    private static boolean extraIndent(ToyCParser.StmtContext ctx) {
        // If the child of the statement is a block, we don't need extra indent
        if (ctx.getChild(0) instanceof ToyCParser.BlockContext) {
            return false;
        }

        if (ctx.getParent() instanceof ToyCParser.StmtContext) {
            ToyCParser.StmtContext parent = (ToyCParser.StmtContext) ctx.getParent();
            return parent.IF() != null || parent.WHILE() != null || parent.ELSE() != null;
        }
        return false;
    }
}