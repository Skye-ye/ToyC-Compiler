package toyc.frontend;

import org.antlr.v4.runtime.CharStream;
import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.tree.ParseTree;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import toyc.AbstractWorldBuilder;
import toyc.ToyCLexer;
import toyc.ToyCParser;
import toyc.World;
import toyc.config.AnalysisConfig;
import toyc.config.Options;
import toyc.frontend.semantic.SemanticChecker;
import toyc.frontend.util.LexerErrorListener;
import toyc.frontend.util.ParserErrorListener;
import toyc.ir.IR;
import toyc.ir.IRPrinter;
import toyc.language.Function;
import toyc.language.Program;
import toyc.frontend.ir.IRBuilder;

import java.io.IOException;
import java.util.*;

public class ToyCWorldBuilder extends AbstractWorldBuilder {

    private static final Logger logger = LogManager.getLogger(ToyCWorldBuilder.class);
    
    private IRBuilder irBuilder;
    private Map<String, Function> functions;
    private Function mainFunction;

    @Override
    public void build(Options options, List<AnalysisConfig> analyses) {
        logger.info("Building ToyC world...");
        
        // Reset World
        World.reset();
        World world = new World();
        World.set(world);
        
        // Set options
        world.setOptions(options);
        
        // Initialize IR builder
        this.irBuilder = new IRBuilder();
        this.functions = new HashMap<>();
        
        // Process input files
        List<String> inputFiles = getInputFiles(options);
        if (inputFiles.isEmpty()) {
            throw new RuntimeException("No input files specified");
        }
        
        // For now, process the first input file
        String inputFile = inputFiles.getFirst();
        
        try {
            // Parse and analyze the input file
            parseAndAnalyze(inputFile);
            
            // Program will be built after IR generation
            
            // Set main function
            if (mainFunction != null) {
                world.setMainFunction(mainFunction);
            } else {
                logger.warn("No main function found in program");
            }
            
            // Set IR builder
            world.setIRBuilder(irBuilder);
            
            logger.info("ToyC world built successfully with {} functions", functions.size());
            
        } catch (IOException e) {
            throw new RuntimeException("Failed to build ToyC world", e);
        }
    }

    private void parseAndAnalyze(String inputFile) throws IOException {
        logger.info("Parsing and analyzing file: {}", inputFile);
        
        // Lexical analysis
        CharStream input = CharStreams.fromFileName(inputFile);
        ToyCLexer lexer = new ToyCLexer(input);
        
        lexer.removeErrorListeners();
        LexerErrorListener lexerErrorListener = new LexerErrorListener();
        lexer.addErrorListener(lexerErrorListener);
        
        // Check for lexer errors
        if (lexerErrorListener.hasError()) {
            lexerErrorListener.printLexerErrorInformation();
            throw new RuntimeException("Lexical analysis failed");
        }
        
        // Parsing
        CommonTokenStream tokens = new CommonTokenStream(lexer);
        ToyCParser parser = new ToyCParser(tokens);
        parser.removeErrorListeners();
        ParserErrorListener parserErrorListener = new ParserErrorListener();
        parser.addErrorListener(parserErrorListener);
        ParseTree tree = parser.program();
        
        // Check for parser errors
        if (parserErrorListener.hasError()) {
            parserErrorListener.printParserErrorInformation();
            throw new RuntimeException("Parsing failed");
        }
        
        // Semantic analysis
        SemanticChecker semanticChecker = new SemanticChecker();
        semanticChecker.visit(tree);
        if (semanticChecker.hasError()) {
            throw new RuntimeException("Semantic analysis failed");
        }
        
        logger.info("Semantic analysis passed, building functions...");
        
        // Generate IR (this also extracts function information)
        irBuilder.visit(tree);
        
        // Get functions from IR builder and set up bidirectional links
        for (Map.Entry<String, IR> entry : irBuilder.getFunctions().entrySet()) {
            String funcName = entry.getKey();
            IR ir = entry.getValue();
            Function function = ir.getFunction();
            
            // Set the IR on the function (using reflection to access private field)
            try {
                java.lang.reflect.Field irField = Function.class.getDeclaredField("ir");
                irField.setAccessible(true);
                irField.set(function, ir);
            } catch (Exception e) {
                throw new RuntimeException("Failed to set IR on function: " + funcName, e);
            }
            
            functions.put(funcName, function);
        }
        
        // Set main function from the functions extracted from IR builder
        mainFunction = functions.get("main");
        if (mainFunction != null) {
            logger.info("Found main function: {}", mainFunction);
        }
        
        // Build program representation and ensure all functions have IR built
        Program program = new Program(new ArrayList<>(functions.values()));
        irBuilder.buildAll(program);
        
        // Set program in world
        World.get().setProgram(program);
        
        // Optimize IR if optimization is enabled
        irBuilder.optimizeAllFunctions();
        
        // Print IR for all functions
        printIR();
        
        logger.info("IR generation completed");
    }

    /**
     * Prints IR for all functions to the console.
     */
    private void printIR() {
        System.out.println("\n" + "=".repeat(60));
        System.out.println("IR DUMP");
        System.out.println("=".repeat(60));
        
        for (Map.Entry<String, IR> entry : irBuilder.getFunctions().entrySet()) {
            IR ir = entry.getValue();
            
            System.out.println();
            IRPrinter.print(ir, System.out);
        }
        
        System.out.println("\n" + "=".repeat(60));
    }

    // Function extraction is now handled by ToyCIRBuilder during IR generation

    /**
     * Gets the extracted functions from the world building process.
     * @return Map of function name to Function object
     */
    public Map<String, Function> getFunctions() {
        return functions;
    }
    
    /**
     * Gets the main function if it exists.
     * @return The main function or null if not found
     */
    public Function getMainFunction() {
        return mainFunction;
    }
}