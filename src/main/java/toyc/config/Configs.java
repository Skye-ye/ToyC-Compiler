package toyc.config;

import java.io.InputStream;
import java.net.URL;
import java.util.Objects;

/**
 * Static utility methods for config system.
 */
public final class Configs {

    private Configs() {
    }

    /**
     * File name of analysis configuration.
     * TODO: the path of configuration file is hardcoded, make it configurable?
     */
    private static final String CONFIG = "toyc-analyses.yml";

    /**
     * @return the content of analysis configuration.
     */
    public static InputStream getAnalysisConfig() {
        return Configs.class
                .getClassLoader()
                .getResourceAsStream(CONFIG);
    }

    /**
     * @return the URL of analysis configuration.
     */
    public static URL getAnalysisConfigURL() {
        return Configs.class
                .getClassLoader()
                .getResource(CONFIG);
    }

    /**
     * Extracts analysis id from given require item.
     */
    static String extractId(String require) {
        int index = require.indexOf('(');
        return index == -1 ? require :
                require.substring(0, index);
    }

    /**
     * Extracts conditions (represented by a string) from given require item.
     */
    static String extractConditions(String require) {
        int index = require.indexOf('(');
        return index == -1 ? null :
                require.substring(index + 1, require.length() - 1);
    }

    /**
     * Checks if options satisfy the given conditions.
     * Examples of conditions:
     * a=b
     * a=b&amp;x=y
     * a=b|c|d&amp;x=y
     * TODO: comprehensive error handling for invalid conditions
     */
    static boolean satisfyConditions(String conditions, AnalysisOptions options) {
        if (conditions != null) {
            outer:
            for (String conds : conditions.split("&")) {
                String[] splits = conds.split("=");
                String key = splits[0];
                String value = splits[1];
                if (value.contains("|")) { // a=b|c
                    // Check each individual value, if one match,
                    // then this condition can be satisfied.
                    for (String v : value.split("\\|")) {
                        if (Objects.toString(options.get(key)).equals(v)) {
                            continue outer;
                        }
                    }
                    return false;
                } else if (!Objects.toString(options.get(key)).equals(value)) { // a=b
                    return false;
                }
            }
        }
        return true;
    }
}
