package toyc.ir;

import toyc.ToyCParser;
import toyc.ToyCParserBaseVisitor;
import toyc.ir.instruction.*;
import toyc.ir.value.Constant;
import toyc.ir.value.Temporary;
import toyc.ir.value.Value;
import toyc.ir.value.Variable;

import java.util.*;

public class IRBuilder extends ToyCParserBaseVisitor<Value> {
    private final Map<String, ControlFlowGraph> functions;
    private ControlFlowGraph currentCFG;
    private BasicBlock currentBlock;
    private final Map<String, Variable> variables;
    private final Stack<Label> breakLabels;
    private final Stack<Label> continueLabels;
    private final Stack<BasicBlock> whileEndBlocks;
    private final Map<String, Boolean> functionReturnTypes; // true for void, false for int
    private int whileCounter;
    private int ifCounter;
    
    public IRBuilder() {
        this.functions = new HashMap<>();
        this.variables = new HashMap<>();
        this.breakLabels = new Stack<>();
        this.continueLabels = new Stack<>();
        this.whileEndBlocks = new Stack<>();
        this.functionReturnTypes = new HashMap<>();
        this.whileCounter = 0;
        this.ifCounter = 0;
    }
    
    public Map<String, ControlFlowGraph> getFunctions() {
        return functions;
    }
    
    @Override
    public Value visitProgram(ToyCParser.ProgramContext ctx) {
        return visit(ctx.compUnit());
    }
    
    @Override
    public Value visitCompUnit(ToyCParser.CompUnitContext ctx) {
        for (ToyCParser.FuncDefContext funcDef : ctx.funcDef()) {
            visit(funcDef);
        }
        return null;
    }
    
    @Override
    public Value visitFuncDef(ToyCParser.FuncDefContext ctx) {
        String funcName = ctx.funcName().getText();
        currentCFG = new ControlFlowGraph(funcName);
        functions.put(funcName, currentCFG);
        
        // Record function return type
        boolean isVoid = ctx.funcType().VOID() != null;
        functionReturnTypes.put(funcName, isVoid);
        
        BasicBlock entryBlock = new BasicBlock(funcName);
        currentCFG.setEntryBlock(entryBlock);
        currentCFG.addBlock(entryBlock);
        currentBlock = entryBlock;
        
        BasicBlock exitBlock = new BasicBlock(funcName + "_exit");
        currentCFG.setExitBlock(exitBlock);
        currentCFG.addBlock(exitBlock);
        
        variables.clear();
        whileCounter = 0;
        ifCounter = 0;
        
        if (ctx.funcFParams() != null) {
            for (ToyCParser.FuncFParamContext param : ctx.funcFParams().funcFParam()) {
                String paramName = param.IDENT().getText();
                Variable paramVar = new Variable(paramName);
                variables.put(paramName, paramVar);
            }
        }
        
        visit(ctx.block());
        
        if (currentBlock != null) {
            Instruction lastInstr = currentBlock.getLastInstruction();
            if (!(lastInstr instanceof ReturnInstruction)) {
                if (ctx.funcType().VOID() != null) {
                    currentBlock.addInstruction(new ReturnInstruction(null));
                }
            }
            currentBlock.addSuccessor(exitBlock);
        }
        
        return null;
    }
    
    @Override
    public Value visitBlock(ToyCParser.BlockContext ctx) {
        for (ToyCParser.StmtContext stmt : ctx.stmt()) {
            if (currentBlock == null) break;
            visit(stmt);
        }
        return null;
    }
    
    @Override
    public Value visitStmt(ToyCParser.StmtContext ctx) {
        if (currentBlock == null) return null;
        
        if (ctx.block() != null) {
            visit(ctx.block());
        } else if (ctx.lVal() != null && ctx.ASSIGN() != null) {
            Variable target = (Variable) visit(ctx.lVal());
            Value source = visit(ctx.exp());
            currentBlock.addInstruction(new AssignInstruction(target, source));
        } else if (ctx.RETURN() != null) {
            Value returnValue = ctx.exp() != null ? visit(ctx.exp()) : null;
            currentBlock.addInstruction(new ReturnInstruction(returnValue));
            BasicBlock exitBlock = currentCFG.getExitBlock();
            if (exitBlock != null) {
                currentBlock.addSuccessor(exitBlock);
            }
            currentBlock = null;
        } else if (ctx.exp() != null && ctx.SEMICOLON() != null) {
            visit(ctx.exp());
        } else if (ctx.varDef() != null) {
            visit(ctx.varDef());
        } else if (ctx.IF() != null) {
            visitIfStatement(ctx);
        } else if (ctx.WHILE() != null) {
            visitWhileStatement(ctx);
        } else if (ctx.BREAK() != null) {
            if (!breakLabels.isEmpty()) {
                Label breakLabel = breakLabels.peek();
                currentBlock.addInstruction(new JumpInstruction(breakLabel));
                BasicBlock nextBlock = whileEndBlocks.peek();
                if (nextBlock != null) {
                    currentBlock.addSuccessor(nextBlock);
                }
                currentBlock = null;
            }
        } else if (ctx.CONTINUE() != null) {
            if (!continueLabels.isEmpty()) {
                Label continueLabel = continueLabels.peek();
                currentBlock.addInstruction(new JumpInstruction(continueLabel));
                // Find the condition block to add as successor
                BasicBlock condBlock = currentCFG.getBlockByLabel(continueLabel);
                if (condBlock != null) {
                    currentBlock.addSuccessor(condBlock);
                }
                currentBlock = null;
            }
        }
        
        return null;
    }
    
