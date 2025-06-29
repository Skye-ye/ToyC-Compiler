package toyc;

import java.io.IOException;

import toyc.semantic.SemanticChecker;
import toyc.util.LexerErrorListener;
import toyc.util.ParserErrorListener;
import toyc.ir.IRBuilder;
import toyc.ir.IRPrinter;
import toyc.ir.IROptimizer;
import toyc.ir.ControlFlowGraph;
import toyc.util.DOTGenerator;
import org.antlr.v4.runtime.*;
import org.antlr.v4.runtime.tree.ParseTree;

import java.util.List;
import java.util.ArrayList;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

public class Compiler {
    public static void main(String[] args) throws IOException {
        if (args.length < 1) {
            System.err.println("input path is required");
            return;
        }
        Compiler compiler = new Compiler();
        compiler.compile(args[0]);
    }
    
    public void compile(String source) throws IOException {
        CharStream input = CharStreams.fromFileName(source);
        ToyCLexer toyCLexer = new ToyCLexer(input);

        toyCLexer.removeErrorListeners();
        LexerErrorListener lexerErrorListener = new LexerErrorListener();
        toyCLexer.addErrorListener(lexerErrorListener);

        // Check for lexer errors
        if (lexerErrorListener.hasError()) {
            lexerErrorListener.printLexerErrorInformation();
        } else {
            CommonTokenStream tokens = new CommonTokenStream(toyCLexer);
            ToyCParser toyCParser = new ToyCParser(tokens);
            toyCParser.removeErrorListeners();
            ParserErrorListener parserErrorListener = new ParserErrorListener();
            toyCParser.addErrorListener(parserErrorListener);
            ParseTree tree = toyCParser.program();

            // Check for parser errors
            if (parserErrorListener.hasError()) {
                parserErrorListener.printParserErrorInformation();
            } else {

                // Perform semantic analysis
                SemanticChecker semanticChecker = new SemanticChecker();
                semanticChecker.visit(tree);
                if (semanticChecker.hasError()) {
                    System.err.println("Semantic analysis failed.");
                } else {
                    // Generate IR
                    IRBuilder irBuilder = new IRBuilder();
                    irBuilder.visit(tree);

                    // Store CFGs for external access
                    List<ControlFlowGraph> cfgs = new ArrayList<>(irBuilder.getFunctions().values());

                    // Optimize IR
                    for (ControlFlowGraph cfg : cfgs) {
                        IROptimizer.optimizeControlFlow(cfg);
                    }

                    // Generate CFG DOT files
                    generateCFGDotFiles(cfgs, source);

                    // Print IR
                    IRPrinter irPrinter = new IRPrinter();
                    String irOutput = irPrinter.printProgram(irBuilder.getFunctions());
                    System.out.println(irOutput);
                }
            }
        }
    }
    
    private void generateCFGDotFiles(List<ControlFlowGraph> cfgs, String sourceFilePath) {
        try {
            // Create output directory
            Path outputDir = Paths.get("output");
            if (!Files.exists(outputDir)) {
                Files.createDirectories(outputDir);
            }
            
            // Extract source file name without extension for prefix
            String sourceFileName = Paths.get(sourceFilePath).getFileName().toString();
            String baseName = sourceFileName.replaceFirst("[.][^.]+$", ""); // Remove extension
            
            // Generate DOT file for each function
            for (ControlFlowGraph cfg : cfgs) {
                String functionName = cfg.getFunctionName();
                String dotFileName = baseName + "_" + functionName + ".dot";
                Path dotFilePath = outputDir.resolve(dotFileName);
                
                try {
                    DOTGenerator.generateDOTFile(cfg, dotFilePath.toString());
                    System.out.println("Generated CFG DOT file: " + dotFilePath);
                } catch (IOException e) {
                    System.err.println("Failed to generate DOT file for function " + functionName + ": " + e.getMessage());
                }
            }
        } catch (IOException e) {
            System.err.println("Failed to create output directory: " + e.getMessage());
        }
    }
}
