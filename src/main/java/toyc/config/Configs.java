package toyc.config;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Objects;

/**
 * Static utility methods for config system.
 */
public final class Configs {

    private Configs() {
    }

    /**
     * File name of algorithm configuration. (including analyses and
     * optimizations)
     * TODO: the path of configuration file is hardcoded, make it configurable?
     */
    private static final String ANALYSES_CONFIG = "toyc-analyses.yml";
    private static final String OPTIMIZATIONS_CONFIG = "toyc-optimizations.yml";

    /**
     * @return the combined content of both algorithm configurations.
     * This merges analyses and optimizations configs into a single stream.
     */
    public static InputStream getAlgorithmConfig() {
        try {
            String analysesContent = readConfigAsString(ANALYSES_CONFIG);
            String optimizationsContent = readConfigAsString(OPTIMIZATIONS_CONFIG);

            // Combine the YAML content (assuming both are valid YAML documents)
            String combined = analysesContent + "\n\n" + optimizationsContent;

            return new ByteArrayInputStream(combined.getBytes(StandardCharsets.UTF_8));
        } catch (IOException e) {
            throw new RuntimeException("Failed to combine configuration files", e);
        }
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
    static boolean satisfyConditions(String conditions, AlgorithmOptions options) {
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

    /**
     * Helper method to read config file as string
     */
    private static String readConfigAsString(String configFile) throws IOException {
        try (InputStream is = Configs.class.getClassLoader().getResourceAsStream(configFile)) {
            if (is == null) {
                throw new IOException("Configuration file not found: " + configFile);
            }
            return new String(is.readAllBytes(), StandardCharsets.UTF_8);
        }
    }
}