    private void visitIfStatement(ToyCParser.StmtContext ctx) {
        Value condition = visit(ctx.exp());
        int currentIf = ifCounter++;
        
        Label thenLabel = new Label("if_true" + currentIf);
        Label endLabel = new Label("if_end" + currentIf);
        
        BasicBlock endBlock = new BasicBlock("if_end" + currentIf);
        endBlock.setLabel(endLabel);
        currentCFG.addBlock(endBlock);
        currentCFG.mapLabelToBlock(endLabel, endBlock);
        
        if (ctx.stmt().size() > 1) { // Has else clause
            Label elseLabel = new Label("if_false" + currentIf);
            
            currentBlock.addInstruction(new ConditionalJumpInstruction(condition, thenLabel));
            currentBlock.addInstruction(new JumpInstruction(elseLabel));
            
            BasicBlock thenBlock = new BasicBlock("if_true" + currentIf);
            thenBlock.setLabel(thenLabel);
            currentCFG.addBlock(thenBlock);
            currentCFG.mapLabelToBlock(thenLabel, thenBlock);
            currentBlock.addSuccessor(thenBlock);
            
            BasicBlock elseBlock = new BasicBlock("if_false" + currentIf);
            elseBlock.setLabel(elseLabel);
            currentCFG.addBlock(elseBlock);
            currentCFG.mapLabelToBlock(elseLabel, elseBlock);
            currentBlock.addSuccessor(elseBlock);
            
            // Process then block
            currentBlock = thenBlock;
            visit(ctx.stmt(0));
            BasicBlock afterThen = currentBlock;
            
            // Process else block
            currentBlock = elseBlock;
            visit(ctx.stmt(1));
            BasicBlock afterElse = currentBlock;
            
            // Connect both branches to end block
            if (afterThen != null && !hasTerminator(afterThen)) {
                afterThen.addInstruction(new JumpInstruction(endLabel));
                afterThen.addSuccessor(endBlock);
            }
            
            if (afterElse != null && !hasTerminator(afterElse)) {
                afterElse.addInstruction(new JumpInstruction(endLabel));
                afterElse.addSuccessor(endBlock);
            }
        } else { // No else clause
            currentBlock.addInstruction(new ConditionalJumpInstruction(condition, thenLabel));
            currentBlock.addInstruction(new JumpInstruction(endLabel));
            
            BasicBlock thenBlock = new BasicBlock("if_true" + currentIf);
            thenBlock.setLabel(thenLabel);
            currentCFG.addBlock(thenBlock);
            currentCFG.mapLabelToBlock(thenLabel, thenBlock);
            currentBlock.addSuccessor(thenBlock);
            currentBlock.addSuccessor(endBlock);
            
            // Process then block
            currentBlock = thenBlock;
            visit(ctx.stmt(0));
            BasicBlock afterThen = currentBlock;
            
            // Connect then branch to end block
            if (afterThen != null && !hasTerminator(afterThen)) {
                afterThen.addInstruction(new JumpInstruction(endLabel));
                afterThen.addSuccessor(endBlock);
            }
        }
        
        currentBlock = endBlock;
    }
    
    private boolean hasTerminator(BasicBlock block) {
        if (block.isEmpty()) return false;
        Instruction lastInstr = block.getLastInstruction();
        return lastInstr instanceof ReturnInstruction || 
               lastInstr instanceof JumpInstruction || 
               lastInstr instanceof ConditionalJumpInstruction;
    }
    
    private void visitWhileStatement(ToyCParser.StmtContext ctx) {
        int currentWhile = whileCounter++;
        
        Label condLabel = new Label("while_cond" + currentWhile);
        Label bodyLabel = new Label("while_body" + currentWhile);
        Label endLabel = new Label("while_end" + currentWhile);
        
        BasicBlock condBlock = new BasicBlock("while_cond" + currentWhile);
        condBlock.setLabel(condLabel);
        currentCFG.addBlock(condBlock);
        currentCFG.mapLabelToBlock(condLabel, condBlock);
        
        BasicBlock bodyBlock = new BasicBlock("while_body" + currentWhile);
        bodyBlock.setLabel(bodyLabel);
        currentCFG.addBlock(bodyBlock);
        currentCFG.mapLabelToBlock(bodyLabel, bodyBlock);
        
        BasicBlock endBlock = new BasicBlock("while_end" + currentWhile);
        endBlock.setLabel(endLabel);
        currentCFG.addBlock(endBlock);
        currentCFG.mapLabelToBlock(endLabel, endBlock);
        
        currentBlock.addInstruction(new JumpInstruction(condLabel));
        currentBlock.addSuccessor(condBlock);
        
        currentBlock = condBlock;
        Value condition = visit(ctx.exp());
        currentBlock.addInstruction(new ConditionalJumpInstruction(condition, bodyLabel));
        currentBlock.addInstruction(new JumpInstruction(endLabel));
        currentBlock.addSuccessor(bodyBlock);
        currentBlock.addSuccessor(endBlock);
        
        breakLabels.push(endLabel);
        continueLabels.push(condLabel);
        whileEndBlocks.push(endBlock);
        
        currentBlock = bodyBlock;
        visit(ctx.stmt(0));
        
        if (currentBlock != null && !hasTerminator(currentBlock)) {
            currentBlock.addInstruction(new JumpInstruction(condLabel));
            currentBlock.addSuccessor(condBlock);
        }
        
        breakLabels.pop();
        continueLabels.pop();
        whileEndBlocks.pop();
        
        currentBlock = endBlock;
    }
    
