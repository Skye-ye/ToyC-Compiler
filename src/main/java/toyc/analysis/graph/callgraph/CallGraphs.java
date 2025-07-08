package toyc.analysis.graph.callgraph;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import toyc.ir.IRPrinter;
import toyc.ir.stmt.Call;
import toyc.language.Function;
import toyc.util.Indexer;
import toyc.util.SimpleIndexer;
import toyc.util.collection.Maps;
import toyc.util.graph.DotAttributes;
import toyc.util.graph.DotDumper;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.PrintStream;
import java.util.Comparator;
import java.util.Map;

/**
 * Static utility methods about call graph.
 */
public final class CallGraphs {

    private static final Logger logger = LogManager.getLogger(CallGraphs.class);

    private CallGraphs() {
    }

    /**
     * Dumps call graph to dot file.
     */
    static void dumpCallGraph(CallGraph<Call, Function> callGraph, File outFile) {
        logger.info("Dumping call graph to {}",
                outFile.getAbsolutePath());
        Indexer<Function> indexer = new SimpleIndexer<>();
        new DotDumper<Function>()
                .setNodeToString(n -> Integer.toString(indexer.getIndex(n)))
                .setNodeLabeler(Function::toString)
                .setGlobalNodeAttributes(DotAttributes.of("shape", "box",
                        "style", "filled", "color", "\".3 .2 1.0\""))
                .setEdgeLabeler(e -> IRPrinter.toString(
                        ((FunctionEdge<Call, Function>) e).callSite()))
                .dump(callGraph, outFile);
    }

    static void dumpFunctions(CallGraph<Call, Function> callGraph,
                              File outFile) {
        try (PrintStream out =
                     new PrintStream(new FileOutputStream(outFile))) {
            logger.info("Dumping reachable methods to {}",
                    outFile.getAbsolutePath());
            callGraph.reachableFunctions()
                    .map(Function::getName)
                    .sorted()
                    .forEach(out::println);
        } catch (FileNotFoundException e) {
            logger.warn("Failed to dump reachable methods to " + outFile, e);
        }
    }

    static void dumpCallEdges(CallGraph<Call, Function> callGraph, File outFile) {
        try (PrintStream out =
                     new PrintStream(new FileOutputStream(outFile))) {
            logger.info("Dumping call edges to {}",
                    outFile.getAbsolutePath());
            callGraph.reachableFunctions()
                    // sort callers
                    .sorted(Comparator.comparing(Function::getName))
                    .forEach(f -> {
                        Map<Call, String> callReps = getCallReps(f);
                        callReps.forEach((call, rep) -> {
                            Function callee = callGraph.getCalleeOf(call);
                            out.println(rep + "\t" + callee);
                        });
                    });
        } catch (FileNotFoundException e) {
            logger.warn("Failed to dump call graph edges to {}", outFile.getAbsolutePath(), e);
        }
    }

    /**
     * @return a map from Call to its string representation in given method.
     */
    private static Map<Call, String> getCallReps(Function caller) {
        Map<String, Integer> counter = Maps.newMap();
        Map<Call, String> callReps =
                Maps.newOrderedMap(Comparator.comparing(Call::getIndex));
        
        // Get existing IR from function
        toyc.ir.IR ir = caller.getIR();
        if (ir == null) {
            return callReps;
        }
        
        ir.forEach(s -> {
            if (s instanceof Call Call) {
                Function callee = Call.getCallExp().getFunction();
                String target = callee.getName();
                int n = getCallNumber(target, counter);
                String rep = caller + "/" + target + "/" + n;
                callReps.put(Call, rep);
            }
        });
        return callReps;
    }

    private static int getCallNumber(String target, Map<String, Integer> counter) {
        Integer n = counter.get(target);
        if (n == null) {
            n = 0;
        }
        counter.put(target, n + 1);
        return n;
    }

    public static String toString(Call Call) {
        return Call.getContainer() + IRPrinter.toString(Call);
    }
}
