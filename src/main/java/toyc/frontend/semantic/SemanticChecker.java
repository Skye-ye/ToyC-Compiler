package toyc.frontend.semantic;

import java.util.List;
import java.util.ArrayList;
import java.util.Map;

import toyc.ToyCParser;
import toyc.ToyCParserBaseVisitor;
import toyc.language.type.FunctionType;
import toyc.language.type.IntType;
import toyc.language.type.Type;
import toyc.language.type.VoidType;
import toyc.frontend.semantic.symbol.SymbolTable;

public class SemanticChecker extends ToyCParserBaseVisitor<Type> {
    private SymbolTable curSymbolTable;
    private FunctionType curFuncType;
    private boolean hasError = false;
    private int whileDepth = 0;

    public boolean hasError() {
        return hasError;
    }

    @Override
    public Type visitProgram(ToyCParser.ProgramContext ctx) {
        // Create a global symbol table
        curSymbolTable = new SymbolTable(null);
        Type result = super.visitProgram(ctx);
        
        // Perform main function validation after all functions are processed
        validateMainFunction();
        
        return result;
    }

    @Override
    public Type visitFuncDef(ToyCParser.FuncDefContext ctx) {
        String funcName = ctx.funcName().IDENT().getText();
        if (curSymbolTable.find(funcName) != null) {
            OutputHelper.printTypeError(OutputHelper.ErrorType.REDEF_FUNC,
                    ctx.funcName().IDENT().getSymbol().getLine(), 
                    ctx.funcName().IDENT().getSymbol().getCharPositionInLine() + 1, 
                    funcName);
            hasError = true;
            return null;
        }

        // Determine the return type of the function
        Type returnType;
        if (ctx.funcType().INT() != null) {
            returnType = IntType.INT;
        } else {
            returnType = VoidType.VOID;
        }

        // Create sub symbol table to store parameters
        SymbolTable subSymbolTable = new SymbolTable(curSymbolTable);

        // Temporarily set the current symbol table to the sub symbol table and restore current one later
        curSymbolTable = subSymbolTable;

        List<Type> paramTypes = new ArrayList<>();
        if (ctx.funcFParams() != null) {
            visit(ctx.funcFParams());
            paramTypes = List.copyOf(curSymbolTable.getTypes());
        }

        curSymbolTable = curSymbolTable.getParent();
        FunctionType funcType = new FunctionType(paramTypes, returnType);
        curSymbolTable.define(funcName, funcType);
        curFuncType = funcType;

        curSymbolTable = subSymbolTable;
        visit(ctx.block());
        
        // Check if non-void function has returned on all paths
        if (returnType instanceof IntType && !hasReturnOnAllPaths(ctx.block())) {
            OutputHelper.printTypeError(OutputHelper.ErrorType.NON_VOID_FUNC_MISSING_RETURN,
                    ctx.funcName().IDENT().getSymbol().getLine(),
                    ctx.funcName().IDENT().getSymbol().getCharPositionInLine() + 1, funcName);
            hasError = true;
        }
        
        curSymbolTable = curSymbolTable.getParent();

        return null;
    }

    @Override
    public Type visitFuncFParams(ToyCParser.FuncFParamsContext ctx) {
        for (int i = 0; i < ctx.funcFParam().size(); i++) {
            visit(ctx.funcFParam(i));
        }
        return null;
    }

    @Override
    public Type visitFuncFParam(ToyCParser.FuncFParamContext ctx) {
        String varName = ctx.IDENT().getText();
        if (curSymbolTable.find(varName) != null) { // Duplicate parameter name
            OutputHelper.printTypeError(OutputHelper.ErrorType.REDEF_PARAM, 
                    ctx.IDENT().getSymbol().getLine(), 
                    ctx.IDENT().getSymbol().getCharPositionInLine() + 1, 
                    varName);
            hasError = true;
            return null;
        }

        if (ctx.INT() != null) {
            curSymbolTable.define(varName, IntType.INT);
        }

        return null;
    }

