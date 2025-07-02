package toyc.analysis.graph.icfg;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import toyc.analysis.graph.callgraph.CallGraph;
import toyc.analysis.graph.cfg.CFG;
import toyc.analysis.graph.cfg.CFGEdge;
import toyc.ir.exp.Var;
import toyc.ir.stmt.Call;
import toyc.ir.stmt.Return;
import toyc.ir.stmt.Stmt;
import toyc.language.Function;
import toyc.util.collection.Maps;
import toyc.util.collection.MultiMap;
import toyc.util.collection.Sets;
import toyc.util.collection.Views;

import java.util.Collections;
import java.util.Map;
import java.util.Set;

import static toyc.analysis.graph.icfg.ICFGBuilder.getCFGOf;

class DefaultICFG extends AbstractICFG<Function, Stmt> {

    private static final Logger logger = LogManager.getLogger(DefaultICFG.class);

    private final MultiMap<Stmt, ICFGEdge<Stmt>> inEdges = Maps.newMultiMap();

    private final MultiMap<Stmt, ICFGEdge<Stmt>> outEdges = Maps.newMultiMap();

    private final Map<Stmt, CFG<Stmt>> stmtToCFG = Maps.newLinkedHashMap();

    DefaultICFG(CallGraph<Stmt, Function> callGraph) {
        super(callGraph);
        build(callGraph);
    }

    private void build(CallGraph<Stmt, Function> callGraph) {
        callGraph.forEach(method -> {
            CFG<Stmt> cfg = getCFGOf(method);
            if (cfg == null) {
                logger.warn("CFG of {} is absent, try to fix this" +
                        " by adding option: -scope REACHABLE", method);
                return;
            }
            cfg.forEach(stmt -> {
                stmtToCFG.put(stmt, cfg);
                cfg.getOutEdgesOf(stmt).forEach(edge -> {
                    ICFGEdge<Stmt> local = isCallSite(stmt) ?
                            new CallToReturnEdge<>(edge) :
                            new NormalEdge<>(edge);
                    outEdges.put(stmt, local);
                    inEdges.put(edge.target(), local);
                });
                if (isCallSite(stmt)) {
                    Function callee = getCalleeOf(stmt);
                    if (getCFGOf(callee) == null) {
                        logger.warn("CFG of {} is missing", callee);
                        return;
                    }
                    // Add call edges
                    Stmt entry = getEntryOf(callee);
                    CallEdge<Stmt> call = new CallEdge<>(stmt, entry, callee);
                    outEdges.put(stmt, call);
                    inEdges.put(entry, call);
                    // Add return edges
                    Stmt exit = getExitOf(callee);
                    Set<Var> retVars = Sets.newHybridSet();
                    // The exit node of CFG is mock, thus it is not
                    // a real return or excepting Stmt. We need to
                    // collect return and exception information from
                    // the real return and excepting Stmts, and attach
                    // them to the ReturnEdge.
                    getCFGOf(callee).getInEdgesOf(exit).forEach(retEdge -> {
                        if (retEdge.getKind() == CFGEdge.Kind.RETURN) {
                            Return ret = (Return) retEdge.source();
                            if (ret.getValue() != null) {
                                retVars.add(ret.getValue());
                            }
                        }
                    });
                    getReturnSitesOf(stmt).forEach(retSite -> {
                        ReturnEdge<Stmt> ret = new ReturnEdge<>(
                                exit, retSite, stmt, retVars);
                        outEdges.put(exit, ret);
                        inEdges.put(retSite, ret);
                    });
                }
            });
        });
    }

    @Override
    public Set<ICFGEdge<Stmt>> getInEdgesOf(Stmt stmt) {
        return inEdges.get(stmt);
    }

    @Override
    public Set<ICFGEdge<Stmt>> getOutEdgesOf(Stmt stmt) {
        return outEdges.get(stmt);
    }

    @Override
    public Stmt getEntryOf(Function method) {
        return getCFGOf(method).getEntry();
    }

    @Override
    public Stmt getExitOf(Function method) {
        return getCFGOf(method).getExit();
    }

    @Override
    public Set<Stmt> getReturnSitesOf(Stmt callSite) {
        assert isCallSite(callSite);
        return stmtToCFG.get(callSite).getSuccsOf(callSite);
    }

    @Override
    public Function getContainingFunctionOf(Stmt stmt) {
        return stmtToCFG.get(stmt).getFunction();
    }

    @Override
    public boolean isCallSite(Stmt stmt) {
        return stmt instanceof Call;
    }

    @Override
    public boolean hasEdge(Stmt source, Stmt target) {
        return getOutEdgesOf(source)
                .stream()
                .anyMatch(edge -> edge.target().equals(target));
    }

    @Override
    public Set<Stmt> getPredsOf(Stmt stmt) {
        return Views.toMappedSet(getInEdgesOf(stmt), ICFGEdge::source);
    }

    @Override
    public Set<Stmt> getSuccsOf(Stmt stmt) {
        return Views.toMappedSet(getOutEdgesOf(stmt), ICFGEdge::target);
    }

    @Override
    public Set<Stmt> getNodes() {
        return Collections.unmodifiableSet(stmtToCFG.keySet());
    }
}
