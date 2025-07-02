package toyc.ir;

import toyc.ToyCParser;
import toyc.ToyCParserBaseVisitor;
import toyc.ir.exp.*;
import toyc.ir.stmt.*;
import toyc.language.Function;
import toyc.language.type.IntType;
import toyc.language.type.Type;
import toyc.language.type.VoidType;
import org.antlr.v4.runtime.ParserRuleContext;

import java.util.*;

public class ToyCIRBuilder extends ToyCParserBaseVisitor<RValue> implements IRBuilder {
    private final Map<String, IR> functions;
    private Function currentFunction;
    private final List<Stmt> stmts;
    private final Map<String, Var> variables;
    private final Map<String, Function> functionMap;
    private int varCounter;
    private int tempCounter;
    
    // Control flow management
    private final Stack<Stmt> breakTargets;
    private final Stack<Stmt> continueTargets;
    
    public ToyCIRBuilder() {
        this.functions = new HashMap<>();
        this.stmts = new ArrayList<>();
        this.variables = new HashMap<>();
        this.functionMap = new HashMap<>();
        this.varCounter = 0;
        this.tempCounter = 0;
        this.breakTargets = new Stack<>();
        this.continueTargets = new Stack<>();
    }
    
    public Map<String, IR> getFunctions() {
        return functions;
    }
    
    /**
     * Updates the functions map with optimized IR.
     * Used for IR optimizations.
     */
    public void updateFunctions(Map<String, IR> optimizedFunctions) {
        functions.clear();
        functions.putAll(optimizedFunctions);
    }

    public IR buildIR(Function function) {
        this.currentFunction = function;
        this.stmts.clear();
        this.variables.clear();
        this.varCounter = 0;
        
        // Create parameters
        List<Var> params = new ArrayList<>();
        for (int i = 0; i < function.getParamCount(); i++) {
            String paramName = function.getParamName(i);
            if (paramName == null) paramName = "param" + i;
            Var param = new Var(function, paramName, function.getParamType(i), i);
            params.add(param);
            variables.put(paramName, param);
        }
        
        // TODO: Parse and convert function body to statements
        // This will be called by the World/compiler when building IR
        
        Set<Var> returnVars = new HashSet<>();
        List<Var> allVars = new ArrayList<>(params);
        allVars.addAll(variables.values());
        
        return new DefaultIR(function, params, returnVars, allVars, stmts);
    }
    
    @Override
    public RValue visitProgram(ToyCParser.ProgramContext ctx) {
        return visit(ctx.compUnit());
    }
    
    @Override
    public RValue visitCompUnit(ToyCParser.CompUnitContext ctx) {
        // First pass: collect function signatures
        for (ToyCParser.FuncDefContext funcDef : ctx.funcDef()) {
            collectFunctionSignature(funcDef);
        }
        
        // Second pass: build IR for each function
        for (ToyCParser.FuncDefContext funcDef : ctx.funcDef()) {
            visit(funcDef);
        }
        return null;
    }
    
    private void collectFunctionSignature(ToyCParser.FuncDefContext ctx) {
        String funcName = ctx.funcName().getText();
        boolean isVoid = ctx.funcType().VOID() != null;
        Type returnType = isVoid ? VoidType.VOID : IntType.INT;
        
        List<Type> paramTypes = new ArrayList<>();
        List<String> paramNames = new ArrayList<>();
        if (ctx.funcFParams() != null) {
            for (ToyCParser.FuncFParamContext param : ctx.funcFParams().funcFParam()) {
                paramTypes.add(IntType.INT); // ToyC only has int parameters
                paramNames.add(param.IDENT().getText());
            }
        }
        
        Function function = new Function(funcName, paramTypes, returnType, paramNames);
        functionMap.put(funcName, function);
    }
    
