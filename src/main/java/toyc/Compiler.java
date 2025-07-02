package toyc;

import java.io.IOException;

import toyc.semantic.SemanticChecker;
import toyc.util.LexerErrorListener;
import toyc.util.ParserErrorListener;
import toyc.ir.ToyCIRBuilder;
import toyc.ir.IRPrinter;
import toyc.ir.IR;
import toyc.ir.IROptimizer;
import toyc.analysis.graph.callgraph.CallGraphBuilder;
import toyc.analysis.graph.callgraph.CallGraph;
import toyc.analysis.graph.cfg.CFGBuilder;
import toyc.analysis.graph.cfg.CFG;
import toyc.analysis.graph.icfg.ICFGBuilder;
import toyc.analysis.graph.icfg.ICFG;
import toyc.ir.stmt.Call;
import toyc.ir.stmt.Stmt;
import toyc.language.Function;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.antlr.v4.runtime.*;
import org.antlr.v4.runtime.tree.ParseTree;

import java.util.Collection;
import java.util.Map;

public class Compiler {
    private static final Logger logger = LogManager.getLogger(Compiler.class);

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
                    ToyCIRBuilder irBuilder = new ToyCIRBuilder();
                    irBuilder.visit(tree);

                    // Optimize IR by removing redundant NOPs
                    System.out.println("=== Optimizing IR ===");
                    Map<String, IR> optimizedFunctions = IROptimizer.optimizeAll(irBuilder.getFunctions());
                    irBuilder.updateFunctions(optimizedFunctions);
                    System.out.println("IR optimization completed");

                    // Store IRs for external access
                    Collection<IR> irs = irBuilder.getFunctions().values();
                    
                    // Set up World for analysis
                    World world = new World();
                    world.setIRBuilder(irBuilder);
                    
                    // Extract source file name from path for output directory naming
                    String sourceFileName = java.nio.file.Paths.get(source).getFileName().toString();
                    world.setSourceFileName(sourceFileName);
                    
                    // Find main function
                    Function mainFunction = irBuilder.getFunctions().values().stream()
                            .map(IR::getFunction)
                            .filter(function -> "main".equals(function.getName()))
                            .findFirst()
                            .orElse(null);
                    if (mainFunction != null) {
                        world.setMainFunction(mainFunction);
                    }
                    World.set(world);

                    // Generate analysis outputs
                    if (mainFunction != null) {
                        generateCallGraph();
                        generateCFGs(irs);
                        generateICFG();
                    }
                    
                    // Print IR using IRPrinter
                    for (IR ir : irs) {
                        IRPrinter.print(ir, System.out);
                    }
                }
            }
        }
    }
    
    private void generateCallGraph() {
        try {
            System.out.println("=== Generating Call Graph ===");
            CallGraphBuilder cgBuilder = new CallGraphBuilder(CallGraphBuilder.ID);
            CallGraph<Call, Function> callGraph = cgBuilder.analyze();
            // Store the call graph result in World for ICFG generation
            World.get().storeResult(CallGraphBuilder.ID, callGraph);
            System.out.println("Call graph generated with " + callGraph.getNumberOfFunctions() + 
                             " functions and " + callGraph.getNumberOfEdges() + " edges");
        } catch (Exception e) {
            System.err.println("Failed to generate call graph: " + e.getMessage());
            logger.error("Error occurred in call graph generation", e);
        }
    }
    
    private void generateCFGs(Collection<IR> irs) {
        try {
            System.out.println("=== Generating Control Flow Graphs ===");
            CFGBuilder cfgBuilder = new CFGBuilder(CFGBuilder.ID);
            int cfgCount = 0;
            for (IR ir : irs) {
                CFG<Stmt> cfg = cfgBuilder.analyze(ir);
                // Store the CFG result in the IR for ICFG generation
                ir.storeResult(CFGBuilder.ID, cfg);
                cfgCount++;
            }
            System.out.println("Generated CFGs for " + cfgCount + " functions");
        } catch (Exception e) {
            System.err.println("Failed to generate CFGs: " + e.getMessage());
            logger.error("Error occurred in CFG generation", e);
        }
    }
    
    private void generateICFG() {
        try {
            System.out.println("=== Generating Inter-procedural Control Flow Graph ===");
            ICFGBuilder icfgBuilder = new ICFGBuilder(ICFGBuilder.ID);
            ICFG<Function, Stmt> icfg = icfgBuilder.analyze();
            System.out.println("ICFG generated successfully");
        } catch (Exception e) {
            System.err.println("Failed to generate ICFG: " + e.getMessage());
            logger.error("Error occurred in ICFG generation", e);
        }
    }
}
