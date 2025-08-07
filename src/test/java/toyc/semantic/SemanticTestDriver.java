package toyc.semantic;

import java.io.IOException;

import toyc.ToyCLexer;
import toyc.ToyCParser;
import toyc.frontend.semantic.SemanticChecker;
import toyc.frontend.util.LexerErrorListener;
import toyc.frontend.util.ParserErrorListener;
import toyc.frontend.util.formatter.ToyCFormatter;
import org.antlr.v4.runtime.*;
import org.antlr.v4.runtime.tree.ParseTree;

public class SemanticTestDriver {
    public static void main(String[] args) throws IOException {
        if (args.length < 1) {
            System.err.println("input path is required");
            return;
        }
        String source = args[0];
        CharStream input = CharStreams.fromFileName(source);
        ToyCLexer toyCLexer = new ToyCLexer(input);

        toyCLexer.removeErrorListeners();
        LexerErrorListener lexerErrorListener = new LexerErrorListener();
        toyCLexer.addErrorListener(lexerErrorListener);

        if (lexerErrorListener.hasError()) {
            lexerErrorListener.printLexerErrorInformation();
        } else {
            CommonTokenStream tokens = new CommonTokenStream(toyCLexer);
            ToyCParser toyCParser = new ToyCParser(tokens);
            toyCParser.removeErrorListeners();
            ParserErrorListener parserErrorListener = new ParserErrorListener();
            toyCParser.addErrorListener(parserErrorListener);
            ParseTree tree = toyCParser.program();
            if (parserErrorListener.hasError()) {
                parserErrorListener.printParserErrorInformation();
            } else {
                SemanticChecker checker = new SemanticChecker();
                checker.visit(tree);
                if (!checker.hasError()) {
                    System.out.println("No semantic errors found.");
                    ToyCFormatter formatter = new ToyCFormatter();
                    formatter.visit(tree);
                    String formattedCode = formatter.getFormattedCode();
                    System.out.println(formattedCode);
                    SemanticChecker semanticChecker = new SemanticChecker();
                    semanticChecker.visit(tree);
                    if (!semanticChecker.hasError()) {
                        System.out.println("Semantic analysis passed.");
                    } else {
                        System.err.println("Semantic analysis failed.");
                    }
                }
            }
        }
    }
}