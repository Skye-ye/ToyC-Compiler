package toyc.frontend.ir;

import toyc.ToyCParser;
import toyc.ToyCParserBaseVisitor;
import toyc.World;
import toyc.ir.IR;
import toyc.ir.exp.*;
import toyc.ir.stmt.*;
import toyc.language.Function;
import toyc.language.Program;
import toyc.language.type.IntType;
import toyc.util.Timer;
import org.antlr.v4.runtime.ParserRuleContext;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import toyc.ir.DefaultIR;

import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

/**
 * IRBuilder converts the parsed AST into an Intermediate Representation (IR).
 * It handles scope management, control flow generation, and various optimizations.
 */
public class IRBuilder extends ToyCParserBaseVisitor<RValue> implements toyc.ir.IRBuilder {

    private static final Logger logger = LogManager.getLogger(IRBuilder.class);

    // ==================== Constants ====================
    private static final String TEMP_PREFIX = "temp$";
    private static final String CONST_PREFIX = "%intconst";
    private static final String PARAM_PREFIX = "param";
    private static final String VAR_SUFFIX_SEPARATOR = "_";

    // ==================== Core State ====================
    private final Map<String, IR> functions;
    private Function currentFunction;
    private final List<Stmt> stmts;

    // ==================== Variable Management ====================
    private int varCounter;
    private int tempCounter;
    private int constCounter;
    private final Stack<Map<String, Var>> scopeStack;
    private final Map<String, Integer> variableCounters; // For handling shadowing

    // ==================== Control Flow Management ====================
    private final Stack<Stmt> breakTargets;
    private final Stack<Stmt> continueTargets;

    public IRBuilder() {
        this.functions = new HashMap<>();
        this.stmts = new ArrayList<>();
        this.scopeStack = new Stack<>();
        this.variableCounters = new HashMap<>();
        this.breakTargets = new Stack<>();
        this.continueTargets = new Stack<>();
        resetCounters();
    }

    // ==================== Public API ====================

    @Override
    public void buildAll(Program program) {
        Timer timer = new Timer("Build IR for all functions");
        timer.start();

        buildAllInParallel(program);

        timer.stop();
        logger.info(timer);
    }

    @Override
    public IR buildIR(Function function) {
        initializeFunctionContext(function);

        List<Var> params = createParameters();
        enterScope();
        registerParameters(params);

        return createIR(params);
    }

    public Map<String, IR> getFunctions() {
        return functions;
    }

    // ==================== AST Visitors - Program Structure ====================

    @Override
    public RValue visitProgram(ToyCParser.ProgramContext ctx) {
        return visit(ctx.compUnit());
    }

    @Override
    public RValue visitCompUnit(ToyCParser.CompUnitContext ctx) {
        // Two-pass compilation for forward references
        buildAllFunctions(ctx);
        return null;
    }

    @Override
    public RValue visitFuncDef(ToyCParser.FuncDefContext ctx) {
        String funcName = ctx.funcName().IDENT().getText();
        currentFunction =
                World.get().getProgram().getFunction(funcName).orElse(null);
        if (currentFunction == null) {
            throw new RuntimeException("Undefined function: " + funcName);
        }

        initializeFunctionState();

        List<Var> params = createAndRegisterParameters(ctx);

        // Visit function body
        visit(ctx.block());

        // Handle implicit return for void functions
        ensureProperReturn(ctx);

        // Apply optimizations and finalize IR
        IR ir = finalizeIR(params);
        functions.put(funcName, ir);

        return null;
    }

    @Override
    public RValue visitBlock(ToyCParser.BlockContext ctx) {
        enterScope();

        for (ToyCParser.StmtContext stmt : ctx.stmt()) {
            visit(stmt);
        }

        exitScope();
        return null;
    }

    // ==================== AST Visitors - Statements ====================

