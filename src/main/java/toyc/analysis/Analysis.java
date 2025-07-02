package toyc.analysis;

/**
 * Abstract base class for all analyses.
 */
public abstract class Analysis {

    /**
     * Configuration of this analysis.
     */
    private final String id;

    // private boolean isStoreResult;

    protected Analysis(String id) {
        this.id = id;
    }

    public String getId() {
        return id;
    }
}
