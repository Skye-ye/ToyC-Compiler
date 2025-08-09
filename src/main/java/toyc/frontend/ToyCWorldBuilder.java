package toyc.frontend;

import org.antlr.v4.runtime.CharStream;
import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.CommonTokenStream;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import toyc.AbstractWorldBuilder;
import toyc.ToyCLexer;
import toyc.ToyCParser;
import toyc.World;
import toyc.config.AnalysisConfig;
import toyc.config.Options;
import toyc.frontend.ir.IRBuilder;
import toyc.frontend.semantic.SemanticChecker;
import toyc.frontend.util.LexerErrorListener;
import toyc.frontend.util.ParserErrorListener;
import toyc.language.Function;
import toyc.language.Program;
import toyc.language.type.IntType;
import toyc.language.type.Type;
import toyc.language.type.VoidType;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ToyCWorldBuilder extends AbstractWorldBuilder {

    private static final Logger logger = LogManager.getLogger(ToyCWorldBuilder.class);
    private Map<String, Function> functions;
    private Map<String, ToyCParser.FuncDefContext> functionContexts;

    @Override
    public void build(Options options, List<AnalysisConfig> analyses) {
        logger.info("Building ToyC world...");

        // Reset World
        World.reset();
        World world = new World();
        World.set(world);

        // Set options
        world.setOptions(options);

        // Initialize function and context maps
        this.functions = new HashMap<>();
        this.functionContexts = new HashMap<>();

        // Process input files
        String inputFile = getInputFile(options);

        // Front-end processing
        frontEnd(inputFile);

        // Set IR builder
        IRBuilder irBuilder = new IRBuilder(functionContexts);
        world.setIRBuilder(irBuilder);

        logger.info("ToyC world built successfully with {} functions", functions.size());
    }

    private void frontEnd(String inputFile) {
        try {
            // Lex the input file
            ToyCLexer lexer = lex(inputFile);

            // Parse the input file
            ToyCParser.ProgramContext programCtx = parse(lexer);

            // Perform semantic analysis
            semanticCheck(programCtx);

            // Collect functions from the program context
            collectFunctions(programCtx);

            Program program = new Program(new ArrayList<>(functions.values()));

            // Set program in world
            World.get().setProgram(program);

            // Set main function (it should exist since we did semantic check)
            Function mainFunction = functions.get("main");
            assert mainFunction != null;
            World.get().setMainFunction(mainFunction);

        } catch (IOException e) {
            logger.error("Error during front-end processing: {}", e.getMessage());
            throw new RuntimeException("Front-end processing failed", e);
        }
    }

    private ToyCLexer lex(String inputFile) throws IOException {
        logger.info("Lexing file: {}", inputFile);

        // Create a CharStream from the input file
        CharStream input = CharStreams.fromFileName(inputFile);
        ToyCLexer lexer = new ToyCLexer(input);

        // Remove default error listeners and add custom one
        lexer.removeErrorListeners();
        LexerErrorListener lexerErrorListener = new LexerErrorListener();
        lexer.addErrorListener(lexerErrorListener);

        // Check for lexer errors
        if (lexerErrorListener.hasError()) {
            lexerErrorListener.printLexerErrorInformation();
            throw new RuntimeException("Lexical analysis failed");
        }

        logger.info("Lexing completed successfully");
        return lexer;
    }

    private ToyCParser.ProgramContext parse(ToyCLexer lexer) {
        logger.info("Parsing input...");

        // Create a token stream from the lexer
        CommonTokenStream tokens = new CommonTokenStream(lexer);
        ToyCParser parser = new ToyCParser(tokens);

        // Remove default error listeners and add custom one
        parser.removeErrorListeners();
        ParserErrorListener parserErrorListener = new ParserErrorListener();
        parser.addErrorListener(parserErrorListener);

        // Parse the input file
        ToyCParser.ProgramContext programCtx = parser.program();

        // Check for parser errors
        if (parserErrorListener.hasError()) {
            parserErrorListener.printParserErrorInformation();
            throw new RuntimeException("Parsing failed");
        }

        logger.info("Parsing completed successfully");
        return programCtx;
    }

    private void semanticCheck(ToyCParser.ProgramContext programCtx) {
        logger.info("Starting semantic analysis...");

        // Create a SemanticChecker instance
        SemanticChecker semanticChecker = new SemanticChecker();
        semanticChecker.visit(programCtx); // Visit the entire program tree

        // Check for semantic errors
        if (semanticChecker.hasError()) {
            throw new RuntimeException("Semantic analysis failed");
        }

        logger.info("Semantic analysis passed");
    }

    private void collectFunctions(ToyCParser.ProgramContext programCtx) {
        // Check if programCtx is null
        if (programCtx == null) {
            logger.warn("ProgramContext is null");
            return;
        }

        // Initialize the CompUnitContext from the ProgramContext
        ToyCParser.CompUnitContext compUnitCtx = programCtx.compUnit();

        // Collect functions from the CompUnitContext
        for (ToyCParser.FuncDefContext funcDef : compUnitCtx.funcDef()) {
            String funcName = funcDef.funcName().IDENT().getText();
            Type returnType = funcDef.funcType().VOID() != null ? VoidType.VOID :
                    IntType.INT;

            List<Type> paramTypes = new ArrayList<>();
            List<String> paramNames = new ArrayList<>();

            if (funcDef.funcFParams() != null) {
                for (ToyCParser.FuncFParamContext param : funcDef.funcFParams().funcFParam()) {
                    paramTypes.add(IntType.INT);
                    paramNames.add(param.IDENT().getText());
                }
            }

            Function function = new Function(funcName, paramTypes, returnType, paramNames);
            functions.put(funcName, function);
            functionContexts.put(funcName, funcDef);
        }
    }
}