    @Override
    public RValue visitStmt(ToyCParser.StmtContext ctx) {
        if (ctx.block() != null) {
            return visit(ctx.block());
        } else if (isAssignment(ctx)) {
            return handleAssignment(ctx);
        } else if (ctx.RETURN() != null) {
            return handleReturn(ctx);
        } else if (isExpressionStatement(ctx)) {
            return handleExpressionStatement(ctx);
        } else if (ctx.varDef() != null) {
            return visit(ctx.varDef());
        } else if (ctx.IF() != null) {
            return visitIfStatement(ctx);
        } else if (ctx.WHILE() != null) {
            return visitWhileStatement(ctx);
        } else if (ctx.BREAK() != null) {
            return handleBreak(ctx);
        } else if (ctx.CONTINUE() != null) {
            return handleContinue(ctx);
        }
        return null;
    }

    @Override
    public RValue visitVarDef(ToyCParser.VarDefContext ctx) {
        String originalName = ctx.IDENT().getText();
        String actualName = handleVariableShadowing(originalName);

        Var variable = createLocalVariable(actualName);
        defineVariable(originalName, variable);

        RValue initValue = visit(ctx.exp());
        generateAssignmentStatement(variable, initValue, ctx);

        return variable;
    }

    // ==================== AST Visitors - Expressions ====================

    @Override
    public RValue visitExp(ToyCParser.ExpContext ctx) {
        if (ctx.funcName() != null) {
            return visitFunctionCall(ctx);
        } else if (isParenthesizedExpression(ctx)) {
            return visit(ctx.exp(0));
        } else if (ctx.lVal() != null) {
            return visit(ctx.lVal());
        } else if (ctx.number() != null) {
            return visit(ctx.number());
        } else if (ctx.unaryOp() != null) {
            return visitUnaryOp(ctx);
        } else if (isBinaryOperation(ctx)) {
            return visitBinaryOp(ctx);
        }
        return null;
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
        return IntLiteral.get(ctx.MINUS() != null ? -value : value);
    }

    // ==================== Control Flow Handling ====================

    private RValue visitIfStatement(ToyCParser.StmtContext ctx) {
        RValue conditionExp = visit(ctx.exp());
        ConditionExp condition = createCondition(conditionExp, ctx);

        // Check for simple optimization case
        if (canOptimizeIfStatement(ctx)) {
            return generateOptimizedIf(condition, ctx);
        }

        return generateStandardIf(condition, ctx);
    }

    private RValue visitWhileStatement(ToyCParser.StmtContext ctx) {
        // Create control flow placeholders
        Stmt conditionLabel = createNopPlaceholder();
        Stmt bodyLabel = createNopPlaceholder();
        Stmt exitLabel = createNopPlaceholder();

        // Setup break/continue context
        pushLoopContext(exitLabel, conditionLabel);

        try {
            generateWhileLoop(ctx, conditionLabel, bodyLabel, exitLabel);
        } finally {
            popLoopContext();
        }

        return null;
    }

    // ==================== Expression Handling ====================

    private RValue visitFunctionCall(ToyCParser.ExpContext ctx) {
        String funcName = ctx.funcName().IDENT().getText();
        Function function = getFunctionOrError(funcName);

        List<Var> arguments = prepareCallArguments(ctx);

        return new CallExp(function, arguments);
    }

    private RValue visitUnaryOp(ToyCParser.ExpContext ctx) {
        String operator = ctx.unaryOp().getText();
        RValue operand = visit(ctx.exp(0));

        return switch (operator) {
            case "+" -> operand; // Unary plus is identity
            case "-" -> new NegExp(convertToVar(operand, ctx));
            case "!" -> new NotExp(convertToVar(operand, ctx));
            default -> throw new RuntimeException("Unknown unary operator: " + operator);
        };
    }

    private RValue visitBinaryOp(ToyCParser.ExpContext ctx) {
        // Handle short-circuit operators specially
        if (ctx.AND() != null) {
            return generateLogicalAnd(ctx);
        } else if (ctx.OR() != null) {
            return generateLogicalOr(ctx);
        }

        // Standard binary operations
        return generateBinaryOperation(ctx);
    }

    // ==================== Helper Methods - Scope Management ====================

    private void enterScope() {
        scopeStack.push(new HashMap<>());
    }

    private void exitScope() {
        if (!scopeStack.isEmpty()) {
            scopeStack.pop();
        }
    }

    private void defineVariable(String name, Var var) {
        if (!scopeStack.isEmpty()) {
            scopeStack.peek().put(name, var);
        }
    }