    @Override
    public Type visitBlock(ToyCParser.BlockContext ctx) {
        if (!(ctx.getParent() instanceof ToyCParser.FuncDefContext)) {
            // If the block is not within a function definition, create a new symbol table
            curSymbolTable = new SymbolTable(curSymbolTable);
            visitChildren(ctx);
            curSymbolTable = curSymbolTable.getParent();
        } else {
            // If the block is within in a function definition, visit the children directly
            visitChildren(ctx);
        }
        return null;
    }

    @Override
    public Type visitStmt(ToyCParser.StmtContext ctx) {
        if (ctx.ASSIGN() != null) { // Assign
            String lValName = ctx.lVal().IDENT().getText();
            Type lValType = visitLVal(ctx.lVal());
            if (lValType == null) {
                return null;
            }
            Type rvalType = visitExp(ctx.exp());
            if (rvalType == null) {
                return null;
            }
            if (lValType instanceof FunctionType) { // Function cannot be assigned
                OutputHelper.printTypeError(OutputHelper.ErrorType.NON_VAR_ASSIGN,
                        ctx.lVal().IDENT().getSymbol().getLine(), 
                        ctx.lVal().IDENT().getSymbol().getCharPositionInLine() + 1, 
                        lValName);
                hasError = true;
                return null;
            }

            if (!lValType.equals(rvalType)) { // Type mismatch
                OutputHelper.printTypeError(OutputHelper.ErrorType.TYPE_MISMATCH_ASSIGN,
                        ctx.ASSIGN().getSymbol().getLine(), 
                        ctx.ASSIGN().getSymbol().getCharPositionInLine() + 1);
                hasError = true;
                return null;
            }
            return null;
        } else if (ctx.RETURN() != null) { // Return
            Type returnType;
            if (ctx.exp() != null) {
                returnType = visitExp(ctx.exp());
            } else { // No expression followed by return, return void
                returnType = VoidType.VOID;
            }
            if (returnType == null) {
                return null;
            }
            Type funcReturnType = curFuncType.returnType();
            if (!returnType.equals(funcReturnType)) {
                OutputHelper.printTypeError(OutputHelper.ErrorType.TYPE_MISMATCH_RETURN,
                        ctx.RETURN().getSymbol().getLine(),
                        ctx.RETURN().getSymbol().getCharPositionInLine() + 1,
                        funcReturnType.toString());
                hasError = true;
            }
            return null;
        } else if (ctx.WHILE() != null) { // While loop
            whileDepth++;
            visitChildren(ctx);
            whileDepth--;
            return null;
        } else if (ctx.BREAK() != null) { // Break statement
            if (whileDepth == 0) {
                OutputHelper.printTypeError(OutputHelper.ErrorType.BREAK_OUTSIDE_WHILE,
                        ctx.BREAK().getSymbol().getLine(), 
                        ctx.BREAK().getSymbol().getCharPositionInLine() + 1);
                hasError = true;
            }
            return null;
        } else if (ctx.CONTINUE() != null) { // Continue statement
            if (whileDepth == 0) {
                OutputHelper.printTypeError(OutputHelper.ErrorType.CONTINUE_OUTSIDE_WHILE,
                        ctx.CONTINUE().getSymbol().getLine(), 
                        ctx.CONTINUE().getSymbol().getCharPositionInLine() + 1);
                hasError = true;
            }
            return null;
        }

        visitChildren(ctx);

        return null;
    }

