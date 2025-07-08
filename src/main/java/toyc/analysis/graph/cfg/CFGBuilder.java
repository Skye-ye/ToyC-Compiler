package toyc.analysis.graph.cfg;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import toyc.World;
import toyc.analysis.FunctionAnalysis;
import toyc.config.AnalysisConfig;
import toyc.ir.IR;
import toyc.ir.stmt.*;

import java.io.File;

public class CFGBuilder extends FunctionAnalysis<CFG<Stmt>> {

    public static final String ID = "cfg";

    private static final Logger logger = LogManager.getLogger(CFGBuilder.class);

    private static final String CFG_DIR = "cfg";


    private final File dumpDir;

    public CFGBuilder(AnalysisConfig config) {
        super(config);
        dumpDir = new File(World.get().getOptions().getOutputDir(), CFG_DIR);
        if (!dumpDir.exists()) {
            dumpDir.mkdirs();
        }
        logger.info("Dumping CFGs in {}", dumpDir.getAbsolutePath());
    }

    @Override
    public CFG<Stmt> analyze(IR ir) {
        StmtCFG cfg = new StmtCFG(ir);
        cfg.setEntry(new Nop());
        cfg.setExit(new Nop());
        buildNormalEdges(cfg);
        CFGDumper.dumpDotFile(cfg, dumpDir);
        return cfg;
    }

    private static void buildNormalEdges(StmtCFG cfg) {
        IR ir = cfg.getIR();
        cfg.addEdge(new CFGEdge<>(CFGEdge.Kind.ENTRY, cfg.getEntry(), ir.getStmt(0)));
        for (int i = 0; i < ir.getStmts().size(); ++i) {
            Stmt curr = ir.getStmt(i);
            cfg.addNode(curr);
            if (curr instanceof Goto) {
                cfg.addEdge(new CFGEdge<>(CFGEdge.Kind.GOTO,
                        curr, ((Goto) curr).getTarget()));
            } else if (curr instanceof If) {
                cfg.addEdge(new CFGEdge<>(CFGEdge.Kind.IF_TRUE,
                        curr, ((If) curr).getTarget()));
                cfg.addEdge(new CFGEdge<>(CFGEdge.Kind.IF_FALSE,
                        curr, ir.getStmt(i + 1)));
            } else if (curr instanceof Return) {
                cfg.addEdge(new CFGEdge<>(CFGEdge.Kind.RETURN, curr, cfg.getExit()));
            } else if (curr.canFallThrough() &&
                    i + 1 < ir.getStmts().size()) { // Defensive check
                cfg.addEdge(new CFGEdge<>(CFGEdge.Kind.FALL_THROUGH,
                        curr, ir.getStmt(i + 1)));
            }
        }
    }
}