    private Var lookupVariable(String name) {
        // Search from innermost to outermost scope
        for (int i = scopeStack.size() - 1; i >= 0; i--) {
            Map<String, Var> scope = scopeStack.get(i);
            if (scope.containsKey(name)) {
                return scope.get(name);
            }
        }
        return null;
    }

    private String handleVariableShadowing(String originalName) {
        if (lookupVariable(originalName) != null) {
            return generateUniqueVariableName(originalName);
        }
        return originalName;
    }

    private String generateUniqueVariableName(String originalName) {
        int counter = variableCounters.getOrDefault(originalName, 0) + 1;
        variableCounters.put(originalName, counter);
        return originalName + VAR_SUFFIX_SEPARATOR + counter;
    }

    // ==================== Helper Methods - Variable Creation ====================

    private Var createTemp() {
        return new Var(currentFunction, TEMP_PREFIX + tempCounter++, IntType.INT, -1);
    }

    private Var createConstVar(IntLiteral literal) {
        return new Var(currentFunction, CONST_PREFIX + constCounter++, IntType.INT, -1, literal);
    }

    private Var createLocalVariable(String name) {
        return new Var(currentFunction, name, IntType.INT, varCounter++);
    }

    // ==================== Helper Methods - Statement Generation ====================

    private void generateAssignmentStatement(Var target, RValue source, ParserRuleContext ctx) {
        switch (source) {
            case Literal literal -> addStatement(new AssignLiteral(target, literal), ctx);
            case Var var -> addStatement(new Copy(target, var), ctx);
            case BinaryExp binaryExp -> addStatement(new Binary(target, binaryExp), ctx);
            case UnaryExp unaryExp -> addStatement(new Unary(target, unaryExp), ctx);
            case CallExp callExp -> addStatement(new Call(currentFunction, callExp, target), ctx);
            default -> throw new RuntimeException("Unknown RValue type: " + source.getClass().getSimpleName());
        }
    }

    private Var convertToVar(RValue exp, ParserRuleContext ctx) {
        if (exp instanceof Var var) {
            return var;
        }

        return switch (exp) {
            case IntLiteral literal -> {
                Var constVar = createConstVar(literal);
                addStatement(new AssignLiteral(constVar, literal), ctx);
                yield constVar;
            }
            case BinaryExp binaryExp -> {
                Var temp = createTemp();
                addStatement(new Binary(temp, binaryExp), ctx);
                yield temp;
            }
            case UnaryExp unaryExp -> {
                Var temp = createTemp();
                addStatement(new Unary(temp, unaryExp), ctx);
                yield temp;
            }
            case CallExp callExp -> {
                Var temp = createTemp();
                addStatement(new Call(currentFunction, callExp, temp), ctx);
                yield temp;
            }
            default -> throw new RuntimeException("Cannot convert to variable: " + exp.getClass().getSimpleName());
        };
    }

    // ==================== Helper Methods - Control Flow ====================

    private Stmt createNopPlaceholder() {
        return new Nop();
    }

    private ConditionExp createCondition(RValue conditionExp, ParserRuleContext ctx) {
        if (conditionExp instanceof ConditionExp cond) {
            return cond;
        }

        // Create condition: var != 0
        Var condVar = convertToVar(conditionExp, ctx);
        Var zeroVar = createZeroConstant(ctx);
        return new ConditionExp(ConditionExp.Op.NE, condVar, zeroVar);
    }

    private Var createZeroConstant(ParserRuleContext ctx) {
        IntLiteral zeroLiteral = IntLiteral.get(0);
        Var zeroVar = createConstVar(zeroLiteral);
        addStatement(new AssignLiteral(zeroVar, zeroLiteral), ctx);
        return zeroVar;
    }

    private void pushLoopContext(Stmt breakTarget, Stmt continueTarget) {
        breakTargets.push(breakTarget);
        continueTargets.push(continueTarget);
    }

    private void popLoopContext() {
        breakTargets.pop();
        continueTargets.pop();
    }

    // ==================== Helper Methods - Statement Addition ====================