    @Override
    public Type visitVarDef(ToyCParser.VarDefContext ctx) {
        String varName = ctx.IDENT().getText();
        if (curSymbolTable.find(varName) != null) {
            OutputHelper.printTypeError(OutputHelper.ErrorType.REDEF_VAR, ctx.IDENT().getSymbol().getLine(),
                    ctx.IDENT().getSymbol().getCharPositionInLine() + 1,
                    varName);
            hasError = true;
            return null;
        }

        Type type = visitExp(ctx.exp());
        if (type == null) {
            return null;
        }
        if (!(type instanceof IntType)) {
            OutputHelper.printTypeError(OutputHelper.ErrorType.TYPE_MISMATCH_ASSIGN,
                    ctx.ASSIGN().getSymbol().getLine(),
                    ctx.ASSIGN().getSymbol().getCharPositionInLine() + 1);
            hasError = true;
            return null;
        }
        curSymbolTable.define(varName, IntType.INT);
        return null;
    }

    @Override
    public Type visitExp(ToyCParser.ExpContext ctx) {
        if (ctx == null) {
            return null;
        }

        if (ctx.number() != null) { // Integer constant
            String numberText = ctx.number().INTEGER_CONST().getText();
            try {
                int value = Integer.parseInt(numberText);
                if (ctx.MUL() != null) {
                    Math.toIntExact(value);
                }
            } catch (NumberFormatException e) {
                OutputHelper.printTypeError(OutputHelper.ErrorType.INTEGER_OVERFLOW, 
                        ctx.number().INTEGER_CONST().getSymbol().getLine(), 
                        ctx.number().INTEGER_CONST().getSymbol().getCharPositionInLine() + 1);
                hasError = true;
                return null;
            }
            return IntType.INT;
        } else if (ctx.funcName() != null) { // Function call
            String funcName = ctx.funcName().IDENT().getText();
            Type type = curSymbolTable.resolve(funcName);
            if (type == null) {
                OutputHelper.printTypeError(OutputHelper.ErrorType.UNDEF_FUNC,
                        ctx.funcName().IDENT().getSymbol().getLine(),
                        ctx.funcName().IDENT().getSymbol().getCharPositionInLine() + 1,
                        funcName);
                hasError = true;
                return null;
            }

            if (!(type instanceof FunctionType)) {
                OutputHelper.printTypeError(OutputHelper.ErrorType.NON_FUNC_CALL,
                        ctx.funcName().IDENT().getSymbol().getLine(),
                        ctx.funcName().IDENT().getSymbol().getCharPositionInLine() + 1, funcName);
                hasError = true;
                return null;
            }

            if (!checkFuncArgs(ctx.funcRParams(), (FunctionType) type)) {
                OutputHelper.printTypeError(OutputHelper.ErrorType.ARGS_MISMATCH,
                        ctx.funcName().IDENT().getSymbol().getLine(),
                        ctx.funcName().IDENT().getSymbol().getCharPositionInLine() + 1, funcName);
                hasError = true;
                return null;
            }

            Type returnType = ((FunctionType) type).returnType();
            
            // Check if void function is used as rvalue (in conditions)
            if (returnType instanceof VoidType && isUsedAsRvalue(ctx)) {
                OutputHelper.printTypeError(OutputHelper.ErrorType.VOID_RETURN_FUNC_USE_AS_RVAL,
                        ctx.funcName().IDENT().getSymbol().getLine(),
                        ctx.funcName().IDENT().getSymbol().getCharPositionInLine() + 1, funcName);
                hasError = true;
                return null;
            }
            
            return returnType;
        } else if (ctx.exp().size() == 1) { // Unary operator or parentheses
            Type type = visitExp(ctx.exp(0));
            if (type == null) {
                return null;
            }
            if (ctx.unaryOp() != null && !(type instanceof IntType)) {
                OutputHelper.printTypeError(OutputHelper.ErrorType.TYPE_MISMATCH_OPERAND,
                        ctx.unaryOp().getStart().getLine(),
                        ctx.unaryOp().getStart().getCharPositionInLine() + 1,
                        ctx.unaryOp().getText());
                hasError = true;
                return null;
            }
            
            // Check for integer overflow in unary constant expressions
            if (ctx.unaryOp() != null && hasIntegerOverflow(ctx)) {
                OutputHelper.printTypeError(OutputHelper.ErrorType.INTEGER_OVERFLOW,
                        ctx.getStart().getLine(), 
                        ctx.getStart().getCharPositionInLine() + 1);
                hasError = true;
                return null;
            }
            
            return type;
        } else if (ctx.exp().size() == 2) { // Binary operator
            Type type1 = visitExp(ctx.exp(0));
            if (type1 == null) {
                return null;
            }
            Type type2 = visitExp(ctx.exp(1));
            if (type2 == null) {
                return null;
            }
            if (!(type1 instanceof IntType) || !(type2 instanceof IntType)) {
                OutputHelper.printTypeError(OutputHelper.ErrorType.TYPE_MISMATCH_OPERAND,
                        ctx.getStart().getLine(),
                        ctx.getChild(1).getText());
                hasError = true;
                return null;
            }
            
            // Check for division by zero
            if (ctx.DIV() != null && isZeroConstant(ctx.exp(1))) {
                OutputHelper.printTypeError(OutputHelper.ErrorType.ZERO_DIVISION,
                        ctx.DIV().getSymbol().getLine(), 
                        ctx.DIV().getSymbol().getCharPositionInLine() + 1);
                hasError = true;
                return null;
            }
            
            // Check for integer overflow in constant expressions
            if (hasIntegerOverflow(ctx)) {
                OutputHelper.printTypeError(OutputHelper.ErrorType.INTEGER_OVERFLOW,
                        ctx.getStart().getLine(), 
                        ctx.getStart().getCharPositionInLine() + 1);
                hasError = true;
                return null;
            }
            
            return IntType.INT;
        } else { // lVal
            ToyCParser.LValContext lValCtx = ctx.lVal();
            return visitLVal(lValCtx);
        }
    }

