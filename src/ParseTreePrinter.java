import org.antlr.v4.runtime.tree.RuleNode;
import org.antlr.v4.runtime.tree.TerminalNode;
import org.antlr.v4.runtime.ParserRuleContext;

public class ParseTreePrinter extends ToyCParserBaseVisitor<String> {
    private int indentLevel = 0;

    @Override
    public String visitChildren(RuleNode node) {
        String ruleName = ToyCParser.ruleNames[((ParserRuleContext)node).getRuleIndex()];
        System.out.println("  ".repeat(indentLevel) + ruleName);

        indentLevel++;
        String result = super.visitChildren(node);
        indentLevel--;

        return result;
    }

    @Override
    public String visitTerminal(TerminalNode node) {
        String tokenName = ToyCLexer.VOCABULARY.getSymbolicName(node.getSymbol().getType());
        System.out.println("  ".repeat(indentLevel) + tokenName + ": " + node.getText());
        return null;
    }
}