    private void addStatement(Stmt stmt, ToyCParser.StmtContext ctx) {
        if (ctx != null && ctx.getStart() != null) {
            stmt.setLineNumber(ctx.getStart().getLine());
        }
        stmts.add(stmt);
    }

    private void addStatement(Stmt stmt, ParserRuleContext ctx) {
        if (ctx != null && ctx.getStart() != null) {
            stmt.setLineNumber(ctx.getStart().getLine());
        }
        stmts.add(stmt);
    }

    private void addStatement(Stmt stmt) {
        // Generated statements keep default -1 line number
        stmts.add(stmt);
    }

    // ==================== Optimization Methods ====================

    private void optimizeJumps() {
        boolean changed;
        do {
            changed = optimizeJumpTargets();
        } while (changed);
    }

    private boolean optimizeJumpTargets() {
        boolean changed = false;

        for (Stmt stmt : stmts) {
            if (stmt instanceof Goto gotoStmt) {
                changed |= optimizeGotoTarget(gotoStmt);
            } else if (stmt instanceof If ifStmt) {
                changed |= optimizeIfTarget(ifStmt);
            }
        }

        return changed;
    }

    private boolean optimizeGotoTarget(Goto gotoStmt) {
        Stmt currentTarget = gotoStmt.getTarget();
        Stmt optimizedTarget = findUltimateTarget(currentTarget);

        if (optimizedTarget != currentTarget) {
            gotoStmt.setTarget(optimizedTarget);
            return true;
        }

        return false;
    }

    private boolean optimizeIfTarget(If ifStmt) {
        Stmt currentTarget = ifStmt.getTarget();
        Stmt optimizedTarget = findUltimateTarget(currentTarget);

        if (optimizedTarget != currentTarget) {
            ifStmt.setTarget(optimizedTarget);
            return true;
        }

        return false;
    }

    private Stmt findUltimateTarget(Stmt target) {
        Set<Stmt> visited = new HashSet<>();
        Stmt current = target;

        while (current != null && !visited.contains(current)) {
            visited.add(current);

            if (current instanceof Nop) {
                current = getNextNonNopOrGotoTarget(current);
            } else if (current instanceof Goto gotoStmt) {
                current = gotoStmt.getTarget();
            } else {
                break;
            }
        }

        return current != null ? current : target;
    }

    private Stmt getNextNonNopOrGotoTarget(Stmt nop) {
        int index = stmts.indexOf(nop);
        if (index >= 0 && index + 1 < stmts.size()) {
            Stmt nextStmt = stmts.get(index + 1);
            if (nextStmt instanceof Goto gotoStmt) {
                return gotoStmt.getTarget();
            }
            return nextStmt;
        }
        return null;
    }

    private void eliminateRedundantNops() {
        Set<Stmt> jumpTargets = collectAllJumpTargets();
        List<Stmt> nopsToRemove = identifyRedundantNops(jumpTargets);

        if (!nopsToRemove.isEmpty()) {
            removeNops(nopsToRemove);
        }
    }

    private Set<Stmt> collectAllJumpTargets() {
        Set<Stmt> targets = new HashSet<>();

        for (Stmt stmt : stmts) {
            if (stmt instanceof Goto gotoStmt) {
                targets.add(gotoStmt.getTarget());
            } else if (stmt instanceof If ifStmt) {
                targets.add(ifStmt.getTarget());
            }
        }

        return targets;
    }

    private List<Stmt> identifyRedundantNops(Set<Stmt> jumpTargets) {
        List<Stmt> redundantNops = new ArrayList<>();

        for (int i = 0; i < stmts.size(); i++) {
            Stmt stmt = stmts.get(i);
            if (stmt instanceof Nop && isRedundantNop(stmt, i, jumpTargets)) {
                redundantNops.add(stmt);
            }
        }

        return redundantNops;
    }

    private boolean isRedundantNop(Stmt nop, int index, Set<Stmt> jumpTargets) {
        // Not targeted by any jump
        if (!jumpTargets.contains(nop)) {
            return true;
        }

        return index != stmts.size() - 1;
    }