    @Override
    public Type visitLVal(ToyCParser.LValContext ctx) {
        if (ctx == null) {
            return null;
        }
        String varName = ctx.IDENT().getText();
        Type type = curSymbolTable.resolve(varName);
        if (type == null) {
            OutputHelper.printTypeError(OutputHelper.ErrorType.UNDEF_VAR,
                    ctx.IDENT().getSymbol().getLine(),
                    ctx.IDENT().getSymbol().getCharPositionInLine() + 1,
                    varName);
            hasError = true;
            return null;
        }
        return type;
    }

    private boolean checkFuncArgs(ToyCParser.FuncRParamsContext ctx, FunctionType funcType) {
        if (funcType == null) {
            return false;
        }

        if (ctx == null) { // no arguments
            return funcType.parameterTypes().isEmpty();
        }

        List<Type> argTypes = new ArrayList<>();
        for (int i = 0; i < ctx.funcRParam().size(); i++) {
            Type type = visitExp(ctx.funcRParam(i).exp());
            if (type == null) {
                return false;
            }
            argTypes.add(type);
        }

        return funcType.checkArguments(argTypes);
    }

    private boolean isZeroConstant(ToyCParser.ExpContext ctx) {
        if (ctx == null) {
            return false;
        }
        
        // Direct number constant
        if (ctx.number() != null) {
            String text = ctx.number().INTEGER_CONST().getText();
            try {
                return Integer.parseInt(text) == 0;
            } catch (NumberFormatException e) {
                return false;
            }
        }
        
        // Parenthesized expression
        if (ctx.exp() != null && ctx.exp().size() == 1 && ctx.L_PAREN() != null) {
            return isZeroConstant(ctx.exp(0));
        }
        
        // Binary expressions that can be evaluated at compile time
        if (ctx.exp() != null && ctx.exp().size() == 2) {
            Integer left = evaluateConstantExpression(ctx.exp(0));
            Integer right = evaluateConstantExpression(ctx.exp(1));
            
            if (left != null && right != null) {
                if (ctx.MINUS() != null) {
                    return (left - right) == 0;
                } else if (ctx.PLUS() != null) {
                    return (left + right) == 0;
                } else if (ctx.MUL() != null) {
                    return (left * right) == 0;
                } else if (ctx.DIV() != null && right != 0) {
                    return (left / right) == 0;
                } else if (ctx.MOD() != null && right != 0) {
                    return (left % right) == 0;
                }
            }
        }
        
        // Unary expressions
        if (ctx.exp() != null && ctx.exp().size() == 1 && ctx.unaryOp() != null) {
            Integer operand = evaluateConstantExpression(ctx.exp(0));
            if (operand != null) {
                if (ctx.unaryOp().MINUS() != null) {
                    return (-operand) == 0;
                } else if (ctx.unaryOp().PLUS() != null) {
                    return operand == 0;
                }
            }
        }
        
        return false;
    }
    
