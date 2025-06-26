package toyc;

import java.io.IOException;

import toyc.semantic.SemanticChecker;
import toyc.util.LexerErrorListener;
import toyc.util.ParserErrorListener;
import org.antlr.v4.runtime.*;
import org.antlr.v4.runtime.tree.ParseTree;

public class Compiler {
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
        } else {CommonTokenStream tokens = new CommonTokenStream(toyCLexer);
            ToyCParser toyCParser = new ToyCParser(tokens);
            toyCParser.removeErrorListeners();
            ParserErrorListener parserErrorListener = new ParserErrorListener();
            toyCParser.addErrorListener(parserErrorListener);
            ParseTree tree = toyCParser.program();
            if (parserErrorListener.hasError()) {
                parserErrorListener.printParserErrorInformation();
            } else {
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