    @Override
    public RValue visitFuncDef(ToyCParser.FuncDefContext ctx) {
        String funcName = ctx.funcName().getText();
        currentFunction = functionMap.get(funcName);
        
        stmts.clear();
        variables.clear();
        varCounter = 0; // Reset variable counter for each function
        tempCounter = 0; // Reset temp counter for each function
        
        // Create parameter variables
        List<Var> params = new ArrayList<>();
        if (ctx.funcFParams() != null) {
            for (int i = 0; i < ctx.funcFParams().funcFParam().size(); i++) {
                ToyCParser.FuncFParamContext param = ctx.funcFParams().funcFParam().get(i);
                String paramName = param.IDENT().getText();
                Var paramVar = new Var(currentFunction, paramName, IntType.INT, i);
                params.add(paramVar);
                variables.put(paramName, paramVar);
            }
        }
        
        // Visit function body
        visit(ctx.block());
        
        // Add implicit return for void functions
        boolean isVoid = ctx.funcType().VOID() != null;
        if (isVoid && (stmts.isEmpty() || !(stmts.getLast() instanceof Return))) {
            addStatement(new Return(null), ctx);
        }
        
        // Build and store IR
        Set<Var> returnVars = new HashSet<>();
        List<Var> allVars = new ArrayList<>(params);
        allVars.addAll(variables.values().stream()
                .filter(v -> !params.contains(v))
                .toList());
        
        // Optimize jumps before setting indices
        optimizeJumps();
        
        // Set indices for statements
        for (int i = 0; i < stmts.size(); i++) {
            stmts.get(i).setIndex(i);
        }
        
        IR ir = new DefaultIR(currentFunction, params, returnVars, allVars, stmts);
        functions.put(funcName, ir);
        
        return null;
    }
    
    @Override
    public RValue visitBlock(ToyCParser.BlockContext ctx) {
        for (ToyCParser.StmtContext stmt : ctx.stmt()) {
            visit(stmt);
        }
        return null;
    }
    
    @Override
    public RValue visitStmt(ToyCParser.StmtContext ctx) {
        if (ctx.block() != null) {
            visit(ctx.block());
        } else if (ctx.lVal() != null && ctx.ASSIGN() != null) {
            Var target = (Var) visit(ctx.lVal());
            RValue source = visit(ctx.exp());
            if (source instanceof Literal) {
                addStatement(new AssignLiteral(target, (Literal) source), ctx);
            } else if (source instanceof Var) {
                addStatement(new Copy(target, (Var) source), ctx);
            } else if (source instanceof BinaryExp) {
                addStatement(new Binary(target, (BinaryExp) source), ctx);
            } else if (source instanceof UnaryExp) {
                addStatement(new Unary(target, (UnaryExp) source), ctx);
            } else if (source instanceof CallExp) {
                addStatement(new Call(currentFunction, (CallExp) source, target), ctx);
            }
        } else if (ctx.RETURN() != null) {
            Var returnValue = null;
            if (ctx.exp() != null) {
                RValue exp = visit(ctx.exp());
                if (exp instanceof Var) {
                    returnValue = (Var) exp;
                } else {
                    // Create temporary for complex expressions
                    returnValue = createTemp();
                    if (exp instanceof Literal) {
                        addStatement(new AssignLiteral(returnValue, (Literal) exp), ctx);
                    } else if (exp instanceof BinaryExp) {
                        addStatement(new Binary(returnValue, (BinaryExp) exp), ctx);
                    } else if (exp instanceof UnaryExp) {
                        addStatement(new Unary(returnValue, (UnaryExp) exp), ctx);
                    }
                }
            }
            addStatement(new Return(returnValue), ctx);
        } else if (ctx.exp() != null && ctx.SEMICOLON() != null) {
            // Expression statement (function call without assignment)
            RValue exp = visit(ctx.exp());
            if (exp instanceof CallExp) {
                addStatement(new Call(currentFunction, (CallExp) exp), ctx);
            }
        } else if (ctx.varDef() != null) {
            visit(ctx.varDef());
        } else if (ctx.IF() != null) {
            visitIfStatement(ctx);
        } else if (ctx.WHILE() != null) {
            visitWhileStatement(ctx);
        } else if (ctx.BREAK() != null) {
            if (!breakTargets.isEmpty()) {
                Goto breakGoto = new Goto();
                breakGoto.setTarget(breakTargets.peek());
                addStatement(breakGoto, ctx);
            } else {
                throw new RuntimeException("break statement outside of loop");
            }
        } else if (ctx.CONTINUE() != null) {
            if (!continueTargets.isEmpty()) {
                Goto continueGoto = new Goto();
                continueGoto.setTarget(continueTargets.peek());
                addStatement(continueGoto, ctx);
            } else {
                throw new RuntimeException("continue statement outside of loop");
            }
        }
        
        return null;
    }
    
    private Var createTemp() {
        String tempName = "temp$" + tempCounter++;
        return new Var(currentFunction, tempName, IntType.INT, -1);
    }
    
