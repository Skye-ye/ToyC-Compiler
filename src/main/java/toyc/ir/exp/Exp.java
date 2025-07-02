package toyc.ir.exp;
import toyc.language.type.Type;

import java.io.Serializable;
import java.util.Set;

public interface Exp extends Serializable {

    /**
     * @return type of this expression.
     */
    Type getType();

    /**
     * @return a set of expressions which are used by (contained in) this Exp.
     */
    default Set<RValue> getUses() {
        return Set.of();
    }

    <T> T accept(ExpVisitor<T> visitor);
}