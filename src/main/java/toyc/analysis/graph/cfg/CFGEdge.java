package toyc.analysis.graph.cfg;

import toyc.util.Hashes;
import toyc.util.graph.AbstractEdge;

/**
 * Represents CFG edges.
 *
 * @param <N> type of CFG nodes.
 */
public class CFGEdge<N> extends AbstractEdge<N> {

    public enum Kind {

        /**
         * Edge from entry node to real start node.
         */
        ENTRY,

        /**
         * Edge kind for fall-through to next statement.
         */
        FALL_THROUGH,

        /**
         * Edge kind for goto statements.
         */
        GOTO,

        /**
         * Edge kind for if statements when condition is true.
         */
        IF_TRUE,

        /**
         * Edge kind for if statements when condition is false.
         */
        IF_FALSE,

        /**
         * Edge kind for return statements.
         * These edges always go to the exit node of the CFG.
         */
        RETURN,
    }

    private final Kind kind;

    CFGEdge(Kind kind, N source, N target) {
        super(source, target);
        this.kind = kind;
    }

    /**
     * @return the kind of the edge.
     * @see Kind
     */
    public Kind getKind() {
        return kind;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        CFGEdge<?> edge = (CFGEdge<?>) o;
        return kind == edge.kind &&
                source.equals(edge.source) &&
                target.equals(edge.target);
    }

    @Override
    public int hashCode() {
        return Hashes.hash(kind, source, target);
    }

    @Override
    public String toString() {
        return "[" + kind + "]: " + source + " -> " + target;
    }
}
