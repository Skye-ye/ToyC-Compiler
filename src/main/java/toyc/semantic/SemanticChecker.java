package toyc.semantic;

import java.util.List;
import java.util.ArrayList;
import toyc.ToyCParser;
import toyc.ToyCParserBaseVisitor;
import toyc.symbol.SymbolTable;
import toyc.type.*;

public class SemanticChecker extends ToyCParserBaseVisitor<Type> {
    private SymbolTable curSymbolTable;
    private FunctionType curFuncType;
    private boolean hasError = false;

    public boolean hasError() {
        return hasError;
    }

    @Override
    public Type visitProgram(ToyCParser.ProgramContext ctx) {
        // Create a global symbol table
        curSymbolTable = new SymbolTable(null);
        return super.visitProgram(ctx);
    }

    @Override
    public Type visitFuncDef(ToyCParser.FuncDefContext ctx) {
        String funcName = ctx.funcName().IDENT().getText();
        if (curSymbolTable.find(funcName) != null) {
            OutputHelper.printTypeError(OutputHelper.ErrorType.REDEF_FUNC, ctx.funcName().IDENT().getSymbol().getLine(), funcName);
            hasError = true;
            return null;
        }

        // Determine the return type of the function
        Type returnType;
        if (ctx.funcType().INT() != null) {
            returnType = IntType.getIntType();
        } else {
            returnType = VoidType.getVoidType();
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
        FunctionType funcType = new FunctionType(returnType, paramTypes);
        curSymbolTable.define(funcName, funcType);
        curFuncType = funcType;

        curSymbolTable = subSymbolTable;
        visit(ctx.block());
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
            OutputHelper.printTypeError(OutputHelper.ErrorType.REDEF_PARAM, ctx.IDENT().getSymbol().getLine(), varName);
            hasError = true;
            return null;
        }

        if (ctx.INT() != null) {
            curSymbolTable.define(varName, IntType.getIntType());
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
            String lvalName = ctx.lVal().IDENT().getText();
            Type lvalType = visitLVal(ctx.lVal());
            if (lvalType == null) {
                return null;
            }
            Type rvalType = visitExp(ctx.exp());
            if (rvalType == null) {
                return null;
            }
            if (lvalType instanceof FunctionType) { // Function cannot be assigned
                OutputHelper.printTypeError(OutputHelper.ErrorType.NON_VAR_ASSIGN,
                        ctx.lVal().IDENT().getSymbol().getLine(), lvalName);
                hasError = true;
                return null;
            }

            if (!lvalType.equals(rvalType)) { // Type mismatch
                OutputHelper.printTypeError(OutputHelper.ErrorType.TYPE_MISMATCH_ASSIGN,
                        ctx.ASSIGN().getSymbol().getLine());
                hasError = true;
                return null;
            }
            return null;
        } else if (ctx.RETURN() != null) { // Return
            Type returnType;
            if (ctx.exp() != null) {
                returnType = visitExp(ctx.exp());
            } else { // No expression followed by return, return void
                returnType = VoidType.getVoidType();
            }
            if (returnType == null) {
                return null;
            }
            Type funcReturnType = curFuncType.getReturnType();
            if (!returnType.equals(funcReturnType)) {
                OutputHelper.printTypeError(OutputHelper.ErrorType.TYPE_MISMATCH_RETURN,
                        ctx.RETURN().getSymbol().getLine(),
                        funcReturnType.toString());
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
            OutputHelper.printTypeError(OutputHelper.ErrorType.REDEF_VAR, ctx.IDENT().getSymbol().getLine(), varName);
            hasError = true;
            return null;
        }

        Type type = visitExp(ctx.exp());
        if (type == null) {
            return null;
        }
        if (!(type instanceof IntType)) {
            OutputHelper.printTypeError(OutputHelper.ErrorType.TYPE_MISMATCH_ASSIGN,
                    ctx.ASSIGN().getSymbol().getLine());
            hasError = true;
            return null;
        }
        curSymbolTable.define(varName, IntType.getIntType());
        return null;
    }

    @Override
    public Type visitExp(ToyCParser.ExpContext ctx) {
        if (ctx == null) {
            return null;
        }

        if (ctx.number() != null) { // Integer constant
            return IntType.getIntType();
        } else if (ctx.funcName() != null) { // Function call
            String funcName = ctx.funcName().IDENT().getText();
            Type type = curSymbolTable.resolve(funcName);
            if (type == null) {
                OutputHelper.printTypeError(OutputHelper.ErrorType.UNDEF_FUNC, ctx.funcName().IDENT().getSymbol().getLine(), funcName);
                hasError = true;
                return null;
            }

            if (!(type instanceof FunctionType)) {
                OutputHelper.printTypeError(OutputHelper.ErrorType.NON_FUNC_CALL, ctx.funcName().IDENT().getSymbol().getLine(), funcName);
                hasError = true;
                return null;
            }

            if (!checkFuncArgs(ctx.funcRParams(), (FunctionType) type)) {
                OutputHelper.printTypeError(OutputHelper.ErrorType.ARGS_MISMATCH,
                        ctx.funcName().IDENT().getSymbol().getLine(), funcName);
                hasError = true;
                return null;
            }

            return ((FunctionType) type).getReturnType();
        } else if (ctx.exp().size() == 1) { // Unary operator or parentheses
            Type type = visitExp(ctx.exp(0));
            if (type == null) {
                return null;
            }
            if (ctx.unaryOp() != null && !(type instanceof IntType)) {
                OutputHelper.printTypeError(OutputHelper.ErrorType.TYPE_MISMATCH_OPERAND,
                        ctx.unaryOp().getStart().getLine(), ctx.unaryOp().getText());
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
                        ctx.getStart().getLine(), ctx.getChild(1).getText());
                hasError = true;
                return null;
            }
            return IntType.getIntType();
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
            OutputHelper.printTypeError(OutputHelper.ErrorType.UNDEF_VAR, ctx.IDENT().getSymbol().getLine(),
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
            return funcType.getArity() == 0;
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
}