    private void removeNops(List<Stmt> nopsToRemove) {
        logger.debug("Removing {} redundant NOP statements", nopsToRemove.size());

        for (Stmt nop : nopsToRemove) {
            redirectJumpsFromNop(nop);
        }

        stmts.removeAll(nopsToRemove);
    }

    private void redirectJumpsFromNop(Stmt nopToRemove) {
        int index = stmts.indexOf(nopToRemove);
        if (index == -1 || index + 1 >= stmts.size()) return;

        Stmt newTarget = stmts.get(index + 1);

        for (Stmt stmt : stmts) {
            if (stmt instanceof Goto gotoStmt && gotoStmt.getTarget() == nopToRemove) {
                gotoStmt.setTarget(newTarget);
            } else if (stmt instanceof If ifStmt && ifStmt.getTarget() == nopToRemove) {
                ifStmt.setTarget(newTarget);
            }
        }
    }

    // ==================== Complex Control Flow Generation ====================

    private RValue generateLogicalAnd(ToyCParser.ExpContext ctx) {
        Var resultVar = createTemp();

        // Evaluate left operand
        RValue leftExp = visit(ctx.exp(0));
        Var leftVar = convertToVar(leftExp, ctx);
        ConditionExp leftCondition = createConditionFromVar(leftVar, ctx);

        // Control flow labels
        Stmt setFalse = createNopPlaceholder();
        Stmt evaluateRight = createNopPlaceholder();
        Stmt setTrueResult = createNopPlaceholder();
        Stmt afterAnd = createNopPlaceholder();

        // if (left) goto evaluateRight; else goto setFalse
        generateConditionalJump(leftCondition, evaluateRight, setFalse, ctx);

        // Evaluate right operand
        addStatement(evaluateRight);
        RValue rightExp = visit(ctx.exp(1));
        Var rightVar = convertToVar(rightExp, ctx);
        ConditionExp rightCondition = createConditionFromVar(rightVar, ctx);

        // if (right) goto setTrueResult; else goto setFalse
        generateConditionalJump(rightCondition, setTrueResult, setFalse, ctx);

        // Set result = 1
        addStatement(setTrueResult);
        assignBooleanValue(resultVar, true, ctx);
        generateGoto(afterAnd, ctx);

        // Set result = 0
        addStatement(setFalse);
        assignBooleanValue(resultVar, false, ctx);

        addStatement(afterAnd);
        return resultVar;
    }

    private RValue generateLogicalOr(ToyCParser.ExpContext ctx) {
        Var resultVar = createTemp();

        // Evaluate left operand
        RValue leftExp = visit(ctx.exp(0));
        Var leftVar = convertToVar(leftExp, ctx);
        ConditionExp leftCondition = createConditionFromVar(leftVar, ctx);

        // Control flow labels
        Stmt setTrue = createNopPlaceholder();
        Stmt setFalse = createNopPlaceholder();
        Stmt afterOr = createNopPlaceholder();

        // if (left) goto setTrue; else evaluate right
        If ifLeftTrue = new If(leftCondition);
        ifLeftTrue.setTarget(setTrue);
        addStatement(ifLeftTrue, ctx);

        // Evaluate right operand
        RValue rightExp = visit(ctx.exp(1));
        Var rightVar = convertToVar(rightExp, ctx);
        ConditionExp rightCondition = createConditionFromVar(rightVar, ctx);

        // if (right) goto setTrue; else goto setFalse
        generateConditionalJump(rightCondition, setTrue, setFalse, ctx);

        // Set result = 1
        addStatement(setTrue);
        assignBooleanValue(resultVar, true, ctx);
        generateGoto(afterOr, ctx);

        // Set result = 0
        addStatement(setFalse);
        assignBooleanValue(resultVar, false, ctx);

        addStatement(afterOr);
        return resultVar;
    }

    private ConditionExp createConditionFromVar(Var var, ParserRuleContext ctx) {
        Var zeroVar = createZeroConstant(ctx);
        return new ConditionExp(ConditionExp.Op.NE, var, zeroVar);
    }

    private void generateConditionalJump(ConditionExp condition, Stmt trueTarget, Stmt falseTarget, ParserRuleContext ctx) {
        If ifStmt = new If(condition);
        ifStmt.setTarget(trueTarget);
        addStatement(ifStmt, ctx);

        Goto gotoFalse = new Goto();
        gotoFalse.setTarget(falseTarget);
        addStatement(gotoFalse, ctx);
    }