    private void visitIfStatement(ToyCParser.StmtContext ctx) {
        RValue conditionExp = visit(ctx.exp());
        
        // Convert condition to ConditionExp if needed
        ConditionExp condition = createCondition(conditionExp, ctx);
        
        // Create placeholder statements for control flow targets
        Stmt thenStart = createNopPlaceholder();
        Stmt afterIf = createNopPlaceholder();
        
        // Create conditional jump: if (condition) goto thenStart
        If ifStmt = new If(condition);
        ifStmt.setTarget(thenStart);
        addStatement(ifStmt, ctx);
        
        if (ctx.stmt().size() > 1) { // Has else clause
            // if (!condition) fall through to else, then goto afterIf
            Stmt elseStart = createNopPlaceholder();
            Goto gotoElse = new Goto();
            gotoElse.setTarget(elseStart);
            addStatement(gotoElse);
            
            // Then branch
            addStatement(thenStart);
            visit(ctx.stmt(0));
            Goto gotoEnd1 = new Goto();
            gotoEnd1.setTarget(afterIf);
            addStatement(gotoEnd1);
            
            // Else branch  
            addStatement(elseStart);
            visit(ctx.stmt(1));
        } else {
            // No else clause: if (!condition) goto afterIf
            Goto gotoEnd = new Goto();
            gotoEnd.setTarget(afterIf);
            addStatement(gotoEnd);
            
            // Then branch
            addStatement(thenStart);
            visit(ctx.stmt(0));
        }
        
        // After if statement
        addStatement(afterIf);
    }
    
    private Stmt createNopPlaceholder() {
        // Create a no-operation statement that can serve as a jump target
        return new Nop();
    }
    
    private ConditionExp createCondition(RValue conditionExp, ParserRuleContext ctx) {
        if (conditionExp instanceof ConditionExp) {
            return (ConditionExp) conditionExp;
        } else {
            // Create a condition comparing with zero: var != 0
            Var condVar = convertToVar(conditionExp, ctx);
            Var zeroVar = createTemp();
            addStatement(new AssignLiteral(zeroVar, IntLiteral.get(0)), ctx);
            return new ConditionExp(ConditionExp.Op.NE, condVar, zeroVar);
        }
    }
    
    
    private void visitWhileStatement(ToyCParser.StmtContext ctx) {
        // Create placeholders for while loop structure
        Stmt conditionStart = createNopPlaceholder();
        Stmt bodyStart = createNopPlaceholder();
        Stmt afterWhile = createNopPlaceholder();
        
        // Push break/continue targets
        breakTargets.push(afterWhile);
        continueTargets.push(conditionStart);
        
        // Jump to condition
        Goto gotoCondition = new Goto();
        gotoCondition.setTarget(conditionStart);
        addStatement(gotoCondition, ctx);
        
        // Condition evaluation
        addStatement(conditionStart);
        RValue conditionExp = visit(ctx.exp());
        
        // Convert condition to ConditionExp if needed
        ConditionExp condition = createCondition(conditionExp, ctx);
        
        // Conditional jump: if (condition) goto bodyStart
        If ifStmt = new If(condition);
        ifStmt.setTarget(bodyStart);
        addStatement(ifStmt, ctx);
        
        // If condition false, goto afterWhile
        Goto gotoEnd = new Goto();
        gotoEnd.setTarget(afterWhile);
        addStatement(gotoEnd);
        
        // Body
        addStatement(bodyStart);
        visit(ctx.stmt(0));
        
        // Jump back to condition
        Goto gotoLoop = new Goto();
        gotoLoop.setTarget(conditionStart);
        addStatement(gotoLoop);
        
        // After while
        addStatement(afterWhile);
        
        // Pop break/continue targets
        breakTargets.pop();
        continueTargets.pop();
    }
    
    @Override
    public RValue visitVarDef(ToyCParser.VarDefContext ctx) {
        String varName = ctx.IDENT().getText();
        Var var = new Var(currentFunction, varName, IntType.INT, varCounter++);
        variables.put(varName, var);
        
        RValue initValue = visit(ctx.exp());
        if (initValue instanceof Literal) {
            addStatement(new AssignLiteral(var, (Literal) initValue), ctx);
        } else if (initValue instanceof Var) {
            addStatement(new Copy(var, (Var) initValue), ctx);
        } else if (initValue instanceof BinaryExp) {
            addStatement(new Binary(var, (BinaryExp) initValue), ctx);
        } else if (initValue instanceof UnaryExp) {
            addStatement(new Unary(var, (UnaryExp) initValue), ctx);
        } else if (initValue instanceof CallExp) {
            addStatement(new Call(currentFunction, (CallExp) initValue, var), ctx);
        }
        
        return var;
    }
    
