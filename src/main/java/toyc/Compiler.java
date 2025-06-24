package toyc;

import java.io.IOException;

import toyc.util.LexerErrorListener;
import toyc.util.ParseTreePrinter;
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

        // remove the default error listener and add our own to collect error information
        toyCLexer.removeErrorListeners();
        LexerErrorListener lexerErrorListener = new LexerErrorListener();
        toyCLexer.addErrorListener(lexerErrorListener);

        CommonTokenStream tokens = new CommonTokenStream(toyCLexer);
        ToyCParser toyCParser = new ToyCParser(tokens);
        toyCParser.removeErrorListeners();
        ParserErrorListener parserErrorListener = new ParserErrorListener();
        toyCParser.addErrorListener(parserErrorListener);

        ParseTree tree = toyCParser.compUnit();

        if (lexerErrorListener.hasError()) {
            lexerErrorListener.printLexerErrorInformation();
        } else {
            if (parserErrorListener.hasError()) {
                parserErrorListener.printParserErrorInformation();
            } else {
                ParseTreePrinter printer = new ParseTreePrinter();
                printer.visit(tree);
            }
        }
    }
}
