package toyc.util.collection;

import javax.annotation.Nonnull;
import java.io.Serializable;

public record Pair<T1, T2>(T1 first, T2 second)
        implements Serializable {

    @Override
    @Nonnull
    public String toString() {
        return "<" + first + ", " + second + ">";
    }
}