    @Override
    public RValue visitExp(ToyCParser.ExpContext ctx) {
        if (ctx.funcName() != null) {
            return visitFunctionCall(ctx);
        } else if (ctx.L_PAREN() != null) {
            if (ctx.exp(0) != null) {
                return visit(ctx.exp(0));
            } else {
                return null;
            }
        } else if (ctx.lVal() != null) {
            return visit(ctx.lVal());
        } else if (ctx.number() != null) {
            return visit(ctx.number());
        } else if (ctx.unaryOp() != null) {
            return visitUnaryOp(ctx);
        } else if (ctx.exp().size() == 2) {
            return visitBinaryOp(ctx);
        }
        
        return null;
    }
    
    private RValue visitFunctionCall(ToyCParser.ExpContext ctx) {
        String funcName = ctx.funcName().getText();
        Function function = functionMap.get(funcName);
        if (function == null) {
            throw new RuntimeException("Undefined function: " + funcName);
        }
        
        List<Var> arguments = new ArrayList<>();
        if (ctx.funcRParams() != null) {
            for (ToyCParser.FuncRParamContext param : ctx.funcRParams().funcRParam()) {
                RValue arg = visit(param.exp());
                if (arg instanceof Var) {
                    arguments.add((Var) arg);
                } else {
                    // Create temporary for complex arguments
                    Var temp = createTemp();
                    if (arg instanceof Literal) {
                        addStatement(new AssignLiteral(temp, (Literal) arg), ctx);
                    } else if (arg instanceof BinaryExp) {
                        addStatement(new Binary(temp, (BinaryExp) arg), ctx);
                    }
                    arguments.add(temp);
                }
            }
        }
        
        return new CallExp(function, arguments);
    }
    
    private RValue visitUnaryOp(ToyCParser.ExpContext ctx) {
        String opText = ctx.unaryOp().getText();
        RValue operand = visit(ctx.exp(0));

        switch (opText) {
            case "+" -> {
                return operand; // Unary plus is no-op
            }
            case "-" -> {
                Var operandVar = convertToVar(operand, ctx);
                return new NegExp(operandVar);
            }
            case "!" -> {
                Var operandVar = convertToVar(operand, ctx);
                return new NotExp(operandVar);
            }
            case null, default ->
                    throw new RuntimeException("Unknown unary operator: " + opText);
        }
    }
    
    private RValue visitBinaryOp(ToyCParser.ExpContext ctx) {
        RValue left = visit(ctx.exp(0));
        RValue right = visit(ctx.exp(1));
        
        // Convert to variables if needed
        Var leftVar = convertToVar(left, ctx);
        Var rightVar = convertToVar(right, ctx);
        
        if (ctx.MUL() != null) {
            return new ArithmeticExp(ArithmeticExp.Op.MUL, leftVar, rightVar);
        } else if (ctx.DIV() != null) {
            return new ArithmeticExp(ArithmeticExp.Op.DIV, leftVar, rightVar);
        } else if (ctx.MOD() != null) {
            return new ArithmeticExp(ArithmeticExp.Op.REM, leftVar, rightVar);
        } else if (ctx.PLUS() != null) {
            return new ArithmeticExp(ArithmeticExp.Op.ADD, leftVar, rightVar);
        } else if (ctx.MINUS() != null) {
            return new ArithmeticExp(ArithmeticExp.Op.SUB, leftVar, rightVar);
        } else if (ctx.LT() != null) {
            return new ConditionExp(ConditionExp.Op.LT, leftVar, rightVar);
        } else if (ctx.GT() != null) {
            return new ConditionExp(ConditionExp.Op.GT, leftVar, rightVar);
        } else if (ctx.LE() != null) {
            return new ConditionExp(ConditionExp.Op.LE, leftVar, rightVar);
        } else if (ctx.GE() != null) {
            return new ConditionExp(ConditionExp.Op.GE, leftVar, rightVar);
        } else if (ctx.EQ() != null) {
            return new ConditionExp(ConditionExp.Op.EQ, leftVar, rightVar);
        } else if (ctx.NEQ() != null) {
            return new ConditionExp(ConditionExp.Op.NE, leftVar, rightVar);
        } else if (ctx.AND() != null) {
            return new ConditionExp(ConditionExp.Op.AND, leftVar, rightVar);
        } else if (ctx.OR() != null) {
            return new ConditionExp(ConditionExp.Op.OR, leftVar, rightVar);
        } else {
            throw new RuntimeException("Unknown binary operator");
        }
    }
    