    private void generateGoto(Stmt target, ParserRuleContext ctx) {
        Goto gotoStmt = new Goto();
        gotoStmt.setTarget(target);
        addStatement(gotoStmt, ctx);
    }

    private void assignBooleanValue(Var target, boolean value, ParserRuleContext ctx) {
        IntLiteral literal = IntLiteral.get(value ? 1 : 0);
        Var constVar = createConstVar(literal);
        addStatement(new AssignLiteral(constVar, literal), ctx);
        addStatement(new Copy(target, constVar), ctx);
    }

    // ==================== Initialization and Finalization ====================

    private void initializeFunctionContext(Function function) {
        this.currentFunction = function;
        resetFunctionState();
    }

    private void initializeFunctionState() {
        resetFunctionState();
        enterScope();
    }

    private void resetFunctionState() {
        stmts.clear();
        scopeStack.clear();
        variableCounters.clear();
        resetCounters();
    }

    private void resetCounters() {
        varCounter = 0;
        tempCounter = 0;
        constCounter = 0;
    }

    private List<Var> createParameters() {
        List<Var> params = new ArrayList<>();
        for (int i = 0; i < currentFunction.getParamCount(); i++) {
            String paramName = currentFunction.getParamName(i);
            if (paramName == null) {
                paramName = PARAM_PREFIX + i;
            }
            Var param = new Var(currentFunction, paramName, currentFunction.getParamType(i), i);
            params.add(param);
        }
        return params;
    }

    private void registerParameters(List<Var> params) {
        for (Var param : params) {
            defineVariable(param.getName(), param);
        }
    }

    private List<Var> createAndRegisterParameters(ToyCParser.FuncDefContext ctx) {
        List<Var> params = new ArrayList<>();

        if (ctx.funcFParams() != null) {
            List<ToyCParser.FuncFParamContext> paramContexts = ctx.funcFParams().funcFParam();
            for (int i = 0; i < paramContexts.size(); i++) {
                String paramName = paramContexts.get(i).IDENT().getText();
                Var param = new Var(currentFunction, paramName, IntType.INT, i);
                params.add(param);
                defineVariable(paramName, param);
            }
        }

        return params;
    }

    private IR createIR(List<Var> params) {
        Set<Var> returnVars = new HashSet<>();
        List<Var> allVars = collectAllVariables(params);

        return new DefaultIR(currentFunction, params, returnVars, allVars, stmts);
    }

    private IR finalizeIR(List<Var> params) {
        // Apply optimizations
        optimizeJumps();
        eliminateRedundantNops();

        // Set statement indices
        assignStatementIndices();

        // Create final IR
        Set<Var> returnVars = new HashSet<>();
        List<Var> allVars = collectAllVariables(params);

        return new DefaultIR(currentFunction, params, returnVars, allVars, stmts);
    }

    private void assignStatementIndices() {
        for (int i = 0; i < stmts.size(); i++) {
            stmts.get(i).setIndex(i);
        }
    }

    private List<Var> collectAllVariables(List<Var> params) {
        List<Var> allVars = new ArrayList<>(params);

        // Add local variables (excluding parameters)
        Set<String> paramNames = new HashSet<>();
        for (Var param : params) {
            paramNames.add(param.getName());
        }

        for (Map<String, Var> scope : scopeStack) {
            for (Var var : scope.values()) {
                if (!paramNames.contains(var.getName())) {
                    allVars.add(var);
                }
            }
        }

        return allVars;
    }

    // ==================== Utility Methods ====================

    private void buildAllFunctions(ToyCParser.CompUnitContext ctx) {
        for (ToyCParser.FuncDefContext funcDef : ctx.funcDef()) {
            visit(funcDef);
        }
    }

    private void buildAllInParallel(Program program) {
        int nThreads = Runtime.getRuntime().availableProcessors();
        List<List<Function>> functionGroups = distributeFunctions(program.allFunctions().toList(), nThreads);

        ExecutorService executor = Executors.newFixedThreadPool(nThreads);

        for (List<Function> group : functionGroups) {
            executor.execute(() -> group.forEach(Function::getIR));
        }

        shutdownExecutor(executor);
    }

