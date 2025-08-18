package toyc.util;

import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Variable naming that reads existing numeric suffixes and increments them
 */
public class NumericSuffixNaming {

    // Pattern to match variable names ending with numbers
    private static final Pattern NUMERIC_SUFFIX_PATTERN = Pattern.compile("^(.+?)(\\d+)$");

    private final Set<String> existingNames;

    public NumericSuffixNaming(Set<String> existingNames) {
        this.existingNames = existingNames;
    }

    /**
     * Generate new variable name by incrementing numeric suffix or adding one
     */
    public String getNewVarName(String originalName) {
        Matcher matcher = NUMERIC_SUFFIX_PATTERN.matcher(originalName);

        String baseName;
        int startNumber;

        if (matcher.matches()) {
            // Variable already has numeric suffix: "var123" -> base="var", number=123
            baseName = matcher.group(1);
            startNumber = Integer.parseInt(matcher.group(2)) + 1;
        } else {
            // Variable has no numeric suffix: "var" -> base="var", number=1
            baseName = originalName;
            startNumber = 1;
        }

        // Find next available number
        String newName;
        int number = startNumber;
        do {
            newName = baseName + number;
            number++;
        } while (existingNames.contains(newName));

        existingNames.add(newName);
        return newName;
    }
}
