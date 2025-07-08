package toyc.frontend.ir;

import toyc.ToyCParser;
import toyc.ToyCParserBaseVisitor;
import toyc.ir.IR;
import toyc.ir.exp.*;
import toyc.ir.stmt.*;
import toyc.language.Function;
import toyc.language.Program;
import toyc.language.type.IntType;
import toyc.language.type.Type;
import toyc.language.type.VoidType;
import toyc.util.Timer;
import org.antlr.v4.runtime.ParserRuleContext;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import toyc.ir.DefaultIR;

import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public class IRBuilder extends ToyCParserBaseVisitor<RValue> implements toyc.ir.IRBuilder {
    
    private static final Logger logger = LogManager.getLogger(IRBuilder.class);
    private final Map<String, IR> functions;
    private Function currentFunction;
    private final List<Stmt> stmts;
    private final Map<String, Function> functionMap;
    private int varCounter;
    private int tempCounter;
    
    // Scoped symbol table management
    private final Stack<Map<String, Var>> scopeStack;
    private final Map<String, Integer> variableCounters;

    // Control flow management
    private final Stack<Stmt> breakTargets;
    private final Stack<Stmt> continueTargets;

    public IRBuilder() {
        this.functions = new HashMap<>();
        this.stmts = new ArrayList<>();
        this.functionMap = new HashMap<>();
        this.varCounter = 0;
        this.tempCounter = 0;
        this.breakTargets = new Stack<>();
        this.continueTargets = new Stack<>();
        this.scopeStack = new Stack<>();
        this.variableCounters = new HashMap<>();
    }
    
    /**
     * Enters a new scope.
     */
    private void enterScope() {
        scopeStack.push(new HashMap<>());
    }
    
    /**
     * Exits the current scope.
     */
    private void exitScope() {
        if (!scopeStack.isEmpty()) {
            scopeStack.pop();
        }
    }
    
    /**
     * Defines a variable in the current scope with proper name resolution for shadowing.
     * @param name The original variable name
     * @param var The variable to define
     */
    private void defineVariable(String name, Var var) {
        if (!scopeStack.isEmpty()) {
            scopeStack.peek().put(name, var);
        }
    }
    
    /**
     * Looks up a variable in the current scope stack.
     * @param name The variable name to look up
     * @return The variable if found, null otherwise
     */
    private Var lookupVariable(String name) {
        // Search from current scope to outer scopes
        for (int i = scopeStack.size() - 1; i >= 0; i--) {
            Map<String, Var> scope = scopeStack.get(i);
            if (scope.containsKey(name)) {
                return scope.get(name);
            }
        }
        return null;
    }
    
    /**
     * Gets all variables from all scopes.
     * @return A list of all variables
     */
    private List<Var> getAllVariables() {
        List<Var> allVars = new ArrayList<>();
        for (Map<String, Var> scope : scopeStack) {
            allVars.addAll(scope.values());
        }
        return allVars;
    }
    
    /**
     * Generates a unique variable name for shadowed variables.
     * @param originalName The original variable name
     * @return A unique name with _i suffix
     */
    private String generateUniqueVariableName(String originalName) {
        int counter = variableCounters.getOrDefault(originalName, 0);
        counter++;
        variableCounters.put(originalName, counter);
        return originalName + "_" + counter;
    }

    public IR buildIR(Function function) {
        this.currentFunction = function;
        this.stmts.clear();
        this.varCounter = 0;
        this.variableCounters.clear();
        this.scopeStack.clear();

        // Create parameters
        List<Var> params = new ArrayList<>();
        
        // Enter function scope
        enterScope();
        
        for (int i = 0; i < function.getParamCount(); i++) {
            String paramName = function.getParamName(i);
            if (paramName == null) paramName = "param" + i;
            Var param = new Var(function, paramName, function.getParamType(i), i);
            params.add(param);
            defineVariable(paramName, param);
        }

        Set<Var> returnVars = new HashSet<>();
        List<Var> allVars = new ArrayList<>(params);
        allVars.addAll(getAllVariables());

        return new DefaultIR(function, params, returnVars, allVars, stmts);
    }

    /**
     * Builds IR for all functions in the given program.
     * Required by the IRBuilder interface.
     */
    @Override
    public void buildAll(Program program) {
        Timer timer = new Timer("Build IR for all functions");
        timer.start();
        int nThreads = Runtime.getRuntime().availableProcessors();

        // Group all functions by number of threads
        List<List<Function>> groups = new ArrayList<>();
        for (int i = 0; i < nThreads; ++i) {
            groups.add(new ArrayList<>());
        }

        List<Function> functions = program.allFunctions().toList();
        int i = 0;
        for (Function f : functions) {
            groups.get(i++ % nThreads).add(f);
        }

        // Build IR for all functions in parallel
        ExecutorService service = Executors.newFixedThreadPool(nThreads);
        for (List<Function> group : groups) {
            service.execute(() -> group.forEach(Function::getIR));
        }
        service.shutdown();
        try {
            service.awaitTermination(Long.MAX_VALUE, TimeUnit.NANOSECONDS);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
        timer.stop();
        logger.info(timer);
    }

    public Map<String, IR> getFunctions() {
        return functions;
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
        String funcName = ctx.funcName().IDENT().getText();
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
        String funcName = ctx.funcName().IDENT().getText();
        currentFunction = functionMap.get(funcName);

        stmts.clear();
        scopeStack.clear();
        variableCounters.clear();
        varCounter = 0; // Reset variable counter for each function
        tempCounter = 0; // Reset temp counter for each function

        // Enter function scope
        enterScope();
        
        // Create parameter variables
        List<Var> params = new ArrayList<>();
        if (ctx.funcFParams() != null) {
            for (int i = 0; i < ctx.funcFParams().funcFParam().size(); i++) {
                ToyCParser.FuncFParamContext param = ctx.funcFParams().funcFParam().get(i);
                String paramName = param.IDENT().getText();
                Var paramVar = new Var(currentFunction, paramName, IntType.INT, i);
                params.add(paramVar);
                defineVariable(paramName, paramVar);
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
        
        // Add only local variables (not parameters) to allVars
        List<Var> allScopedVars = getAllVariables();
        for (Var var : allScopedVars) {
            boolean isParameter = false;
            for (Var param : params) {
                if (param.getName().equals(var.getName())) {
                    isParameter = true;
                    break;
                }
            }
            if (!isParameter) {
                allVars.add(var);
            }
        }

        // Optimize jumps and eliminate redundant NOPs before setting indices
        optimizeJumps();
        eliminateRedundantNops();

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
        // Enter new scope for block
        enterScope();
        
        for (ToyCParser.StmtContext stmt : ctx.stmt()) {
            visit(stmt);
        }
        
        // Exit scope when leaving block
        exitScope();
        return null;
    }

    @Override
    public RValue visitStmt(ToyCParser.StmtContext ctx) {
        if (ctx.block() != null) {
            visit(ctx.block());
        } else if (ctx.lVal() != null && ctx.ASSIGN() != null) {
            Var target = (Var) visit(ctx.lVal());
            RValue source = visit(ctx.exp());
            if (source instanceof Literal literal) {
                addStatement(new AssignLiteral(target, literal), ctx);
            } else if (source instanceof Var var) {
                addStatement(new Copy(target, var), ctx);
            } else if (source instanceof BinaryExp binaryExp) {
                addStatement(new Binary(target, binaryExp), ctx);
            } else if (source instanceof UnaryExp unaryExp) {
                addStatement(new Unary(target, unaryExp), ctx);
            } else if (source instanceof CallExp callExp) {
                addStatement(new Call(currentFunction, callExp, target), ctx);
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
                    if (exp instanceof Literal literalExp) {
                        addStatement(new AssignLiteral(returnValue, literalExp), ctx);
                    } else if (exp instanceof BinaryExp binaryExp) {
                        addStatement(new Binary(returnValue, binaryExp), ctx);
                    } else if (exp instanceof UnaryExp unaryExp) {
                        addStatement(new Unary(returnValue, unaryExp), ctx);
                    } else if (exp instanceof CallExp callExp) {
                        addStatement(new Call(currentFunction, callExp, returnValue), ctx);
                    }
                }
            }
            addStatement(new Return(returnValue), ctx);
        } else if (ctx.exp() != null && ctx.SEMICOLON() != null) {
            // Expression statement (function call without assignment)
            RValue exp = visit(ctx.exp());
            if (exp instanceof CallExp callExp) {
                addStatement(new Call(currentFunction, callExp), ctx);
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

        // Check if then-branch is a simple break or continue statement
        boolean isSimpleBreak = isSimpleBreakOrContinue(ctx.stmt(0));
        
        if (isSimpleBreak && ctx.stmt().size() == 1) {
            // Optimize: if (condition) break; -> if (condition) goto breakTarget;
            If ifStmt = new If(condition);
            
            // Check if it's a direct break/continue or inside a block
            ToyCParser.StmtContext targetStmt = ctx.stmt(0);
            if (targetStmt.block() != null && targetStmt.block().stmt().size() == 1) {
                targetStmt = targetStmt.block().stmt(0);
            }
            
            if (targetStmt.BREAK() != null && !breakTargets.isEmpty()) {
                ifStmt.setTarget(breakTargets.peek());
            } else if (targetStmt.CONTINUE() != null && !continueTargets.isEmpty()) {
                ifStmt.setTarget(continueTargets.peek());
            }
            addStatement(ifStmt, ctx);
            return;
        }

        // Create placeholder statements for control flow targets
        Stmt thenStart = createNopPlaceholder();
        Stmt afterIf = createNopPlaceholder();

        // Create conditional jump: if (condition) goto thenStart
        If ifStmt = new If(condition);
        ifStmt.setTarget(thenStart);
        addStatement(ifStmt, ctx);

        if (ctx.stmt().size() > 1) { // Has else clause
            boolean thenEndsWithReturn = endsWithReturn(ctx.stmt(0));
            boolean elseEndsWithReturn = endsWithReturn(ctx.stmt(1));
            
            // if (!condition) fall through to else, then goto afterIf
            Stmt elseStart = createNopPlaceholder();
            Goto gotoElse = new Goto();
            gotoElse.setTarget(elseStart);
            addStatement(gotoElse, ctx);

            // Then branch
            addStatement(thenStart);
            visit(ctx.stmt(0));
            
            // Only add goto after then-branch if it doesn't end with return
            if (!thenEndsWithReturn) {
                Goto gotoEnd1 = new Goto();
                gotoEnd1.setTarget(afterIf);
                addStatement(gotoEnd1, ctx);
            }

            // Else branch
            addStatement(elseStart);
            visit(ctx.stmt(1));
            
            // Only add afterIf if at least one branch doesn't end with return
            if (!thenEndsWithReturn || !elseEndsWithReturn) {
                addStatement(afterIf);
            }
        } else {
            // No else clause: if (!condition) goto afterIf
            Goto gotoEnd = new Goto();
            gotoEnd.setTarget(afterIf);
            addStatement(gotoEnd, ctx);

            // Then branch
            addStatement(thenStart);
            visit(ctx.stmt(0));
            
            // Always add afterIf for single-branch if statements
            addStatement(afterIf);
        }
    }

    private boolean isSimpleBreakOrContinue(ToyCParser.StmtContext stmt) {
        if (stmt.BREAK() != null || stmt.CONTINUE() != null) {
            return true;
        }
        // Check if it's a block with a single break or continue statement
        if (stmt.block() != null && stmt.block().stmt().size() == 1) {
            ToyCParser.StmtContext innerStmt = stmt.block().stmt(0);
            return innerStmt.BREAK() != null || innerStmt.CONTINUE() != null;
        }
        return false;
    }

    private boolean endsWithReturn(ToyCParser.StmtContext stmt) {
        if (stmt.RETURN() != null) {
            return true;
        }
        // Check if it's a block that ends with a return statement
        if (stmt.block() != null && !stmt.block().stmt().isEmpty()) {
            List<ToyCParser.StmtContext> stmts = stmt.block().stmt();
            return endsWithReturn(stmts.getLast());
        }
        return false;
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

        // Condition evaluation (no initial jump needed, fall through)
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
        addStatement(gotoEnd, ctx);

        // Body
        addStatement(bodyStart);
        visit(ctx.stmt(0));

        // Jump back to condition
        Goto gotoLoop = new Goto();
        gotoLoop.setTarget(conditionStart);
        addStatement(gotoLoop, ctx);

        // After while
        addStatement(afterWhile);

        // Pop break/continue targets
        breakTargets.pop();
        continueTargets.pop();
    }

    @Override
    public RValue visitVarDef(ToyCParser.VarDefContext ctx) {
        String originalName = ctx.IDENT().getText();
        
        // Check if variable is being shadowed
        String actualName = originalName;
        if (lookupVariable(originalName) != null) {
            // Variable is being shadowed, generate unique name
            actualName = generateUniqueVariableName(originalName);
        }
        
        Var lVar = new Var(currentFunction, actualName, IntType.INT, varCounter++);
        defineVariable(originalName, lVar);

        RValue initValue = visit(ctx.exp());
        if (initValue instanceof Literal literal) {
            addStatement(new AssignLiteral(lVar, literal), ctx);
        } else if (initValue instanceof Var rVar) {
            addStatement(new Copy(lVar, rVar), ctx);
        } else if (initValue instanceof BinaryExp binaryExp) {
            addStatement(new Binary(lVar, binaryExp), ctx);
        } else if (initValue instanceof UnaryExp unaryExp) {
            addStatement(new Unary(lVar, unaryExp), ctx);
        } else if (initValue instanceof CallExp callExp) {
            addStatement(new Call(currentFunction, callExp, lVar), ctx);
        }

        return lVar;
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
        String funcName = ctx.funcName().IDENT().getText();
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
                    } else if (arg instanceof UnaryExp) {
                        addStatement(new Unary(temp, (UnaryExp) arg), ctx);
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
        Var var = lookupVariable(varName);
        if (var == null) {
            throw new RuntimeException("Undefined variable: " + varName);
        }
        return var;
    }

    @Override
    public RValue visitNumber(ToyCParser.NumberContext ctx) {
        int value = Integer.parseInt(ctx.INTEGER_CONST().getText());
        if (ctx.MINUS() != null) {
            return IntLiteral.get(-value);
        } else {
            return IntLiteral.get(value);
        }
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
     * Eliminates redundant NOP statements that are no longer needed as jump targets.
     * A NOP is considered redundant if:
     * 1. No other statements jump to it, OR
     * 2. It's immediately followed by another statement that all jumpers could target instead
     */
    private void eliminateRedundantNops() {
        // Find all jump targets
        Set<Stmt> jumpTargets = findAllJumpTargets();
        
        // Create a list to track statements to remove
        List<Stmt> toRemove = new ArrayList<>();
        
        for (int i = 0; i < stmts.size(); i++) {
            Stmt stmt = stmts.get(i);
            
            if (stmt instanceof Nop) {
                // Check if this NOP is redundant
                if (isRedundantNop(stmt, i, jumpTargets)) {
                    toRemove.add(stmt);
                }
            }
        }
        
        // Remove redundant NOPs and update jump targets
        if (!toRemove.isEmpty()) {
            logger.debug("Removing {} redundant NOP statements", toRemove.size());
            
            // Update jump targets before removing NOPs
            for (Stmt nopToRemove : toRemove) {
                updateJumpTargetsForRemovedNop(nopToRemove);
            }
            
            // Remove the NOPs from the statement list
            stmts.removeAll(toRemove);
        }
    }
    
    /**
     * Finds all statements that are targets of jump instructions.
     */
    private Set<Stmt> findAllJumpTargets() {
        Set<Stmt> targets = new HashSet<>();
        
        for (Stmt stmt : stmts) {
            if (stmt instanceof Goto gotoStmt) {
                targets.add(gotoStmt.getTarget());
            } else if (stmt instanceof If ifStmt) {
                targets.add(ifStmt.getTarget());
            }
            // Add other jump statement types if needed
        }
        
        return targets;
    }
    
    /**
     * Determines if a NOP statement is redundant and can be removed.
     */
    private boolean isRedundantNop(Stmt nop, int nopIndex, Set<Stmt> jumpTargets) {
        // If no jumps target this NOP, it's redundant
        if (!jumpTargets.contains(nop)) {
            return true;
        }
        
        // If this NOP is at the end of the function, and something jumps to it,
        // we should keep it (it might be a valid exit point)
        if (nopIndex == stmts.size() - 1) {
            return false;
        }
        
        // If this NOP is followed by another NOP, we can eliminate this one
        // (jumps will be redirected to the next NOP)
        Stmt nextStmt = stmts.get(nopIndex + 1);
        if (nextStmt instanceof Nop) {
            return true;
        }
        
        // Advanced optimization: if this NOP is followed by a non-NOP statement,
        // we can redirect jumps to the next statement and remove this NOP
        // This is safe because the next statement will execute anyway
        return true;
    }
    
    /**
     * Updates all jump targets that point to a NOP being removed.
     */
    private void updateJumpTargetsForRemovedNop(Stmt nopToRemove) {
        int nopIndex = stmts.indexOf(nopToRemove);
        if (nopIndex == -1) return;
        
        // Find what this NOP should redirect to
        Stmt redirectTarget = null;
        if (nopIndex + 1 < stmts.size()) {
            redirectTarget = stmts.get(nopIndex + 1);
        }
        
        // Update all jumps that target this NOP
        for (Stmt stmt : stmts) {
            if (stmt instanceof Goto gotoStmt && gotoStmt.getTarget() == nopToRemove) {
                gotoStmt.setTarget(redirectTarget);
            } else if (stmt instanceof If ifStmt && ifStmt.getTarget() == nopToRemove) {
                ifStmt.setTarget(redirectTarget);
            }
        }
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



    /**
     * Optimizes all function IRs (placeholder for future optimizations).
     * Used by ToyCWorldBuilder.
     */
    public void optimizeAllFunctions() {
        logger.info("Optimizing {} functions", functions.size());
        // TODO: Implement IR optimizations
        for (Map.Entry<String, IR> entry : functions.entrySet()) {
            logger.debug("Optimizing function: {}", entry.getKey());
            // Apply optimizations to entry.getValue()
        }
    }
}