    private List<List<Function>> distributeFunctions(List<Function> functions, int nGroups) {
        List<List<Function>> groups = new ArrayList<>();
        for (int i = 0; i < nGroups; i++) {
            groups.add(new ArrayList<>());
        }

        for (int i = 0; i < functions.size(); i++) {
            groups.get(i % nGroups).add(functions.get(i));
        }

        return groups;
    }

    private void shutdownExecutor(ExecutorService executor) {
        executor.shutdown();
        try {
            if (!executor.awaitTermination(60, TimeUnit.SECONDS)) {
                executor.shutdownNow();
                if (!executor.awaitTermination(60, TimeUnit.SECONDS)) {
                    System.err.println("Executor did not terminate gracefully");
                }
            }
        } catch (InterruptedException e) {
            executor.shutdownNow();
            Thread.currentThread().interrupt();
        }
    }

    private Function getFunctionOrError(String funcName) {
        Function function =
                World.get().getProgram().getFunction(funcName).orElse(null);
        if (function == null) {
            throw new RuntimeException("Undefined function: " + funcName);
        }
        return function;
    }

    private List<Var> prepareCallArguments(ToyCParser.ExpContext ctx) {
        List<Var> arguments = new ArrayList<>();

        if (ctx.funcRParams() != null) {
            for (ToyCParser.FuncRParamContext param : ctx.funcRParams().funcRParam()) {
                RValue arg = visit(param.exp());
                Var argVar = convertToVar(arg, ctx);
                arguments.add(argVar);
            }
        }

        return arguments;
    }

    private void ensureProperReturn(ToyCParser.FuncDefContext ctx) {
        if (isVoidFunction(ctx) && !endsWithReturn()) {
            addStatement(new Return(null), ctx);
        }
    }

    private boolean endsWithReturn() {
        return !stmts.isEmpty() && stmts.getLast() instanceof Return;
    }

    private boolean isVoidFunction(ToyCParser.FuncDefContext ctx) {
        return ctx.funcType().VOID() != null;
    }

    // ==================== Statement Type Checks ====================

    private boolean isAssignment(ToyCParser.StmtContext ctx) {
        return ctx.lVal() != null && ctx.ASSIGN() != null;
    }

    private boolean isExpressionStatement(ToyCParser.StmtContext ctx) {
        return ctx.exp() != null && ctx.SEMICOLON() != null;
    }

    private boolean isParenthesizedExpression(ToyCParser.ExpContext ctx) {
        return ctx.L_PAREN() != null && ctx.exp(0) != null;
    }

    private boolean isBinaryOperation(ToyCParser.ExpContext ctx) {
        return ctx.exp().size() == 2;
    }

    private boolean canOptimizeIfStatement(ToyCParser.StmtContext ctx) {
        return isSimpleBreakOrContinue(ctx.stmt(0)) && ctx.stmt().size() == 1;
    }

    private boolean isSimpleBreakOrContinue(ToyCParser.StmtContext stmt) {
        if (stmt.BREAK() != null || stmt.CONTINUE() != null) {
            return true;
        }

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

        if (stmt.block() != null && !stmt.block().stmt().isEmpty()) {
            List<ToyCParser.StmtContext> stmts = stmt.block().stmt();
            return endsWithReturn(stmts.getLast());
        }

        return false;
    }

    // ==================== Specific Statement Handlers ====================

    private RValue handleAssignment(ToyCParser.StmtContext ctx) {
        Var target = (Var) visit(ctx.lVal());
        RValue source = visit(ctx.exp());
        generateAssignmentStatement(target, source, ctx);
        return null;
    }

    private RValue handleReturn(ToyCParser.StmtContext ctx) {
        Var returnValue = null;

        if (ctx.exp() != null) {
            RValue exp = visit(ctx.exp());
            returnValue = convertToVar(exp, ctx);
        }

        addStatement(new Return(returnValue), ctx);
        return null;
    }

