package toyc.ir;

import toyc.ir.instruction.*;

public interface InstructionVisitor {
    void visit(AssignInstruction instruction);
    void visit(BinaryOpInstruction instruction);
    void visit(UnaryOpInstruction instruction);
    void visit(CallInstruction instruction);
    void visit(ReturnInstruction instruction);
    void visit(JumpInstruction instruction);
    void visit(ConditionalJumpInstruction instruction);
    void visit(LabelInstruction instruction);
}