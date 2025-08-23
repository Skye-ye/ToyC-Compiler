package toyc.algorithm.analysis.graph.cfg;

import toyc.ir.stmt.Stmt;
import toyc.language.Function;
import toyc.language.type.Type;
import toyc.util.Indexer;
import toyc.util.SimpleIndexer;
import toyc.util.graph.DotAttributes;
import toyc.util.graph.DotDumper;

import java.io.File;
import java.util.stream.Collectors;

public class CFGDumper {

    /**
     * Limits length of file name, otherwise it may exceed the max file name
     * length of the underlying file system.
     */
    private static final int FILENAME_LIMIT = 200;

    /**
     * Dumps the given CFG to .dot file.
     */
    static <N> void dumpDotFile(CFG<N> cfg, File dumpDir) {
        Indexer<N> indexer = new SimpleIndexer<>();
        new DotDumper<N>()
                .setNodeToString(n -> Integer.toString(indexer.getIndex(n)))
                .setNodeLabeler(n -> toLabel(n, cfg))
                .setGlobalNodeAttributes(DotAttributes.of("shape", "box",
                        "style", "filled", "color", "\".3 .2 1.0\""))
                .setEdgeLabeler(e -> {
                    CFGEdge<N> edge = (CFGEdge<N>) e;
                    return edge.getKind().toString();
                })
                .setEdgeAttributer(e -> DotAttributes.of())
                .dump(cfg, new File(dumpDir, toDotFileName(cfg)));
    }

    public static <N> String toLabel(N node, CFG<N> cfg) {
        if (cfg.isEntry(node)) {
            return "Entry[" + cfg.getFunction() + "]";
        } else if (cfg.isExit(node)) {
            return "Exit[" + cfg.getFunction() + "]";
        } else {
            return node instanceof Stmt ?
                    ((Stmt) node).getIndex() + ": " + node.toString().replace("\"", "\\\"") :
                    node.toString();
        }
    }

    private static String toDotFileName(CFG<?> cfg) {
        Function f = cfg.getFunction();
        String fileName = f.getName() + '(' +
                f.getParamTypes()
                        .stream()
                        .map(Type::toString)
                        .collect(Collectors.joining(",")) +
                ')';
        if (fileName.length() > FILENAME_LIMIT) {
            fileName = fileName.substring(0, FILENAME_LIMIT) + "...";
        }
        // escape invalid characters in file name
        fileName = fileName.replaceAll("[\\[\\]<>]", "_") + ".dot";
        return fileName;
    }
}