    private RValue handleExpressionStatement(ToyCParser.StmtContext ctx) {
        RValue exp = visit(ctx.exp());

        if (exp instanceof CallExp callExp) {
            addStatement(new Call(currentFunction, callExp), ctx);
        }

        return null;
    }

    private RValue handleBreak(ToyCParser.StmtContext ctx) {
        if (breakTargets.isEmpty()) {
            throw new RuntimeException("break statement outside of loop");
        }

        Goto breakGoto = new Goto();
        breakGoto.setTarget(breakTargets.peek());
        addStatement(breakGoto, ctx);

        return null;
    }

    private RValue handleContinue(ToyCParser.StmtContext ctx) {
        if (continueTargets.isEmpty()) {
            throw new RuntimeException("continue statement outside of loop");
        }

        Goto continueGoto = new Goto();
        continueGoto.setTarget(continueTargets.peek());
        addStatement(continueGoto, ctx);

        return null;
    }

    private RValue generateOptimizedIf(ConditionExp condition, ToyCParser.StmtContext ctx) {
        If ifStmt = new If(condition);

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
        return null;
    }

    private RValue generateStandardIf(ConditionExp condition, ToyCParser.StmtContext ctx) {
        Stmt thenStart = createNopPlaceholder();
        Stmt afterIf = createNopPlaceholder();

        // if (condition) goto thenStart
        If ifStmt = new If(condition);
        ifStmt.setTarget(thenStart);
        addStatement(ifStmt, ctx);

        if (ctx.stmt().size() > 1) {
            // Has else clause
            generateIfElse(ctx, thenStart, afterIf);
        } else {
            // No else clause
            generateIfOnly(ctx, thenStart, afterIf);
        }

        return null;
    }

    private void generateIfElse(ToyCParser.StmtContext ctx, Stmt thenStart, Stmt afterIf) {
        boolean thenReturns = endsWithReturn(ctx.stmt(0));
        boolean elseReturns = endsWithReturn(ctx.stmt(1));

        Stmt elseStart = createNopPlaceholder();

        // goto else
        Goto gotoElse = new Goto();
        gotoElse.setTarget(elseStart);
        addStatement(gotoElse, ctx);

        // Then branch
        addStatement(thenStart);
        visit(ctx.stmt(0));

        if (!thenReturns) {
            generateGoto(afterIf, ctx);
        }

        // Else branch
        addStatement(elseStart);
        visit(ctx.stmt(1));

        // Add afterIf if needed
        if (!thenReturns || !elseReturns) {
            addStatement(afterIf);
        }
    }

    private void generateIfOnly(ToyCParser.StmtContext ctx, Stmt thenStart, Stmt afterIf) {
        // goto afterIf
        Goto gotoEnd = new Goto();
        gotoEnd.setTarget(afterIf);
        addStatement(gotoEnd, ctx);

        // Then branch
        addStatement(thenStart);
        visit(ctx.stmt(0));

        // Always add afterIf for single-branch if
        addStatement(afterIf);
    }

    private void generateWhileLoop(ToyCParser.StmtContext ctx, Stmt conditionStart, Stmt bodyStart, Stmt afterWhile) {
        // Condition evaluation
        addStatement(conditionStart);
        RValue conditionExp = visit(ctx.exp());
        ConditionExp condition = createCondition(conditionExp, ctx);

        // if (condition) goto bodyStart
        If ifStmt = new If(condition);
        ifStmt.setTarget(bodyStart);
        addStatement(ifStmt, ctx);

        // else goto afterWhile
        generateGoto(afterWhile, ctx);

        // Body
        addStatement(bodyStart);
        visit(ctx.stmt(0));

        // goto conditionStart
        generateGoto(conditionStart, ctx);

        // After while
        addStatement(afterWhile);
    }

    private RValue generateBinaryOperation(ToyCParser.ExpContext ctx) {
        RValue left = visit(ctx.exp(0));
        RValue right = visit(ctx.exp(1));

        Var leftVar = convertToVar(left, ctx);
        Var rightVar = convertToVar(right, ctx);

        // Arithmetic operations
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
        }

        // Comparison operations
        else if (ctx.LT() != null) {
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
        }

        throw new RuntimeException("Unknown binary operator");
    }
}