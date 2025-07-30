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
import toyc.config.AlgorithmConfig;
import toyc.config.Options;
import toyc.frontend.semantic.SemanticChecker;
import toyc.frontend.util.LexerErrorListener;
import toyc.frontend.util.ParserErrorListener;
import toyc.ir.IR;
import toyc.language.Function;
import toyc.language.Program;
import toyc.frontend.ir.IRBuilder;

import java.io.IOException;
import java.lang.reflect.Field;
import java.util.*;

public class ToyCWorldBuilder extends AbstractWorldBuilder {

    private static final Logger logger = LogManager.getLogger(ToyCWorldBuilder.class);
    
    private IRBuilder irBuilder;
    private Map<String, Function> functions;

    @Override
    public void build(Options options, List<AlgorithmConfig> analyses) {
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
        String inputFile = getInputFile(options);

        try {
            // Parse and analyze the input file
            parseAndAnalyze(inputFile);
            
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
                Field irField = Function.class.getDeclaredField("ir");
                irField.setAccessible(true);
                irField.set(function, ir);
            } catch (Exception e) {
                throw new RuntimeException("Failed to set IR on function: " + funcName, e);
            }
            
            functions.put(funcName, function);
        }
        
        // Build program representation and ensure all functions have IR built
        Program program = new Program(new ArrayList<>(functions.values()));
        irBuilder.buildAll(program);
        
        // Set program in world
        World.get().setProgram(program);
        World.get().setMainFunction(functions.get("main"));
        
        logger.info("IR generation completed");
    }
}