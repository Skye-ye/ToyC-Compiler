package toyc.analysis.graph.icfg;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import toyc.World;
import toyc.analysis.ProgramAnalysis;
import toyc.analysis.graph.callgraph.CallGraph;
import toyc.analysis.graph.callgraph.CallGraphBuilder;
import toyc.analysis.graph.cfg.CFG;
import toyc.analysis.graph.cfg.CFGBuilder;
import toyc.analysis.graph.cfg.CFGDumper;
import toyc.ir.IR;
import toyc.ir.stmt.Stmt;
import toyc.language.Function;
import toyc.util.Indexer;
import toyc.util.SimpleIndexer;
import toyc.util.graph.DotAttributes;
import toyc.util.graph.DotDumper;

import java.io.File;

public class ICFGBuilder extends ProgramAnalysis<ICFG<Function, Stmt>> {

    public static final String ID = "icfg";

    private static final Logger logger = LogManager.getLogger(ICFGBuilder.class);

    public ICFGBuilder(String config) {
        super(config);
    }

    @Override
    public ICFG<Function, Stmt> analyze() {
        CallGraph<Stmt, Function> callGraph = World.get().getResult(CallGraphBuilder.ID);
        ICFG<Function, Stmt> icfg = new DefaultICFG(callGraph);
        dumpICFG(icfg);
        return icfg;
    }

    private static void dumpICFG(ICFG<Function, Stmt> icfg) {
        Function mainMethod;
        String fileName;
        if ((mainMethod = World.get().getMainFunction()) != null) {
            fileName = mainMethod + "-icfg.dot";
        } else {
            fileName = "icfg.dot";
        }
        File dotFile = new File(World.get().getOutputDir(), fileName);
        logger.info("Dumping ICFG to {}", dotFile.getAbsolutePath());
        Indexer<Stmt> indexer = new SimpleIndexer<>();
        new DotDumper<Stmt>()
                .setNodeToString(n -> Integer.toString(indexer.getIndex(n)))
                .setNodeLabeler(n -> toLabel(n, icfg))
                .setGlobalNodeAttributes(DotAttributes.of("shape", "box",
                        "style", "filled", "color", "\".3 .2 1.0\""))
                .setEdgeAttributer(e -> switch (e) {
                    case CallEdge ignored ->
                            DotAttributes.of("style", "dashed", "color", "blue");
                    case ReturnEdge ignored ->
                            DotAttributes.of("style", "dashed", "color", "red");
                    case CallToReturnEdge ignored ->
                            DotAttributes.of("style", "dashed");
                    case null, default -> DotAttributes.of();
                })
                .dump(icfg, dotFile);
    }

    private static String toLabel(Stmt stmt, ICFG<Function, Stmt> icfg) {
        Function function = icfg.getContainingFunctionOf(stmt);
        CFG<Stmt> cfg = getCFGOf(function);
        assert cfg != null;
        return CFGDumper.toLabel(stmt, cfg);
    }

    static CFG<Stmt> getCFGOf(Function function) {
        // Get CFG from the same IR instance where it was stored during generation
        // Use function name as key since IRBuilder stores by name
        IR ir = World.get().getIRBuilder().getFunctions().get(function.getName());
        return ir.getResult(CFGBuilder.ID);
    }
}
