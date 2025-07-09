package toyc.analysis;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import toyc.World;
import toyc.config.AnalysisConfig;
import toyc.ir.IR;
import toyc.ir.IRPrinter;
import toyc.ir.stmt.Stmt;
import toyc.language.Function;
import toyc.util.collection.Maps;
import toyc.util.collection.MultiMap;
import toyc.util.collection.Pair;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.function.BiFunction;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static toyc.util.collection.CollectionUtils.getOne;

/**
 * Special class for process the results of other analyses after they finish.
 * This class is designed mainly for testing purpose. Currently, it supports
 * input/output analysis results from/to file, and compare analysis results
 * with input results. This analysis should be placed after the other analyses.
 */
public class ResultProcessor extends ProgramAnalysis {

    public static final String ID = "process-result";

    private static final Logger logger = LogManager.getLogger(ResultProcessor.class);

    private final String action;

    private PrintStream out;

    private MultiMap<Pair<String, String>, String> inputs;

    private Set<String> mismatches;

    public ResultProcessor(AnalysisConfig config) {
        super(config);
        action = getOptions().getString("action");
    }

    @Override
    public Object analyze() {
        // initialization
        switch (action) {
            case "dump" -> setOutput();
            case "compare" -> readInputs();
        }
        mismatches = new LinkedHashSet<>();
        // Classify given analysis IDs into two groups, one for inter-procedural
        // and the another one for intra-procedural analysis.
        // If an ID has result in World, then it is classified as
        // inter-procedural analysis, and others are intra-procedural analyses.
        @SuppressWarnings("unchecked")
        Map<Boolean, List<String>> groups = ((List<String>) getOptions().get("analyses"))
                .stream()
                .collect(Collectors.groupingBy(id -> World.get().getResult(id) != null));
        if (groups.containsKey(false)) {
            processIntraResults(groups.get(false));
        }
        if (getOptions().getBoolean("log-mismatches")) {
            mismatches.forEach(logger::info);
        }
        return mismatches;
    }

    private void setOutput() {
        String output = getOptions().getString("file");
        if (output != null) {
            try {
                out = new PrintStream(output);
            } catch (FileNotFoundException e) {
                throw new RuntimeException("Failed to open output file", e);
            }
        } else {
            out = System.out;
        }
    }

    private void readInputs() {
        String input = getOptions().getString("file");
        Path path = Path.of(input);
        try {
            inputs = Maps.newMultiMap();
            BufferedReader reader = Files.newBufferedReader(path);
            String line;
            Pair<String, String> currentKey = null;
            while ((line = reader.readLine()) != null) {
                Pair<String, String> key = extractKey(line);
                if (key != null) {
                    currentKey = key;
                } else if (!line.isBlank()) {
                    inputs.put(currentKey, line);
                }
            }
        } catch (IOException e) {
            throw new RuntimeException("Failed to read input file", e);
        }
    }

    private static Pair<String, String> extractKey(String line) {
        if (line.startsWith("----------") && line.endsWith("----------")) {
            int ms = line.indexOf('<'); // method start
            int me = line.indexOf("> "); // method end
            String method = line.substring(ms, me + 1);
            int as = line.lastIndexOf('('); // analysis start
            int ae = line.lastIndexOf(')'); // analysis end
            String analysis = line.substring(as + 1, ae);
            return new Pair<>(method, analysis);
        } else {
            return null;
        }
    }

    private void processIntraResults(List<String> analyses) {
        Stream<Function> functions = World.get()
                .getProgram()
                .allFunctions()
                .sorted(Comparator.comparing(f ->
                        f.getIR().getStmt(0).getLineNumber()));
        processResults(functions, analyses, (f, id) -> f.getIR().getResult(id));
    }

    private void processResults(Stream<Function> functions,
                                List<String> analyses,
                                BiFunction<Function, String, ?> resultGetter) {
        functions.forEach(function ->
                analyses.forEach(id -> {
                    switch (action) {
                        case "dump" -> dumpResult(function, id, resultGetter);
                        case "compare" -> compareResult(function, id,
                                resultGetter);
                    }
                })
        );
    }

    private void dumpResult(Function function, String id,
                            BiFunction<Function, String, ?> resultGetter) {
        out.printf("-------------------- %s (%s) --------------------%n", function,
                id);
        Object result = resultGetter.apply(function, id);
        if (result instanceof Set) {
            ((Set<?>) result).forEach(e -> out.println(toString(e)));
        } else if (result instanceof StmtResult<?> StmtResult) {
            IR ir = function.getIR();
            ir.forEach(stmt -> out.println(toString(stmt, StmtResult)));
        } else {
            out.println(toString(result));
        }
        out.println();
    }

    /**
     * Converts an object to string representation.
     * Here we specially handle Stmt by calling IRPrint.toString().
     */
    private static String toString(Object o) {
        if (o instanceof Stmt) {
            return IRPrinter.toString((Stmt) o);
        } else {
            return Objects.toString(o);
        }
    }

    /**
     * Converts a stmt and its analysis result to the corresponding
     * string representation.
     */
    private static String toString(Stmt stmt, StmtResult<?> result) {
        return toString(stmt) + " " + toString(result.getResult(stmt));
    }

    private void compareResult(Function function, String id,
                               BiFunction<Function, String, ?> resultGetter) {
        Set<String> inputResult = inputs.get(new Pair<>(function.toString(), id));
        Object result = resultGetter.apply(function, id);
        if (result instanceof Set) {
            Set<String> given = ((Set<?>) result)
                    .stream()
                    .map(ResultProcessor::toString)
                    .collect(Collectors.toCollection(LinkedHashSet::new));
            given.forEach(s -> {
                if (!inputResult.contains(s)) {
                    mismatches.add(function + " " + s +
                            " should NOT be included");
                }
            });
            inputResult.forEach(s -> {
                if (!given.contains(s)) {
                    mismatches.add(function + " " + s +
                            " should be included");
                }
            });
        } else if (result instanceof StmtResult<?> StmtResult) {
            Set<String> lines = inputs.get(new Pair<>(function.toString(), id));
            IR ir = function.getIR();
            ir.forEach(stmt -> {
                String stmtStr = toString(stmt);
                String given = toString(stmt, StmtResult);
                for (String line : lines) {
                    if (line.startsWith(stmtStr) && !line.equals(given)) {
                        int idx = stmtStr.length();
                        mismatches.add(String.format("%s %s expected: %s, given: %s",
                                function, stmtStr, line.substring(idx + 1),
                                given.substring(idx + 1)));
                    }
                }
            });
        } else if (inputResult.size() == 1) {
            if (!toString(result).equals(getOne(inputResult))) {
                mismatches.add(String.format("%s expected: %s, given: %s",
                        function, getOne(inputResult), toString(result)));
            }
        } else {
            logger.warn("Cannot compare result of analysis {} for {}," +
                            " expected: {}, given: {}",
                    id, function, inputResult, result);
        }
    }
}
