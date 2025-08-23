package toyc.algorithm.analysis.loop;

import toyc.ir.stmt.Stmt;

import javax.annotation.Nonnull;
import java.util.Set;

/**
 * Loop data structure
 */
public record Loop(Stmt header, Set<Stmt> tails, Set<Stmt> body) {

    public boolean contains(Stmt stmt) {
        return body.contains(stmt);
    }

    public int size() {
        return body.size();
    }

    @Override
    @Nonnull
    public String toString() {
        return String.format("Loop[header=%s, tail=%s, body=%s]",
                header, tails, body);
    }
}