    private Integer evaluateConstantExpression(ToyCParser.ExpContext ctx) {
        if (ctx == null) {
            return null;
        }
        
        // Direct number constant
        if (ctx.number() != null) {
            String text = ctx.number().INTEGER_CONST().getText();
            try {
                int value = Integer.parseInt(text);
                if (ctx.MINUS() != null) {
                    return Math.negateExact(value);
                }else {
                    return value;
                }
            } catch (NumberFormatException e) {
                return null;
            }
        }
        
        // Parenthesized expression
        if (ctx.exp() != null && ctx.exp().size() == 1 && ctx.L_PAREN() != null) {
            return evaluateConstantExpression(ctx.exp(0));
        }
        
        // Binary expressions
        if (ctx.exp() != null && ctx.exp().size() == 2) {
            Integer left = evaluateConstantExpression(ctx.exp(0));
            Integer right = evaluateConstantExpression(ctx.exp(1));
            
            if (left != null && right != null) {
                try {
                    if (ctx.MINUS() != null) {
                        return Math.subtractExact(left, right);
                    } else if (ctx.PLUS() != null) {
                        return Math.addExact(left, right);
                    } else if (ctx.MUL() != null) {
                        return Math.multiplyExact(left, right);
                    } else if (ctx.DIV() != null && right != 0) {
                        return left / right;
                    } else if (ctx.MOD() != null && right != 0) {
                        return left % right;
                    }
                } catch (ArithmeticException e) {
                    return null;
                }
            }
        }
        
        // Unary expressions
        if (ctx.exp() != null && ctx.exp().size() == 1 && ctx.unaryOp() != null) {
            Integer operand = evaluateConstantExpression(ctx.exp(0));
            if (operand != null) {
                try {
                    if (ctx.unaryOp().MINUS() != null) {
                        return Math.negateExact(operand);
                    } else if (ctx.unaryOp().PLUS() != null) {
                        return operand;
                    } else if (ctx.unaryOp().NOT() != null) {
                        return operand == 0 ? 1 : 0;
                    }
                } catch (ArithmeticException e) {
                    return null;
                }
            }
        }
        
        return null;
    }
    