    private Var convertToVar(RValue exp, ParserRuleContext ctx) {
        if (exp instanceof Var) {
            return (Var) exp;
        } else {
            Var temp = createTemp();
            switch (exp) {
                case Literal literal ->
                        addStatement(new AssignLiteral(temp, literal), ctx);
                case BinaryExp binaryExp ->
                        addStatement(new Binary(temp, binaryExp), ctx);
                case UnaryExp unaryExp -> addStatement(new Unary(temp, unaryExp), ctx);
                case CallExp callExp ->
                        addStatement(new Call(currentFunction, callExp, temp), ctx);
                default ->
                        throw new RuntimeException("Cannot convert expression to variable: " + exp.getClass().getSimpleName());
            }
            return temp;
        }
    }
    
    @Override
    public RValue visitLVal(ToyCParser.LValContext ctx) {
        String varName = ctx.IDENT().getText();
        Var var = variables.get(varName);
        if (var == null) {
            throw new RuntimeException("Undefined variable: " + varName);
        }
        return var;
    }
    
    @Override
    public RValue visitNumber(ToyCParser.NumberContext ctx) {
        int value = Integer.parseInt(ctx.INTEGER_CONST().getText());
        return IntLiteral.get(value);
    }
    
    /**
     * Optimizes jump statements by eliminating redundant jumps through NOPs.
     * For example: "goto A; A: nop; B: goto C" becomes "goto C; A: nop; B: goto C"
     */
    private void optimizeJumps() {
        boolean changed = true;
        while (changed) {
            changed = false;

            for (Stmt stmt : stmts) {
                // Optimize Goto statements
                if (stmt instanceof Goto gotoStmt) {
                    Stmt target = gotoStmt.getTarget();
                    Stmt newTarget = findUltimateTarget(target);
                    if (newTarget != target) {
                        gotoStmt.setTarget(newTarget);
                        changed = true;
                    }
                }

                // Optimize If statements
                if (stmt instanceof If ifStmt) {
                    Stmt target = ifStmt.getTarget();
                    Stmt newTarget = findUltimateTarget(target);
                    if (newTarget != target) {
                        ifStmt.setTarget(newTarget);
                        changed = true;
                    }
                }
            }
        }
    }
    
    /**
     * Finds the ultimate target of a jump by following chains of NOPs and single Gotos.
     */
    private Stmt findUltimateTarget(Stmt target) {
        Set<Stmt> visited = new HashSet<>();
        Stmt current = target;
        
        while (current != null && !visited.contains(current)) {
            visited.add(current);
            
            // If target is a NOP, skip to the next non-NOP statement
            if (current instanceof Nop) {
                int targetIndex = stmts.indexOf(current);
                if (targetIndex >= 0 && targetIndex + 1 < stmts.size()) {
                    Stmt nextStmt = stmts.get(targetIndex + 1);
                    // If next statement is a Goto, follow it
                    if (nextStmt instanceof Goto nextGoto) {
                        current = nextGoto.getTarget();
                        continue;
                    } else {
                        // For any other statement type, target it directly (skip the NOP)
                        current = nextStmt;
                        continue;
                    }
                }
                break;
            }
            
            // If target is a Goto to another statement, follow it
            if (current instanceof Goto gotoStmt) {
                current = gotoStmt.getTarget();
                continue;
            }
            
            break;
        }
        
        return current != null ? current : target;
    }
    
    /**
     * Helper method to add a statement with line number from parse context
     */
    private void addStatement(Stmt stmt, ToyCParser.StmtContext ctx) {
        if (ctx != null && ctx.getStart() != null) {
            stmt.setLineNumber(ctx.getStart().getLine());
        }
        stmts.add(stmt);
    }
    
    /**
     * Helper method to add a statement with line number from any parse tree context
     */
    private void addStatement(Stmt stmt, ParserRuleContext ctx) {
        if (ctx != null && ctx.getStart() != null) {
            stmt.setLineNumber(ctx.getStart().getLine());
        }
        stmts.add(stmt);
    }
    
    /**
     * Helper method to add a statement without context (for generated statements)
     */
    private void addStatement(Stmt stmt) {
        // Keep default -1 line number for generated statements
        stmts.add(stmt);
    }
}