    @Override
    public Value visitVarDef(ToyCParser.VarDefContext ctx) {
        String varName = ctx.IDENT().getText();
        Variable var = new Variable(varName);
        variables.put(varName, var);
        
        Value initValue = visit(ctx.exp());
        currentBlock.addInstruction(new AssignInstruction(var, initValue));
        
        return var;
    }
    
    @Override
    public Value visitExp(ToyCParser.ExpContext ctx) {
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
    
    private Value visitFunctionCall(ToyCParser.ExpContext ctx) {
        String funcName = ctx.funcName().getText();
        List<Value> arguments = new ArrayList<>();
        
        if (ctx.funcRParams() != null) {
            for (ToyCParser.FuncRParamContext param : ctx.funcRParams().funcRParam()) {
                arguments.add(visit(param.exp()));
            }
        }
        
        // Check if this is a void function
        Boolean isVoid = functionReturnTypes.get(funcName);
        if (isVoid != null && isVoid) {
            // Void function call - don't create a temporary result
            currentBlock.addInstruction(new CallInstruction(null, funcName, arguments));
            return null;
        } else {
            // Non-void function call - create temporary result
            Temporary result = new Temporary();
            currentBlock.addInstruction(new CallInstruction(result, funcName, arguments));
            return result;
        }
    }
    
    private Value visitUnaryOp(ToyCParser.ExpContext ctx) {
        UnaryOpInstruction.UnaryOp op;
        String opText = ctx.unaryOp().getText();
        op = switch (opText) {
            case "+" -> UnaryOpInstruction.UnaryOp.PLUS;
            case "-" -> UnaryOpInstruction.UnaryOp.MINUS;
            case "!" -> UnaryOpInstruction.UnaryOp.NOT;
            default ->
                    throw new RuntimeException("Unknown unary operator: " + opText);
        };
        
        Value operand = visit(ctx.exp(0));
        Temporary result = new Temporary();
        currentBlock.addInstruction(new UnaryOpInstruction(result, op, operand));
        return result;
    }
    
    private Value visitBinaryOp(ToyCParser.ExpContext ctx) {
        BinaryOpInstruction.BinaryOp op;
        
        if (ctx.MUL() != null) op = BinaryOpInstruction.BinaryOp.MUL;
        else if (ctx.DIV() != null) op = BinaryOpInstruction.BinaryOp.DIV;
        else if (ctx.MOD() != null) op = BinaryOpInstruction.BinaryOp.MOD;
        else if (ctx.PLUS() != null) op = BinaryOpInstruction.BinaryOp.ADD;
        else if (ctx.MINUS() != null) op = BinaryOpInstruction.BinaryOp.SUB;
        else if (ctx.LT() != null) op = BinaryOpInstruction.BinaryOp.LT;
        else if (ctx.GT() != null) op = BinaryOpInstruction.BinaryOp.GT;
        else if (ctx.LE() != null) op = BinaryOpInstruction.BinaryOp.LE;
        else if (ctx.GE() != null) op = BinaryOpInstruction.BinaryOp.GE;
        else if (ctx.EQ() != null) op = BinaryOpInstruction.BinaryOp.EQ;
        else if (ctx.NEQ() != null) op = BinaryOpInstruction.BinaryOp.NEQ;
        else if (ctx.AND() != null) op = BinaryOpInstruction.BinaryOp.AND;
        else if (ctx.OR() != null) op = BinaryOpInstruction.BinaryOp.OR;
        else throw new RuntimeException("Unknown binary operator");
        
        Value left = visit(ctx.exp(0));
        Value right = visit(ctx.exp(1));
        Temporary result = new Temporary();
        currentBlock.addInstruction(new BinaryOpInstruction(result, left, op, right));
        return result;
    }
    
    @Override
    public Value visitLVal(ToyCParser.LValContext ctx) {
        String varName = ctx.IDENT().getText();
        return variables.get(varName);
    }
    
    @Override
    public Value visitNumber(ToyCParser.NumberContext ctx) {
        int value = Integer.parseInt(ctx.INTEGER_CONST().getText());
        return new Constant(value);
    }
}