    private boolean hasIntegerOverflow(ToyCParser.ExpContext ctx) {
        if (ctx == null) {
            return false;
        }
        
        // Direct number constant - already checked in visitExp for number()
        if (ctx.number() != null) {
            return false;
        }
        
        // Parenthesized expression
        if (ctx.exp() != null && ctx.exp().size() == 1 && ctx.L_PAREN() != null) {
            return hasIntegerOverflow(ctx.exp(0));
        }
        
        // Binary expressions
        if (ctx.exp() != null && ctx.exp().size() == 2) {
            Integer left = evaluateConstantExpression(ctx.exp(0));
            Integer right = evaluateConstantExpression(ctx.exp(1));
            
            if (left != null && right != null) {
                try {
                    if (ctx.MINUS() != null) {
                        Math.subtractExact(left, right);
                    } else if (ctx.PLUS() != null) {
                        Math.addExact(left, right);
                    } else if (ctx.MUL() != null) {
                        Math.multiplyExact(left, right);
                    } else if (ctx.DIV() != null && right != 0) {
                        // Division doesn't overflow for integers
                        return false;
                    } else if (ctx.MOD() != null && right != 0) {
                        // Modulo doesn't overflow for integers
                        return false;
                    }
                    return false;
                } catch (ArithmeticException e) {
                    return true;
                }
            }
            
            // Check recursively for overflow in subexpressions
            return hasIntegerOverflow(ctx.exp(0)) || hasIntegerOverflow(ctx.exp(1));
        }
        
        // Unary expressions
        if (ctx.exp() != null && ctx.exp().size() == 1 && ctx.unaryOp() != null) {
            Integer operand = evaluateConstantExpression(ctx.exp(0));
            if (operand != null) {
                try {
                    if (ctx.unaryOp().MINUS() != null) {
                        Math.negateExact(operand);
                    }
                    return false;
                } catch (ArithmeticException e) {
                    return true;
                }
            }
            
            // Check recursively for overflow in subexpression
            return hasIntegerOverflow(ctx.exp(0));
        }
        
        return false;
    }

    private boolean isUsedAsRvalue(ToyCParser.ExpContext ctx) {
        if (ctx.getParent() instanceof ToyCParser.StmtContext parent) {
            // Check if used in while condition, if condition, or assignment RHS
            return parent.WHILE() != null || parent.IF() != null || parent.ASSIGN() != null;
        }
        return false;
    }

    private boolean hasReturnOnAllPaths(ToyCParser.BlockContext block) {
        if (block == null || block.stmt() == null) {
            return false;
        }
        
        for (ToyCParser.StmtContext stmt : block.stmt()) {
            if (hasReturnInStmt(stmt)) {
                return true;
            }
        }
        return false;
    }

    private boolean hasReturnInStmt(ToyCParser.StmtContext stmt) {
        if (stmt.RETURN() != null) {
            return true;
        }
        
        if (stmt.IF() != null) {
            // For if statement, both branches must have return
            List<ToyCParser.StmtContext> stmts = stmt.stmt();
            if (stmts.size() == 2) { // if-else
                return hasReturnInStmt(stmts.get(0)) && hasReturnInStmt(stmts.get(1));
            }
            return false; // if without else cannot guarantee return
        }
        
        if (stmt.block() != null) {
            return hasReturnOnAllPaths(stmt.block());
        }
        
        return false;
    }
    
    private void validateMainFunction() {
        // Check if the main function exists
        if (!checkMainExists()) {
            hasError = true;
        } else {
            // Proceed with further checks
            checkMainParams();
            checkMainReturnType();
        }
    }

    private boolean checkMainExists() {
        if (curSymbolTable.find("main") == null) {
            OutputHelper.printTypeError(OutputHelper.ErrorType.UNDEF_MAIN);
            hasError = true;
            return false;
        }
        return true;
    }

    private void checkMainReturnType() {
        FunctionType mainFuncType = (FunctionType) curSymbolTable.find("main");
        if (mainFuncType == null) {
            OutputHelper.printTypeError(OutputHelper.ErrorType.UNDEF_MAIN);
            hasError = true;
            return;
        }

        if (!(mainFuncType.returnType() instanceof IntType)) {
            OutputHelper.printTypeError(OutputHelper.ErrorType.MAIN_RETURN_NON_INT_TYPE);
            hasError = true;
        }
    }

    private void checkMainParams() {
        FunctionType mainFuncType = (FunctionType) curSymbolTable.find("main");
        if (mainFuncType == null) {
            OutputHelper.printTypeError(OutputHelper.ErrorType.UNDEF_MAIN);
            hasError = true;
            return;
        }

        if (!mainFuncType.parameterTypes().isEmpty()) {
            OutputHelper.printTypeError(OutputHelper.ErrorType.MAIN_NON_EMPTY_PARAM);
            hasError = true;
        }
    }
}