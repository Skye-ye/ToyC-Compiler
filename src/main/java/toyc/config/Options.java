package toyc.config;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.fasterxml.jackson.dataformat.yaml.YAMLGenerator;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import toyc.WorldBuilder;
import picocli.CommandLine;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;
import picocli.CommandLine.Parameters;

import java.io.File;
import java.io.IOException;
import java.io.Serializable;
import java.nio.file.Path;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Predicate;

/**
 * Option class for Tai-e.
 * We name this class in the plural to avoid name collision with {@link Option}.
 */
@Command(name = "Options",
        description = "Toyc options",
        usageHelpWidth = 120
)
public class Options implements Serializable {

    private static final Logger logger = LogManager.getLogger(Options.class);

    private static final String OPTIONS_FILE = "options.yml";

    private static final String DEFAULT_OUTPUT_DIR = "output";

    // ---------- file-based options ----------
    @JsonProperty
    @Option(names = "--options-file",
            description = "The options file")
    private File optionsFile;

    // ---------- information options ----------
    @JsonProperty
    @Option(names = {"-h", "--help"},
            description = "Display this help message",
            defaultValue = "false",
            usageHelp = true)
    private boolean printHelp;

    public boolean isPrintHelp() {
        return printHelp;
    }

    public void printHelp() {
        CommandLine cmd = new CommandLine(this);
        cmd.setUsageHelpLongOptionsMaxWidth(30);
        cmd.usage(System.out);
    }

    // ---------- program options ----------
    @JsonProperty
    @Parameters(index = "0..*",
            description = "One or more source files to compile.",
            paramLabel = "SOURCE-FILE")
    private List<String> inputFiles = List.of(); // Changed from List<File>

    public List<String> getInputFiles() {
        return inputFiles;
    }

    // ---------- general analysis options ----------
    @JsonProperty
    @Option(names = "--world-builder",
            description = "Specify world builder class (default: ${DEFAULT-VALUE})",
            defaultValue = "toyc.frontend.ToyCWorldBuilder")
    private Class<? extends WorldBuilder> worldBuilderClass;

    public Class<? extends WorldBuilder> getWorldBuilderClass() {
        return worldBuilderClass;
    }

    @JsonProperty
    @JsonSerialize(using = OutputDirSerializer.class)
    @JsonDeserialize(using = OutputDirDeserializer.class)
    @Option(names = "--output-dir",
            description = "Specify output directory (default: ${DEFAULT-VALUE})"
                    + ", '" + PlaceholderAwareFile.AUTO_GEN + "' can be used as a placeholder"
                    + " for an automatically generated timestamp",
            defaultValue = DEFAULT_OUTPUT_DIR,
            converter = OutputDirConverter.class)
    private File outputDir;

    public File getOutputDir() {
        return outputDir;
    }

    @JsonProperty
    @Option(names = "--pre-build-ir",
            description = "Build IR for all available methods before" +
                    " starting any analysis (default: ${DEFAULT-VALUE})",
            defaultValue = "false")
    private boolean preBuildIR;

    public boolean isPreBuildIR() {
        return preBuildIR;
    }

    @JsonProperty
    @Option(names = {"-wc", "--world-cache-mode"},
            description = "Enable world cache mode to save build time"
                    + " by caching the completed built world to the disk.",
            defaultValue = "false")
    private boolean worldCacheMode;

    public boolean isWorldCacheMode() {
        return worldCacheMode;
    }

    @JsonProperty
    @Option(names = "-scope",
            description = "Scope for method/class analyses (default: ${DEFAULT-VALUE}," +
                    " valid values: ${COMPLETION-CANDIDATES})",
            defaultValue = "REACHABLE")
    private Scope scope;

    public Scope getScope() {
        return scope;
    }

    // ---------- specific analysis options ----------
    @JsonProperty
    @Option(names = {"-p", "--plan-file"},
            description = "The analysis plan file")
    private File planFile;

    public File getPlanFile() {
        return planFile;
    }

    @JsonProperty
    @Option(names = {"-a", "--analysis"},
            description = "Analyses to be executed",
            paramLabel = "<analysisID[=<options>]>",
            mapFallbackValue = "")
    private Map<String, String> analyses = Map.of();

    public Map<String, String> getAnalyses() {
        return analyses;
    }

    @JsonProperty
    @Option(names = {"-g", "--gen-plan-file"},
            description = "Merely generate analysis plan",
            defaultValue = "false")
    private boolean onlyGenPlan;

    public boolean isOnlyGenPlan() {
        return onlyGenPlan;
    }

    @JsonProperty
    @Option(names = {"-kr", "--keep-result"},
            description = "The analyses whose results are kept" +
                    " (multiple analyses are split by ',', default: ${DEFAULT-VALUE})",
            split = ",", paramLabel = "<analysisID>",
            defaultValue = Plan.KEEP_ALL)
    private Set<String> keepResult;

    public Set<String> getKeepResult() {
        return keepResult;
    }

    /**
     * Parses arguments and return the parsed and post-processed Options.
     */
    public static Options parse(String... args) {
        Options options = CommandLine.populateCommand(new Options(), args);
        return postProcess(options);
    }

    /**
     * Validates input options and do some post-process on it.
     *
     * @return the Options object after post-process.
     */
    private static Options postProcess(Options options) {
        // If help is requested, skip validation and return immediately
        if (options.isPrintHelp()) {
            return options;
        }
        
        if (options.optionsFile != null) {
            // If options file is given, we ignore other options,
            // and instead read options from the file.
            options = readRawOptions(options.optionsFile);
        }
        if (!options.analyses.isEmpty() && options.planFile != null) {
            // The user should choose either options or plan file to
            // specify analyses to be executed.
            throw new ConfigException("Conflict options: " +
                    "--analysis and --plan-file should not be used simultaneously");
        }
        if (options.getInputFiles() == null || options.getInputFiles().isEmpty()) {
            throw new ConfigException("Missing source file");
        }
        // mkdir for output dir
        if (!options.outputDir.exists()) {
            options.outputDir.mkdirs();
        }
        logger.info("Output directory: {}",
                options.outputDir.getAbsolutePath());
        // write options to file for future reviewing and issue submitting
        writeOptions(options, new File(options.outputDir, OPTIONS_FILE));
        return options;
    }

    /**
     * Reads options from file.
     * Note: the returned options have not been post-processed.
     */
    private static Options readRawOptions(File file) {
        ObjectMapper mapper = new ObjectMapper(new YAMLFactory());
        try {
            return mapper.readValue(file, Options.class);
        } catch (IOException e) {
            throw new ConfigException("Failed to read options from " + file, e);
        }
    }

    /**
     * Writes options to given file.
     */
    private static void writeOptions(Options options, File output) {
        ObjectMapper mapper = new ObjectMapper(
                new YAMLFactory()
                        .disable(YAMLGenerator.Feature.WRITE_DOC_START_MARKER)
                        .disable(YAMLGenerator.Feature.SPLIT_LINES)
                        .enable(YAMLGenerator.Feature.MINIMIZE_QUOTES));
        try {
            logger.info("Writing options to {}", output.getAbsolutePath());
            mapper.writeValue(output, options);
        } catch (IOException e) {
            throw new ConfigException("Failed to write options to "
                    + output.getAbsolutePath(), e);
        }
    }

    /**
     * Represents a file that supports placeholder and automatically replaces it
     * with current timestamp values. This class extends the standard File class.
     */
    private static class PlaceholderAwareFile extends File {

        /**
         * The placeholder for an automatically generated timestamp.
         */
        private static final String AUTO_GEN = "$AUTO-GEN";

        private final String rawPathname;

        public PlaceholderAwareFile(String pathname) {
            super(resolvePathname(pathname));
            this.rawPathname = pathname;
        }

        public String getRawPathname() {
            return rawPathname;
        }

        private static String resolvePathname(String pathname) {
            if (pathname.contains(AUTO_GEN)) {
                String timestamp = DateTimeFormatter.ofPattern("yyyyMMddHHmmss")
                        .withZone(ZoneId.systemDefault())
                        .format(Instant.now());
                pathname = pathname.replace(AUTO_GEN, timestamp);
                // check if the output dir already exists
                File file = Path.of(pathname).toAbsolutePath().normalize().toFile();
                if (file.exists()) {
                    throw new RuntimeException("The generated file already exists, "
                            + "please wait for a second to start again: " + pathname);
                }
            }
            return Path.of(pathname).toAbsolutePath().normalize().toString();
        }

    }

    /**
     * @see #outputDir
     */
    private static class OutputDirConverter implements CommandLine.ITypeConverter<File> {
        @Override
        public File convert(String outputDir) {
            return new PlaceholderAwareFile(outputDir);
        }
    }

    /**
     * Serializer for raw {@link #outputDir}.
     */
    private static class OutputDirSerializer extends JsonSerializer<File> {
        @Override
        public void serialize(File value, JsonGenerator gen,
                              SerializerProvider serializers) throws IOException {
            if (value instanceof PlaceholderAwareFile file) {
                gen.writeString(toSerializedFilePath(file.getRawPathname()));
            } else {
                throw new RuntimeException("Unexpected type: " + value);
            }
        }
    }

    /**
     * Deserializer for {@link #outputDir}.
     */
    private static class OutputDirDeserializer extends JsonDeserializer<File> {

        @Override
        public File deserialize(JsonParser p, DeserializationContext ctxt)
                throws IOException {
            return new PlaceholderAwareFile(p.getValueAsString());
        }
    }

    /**
     * Converter for classpath with system path separator.
     */
    private static class ClassPathConverter implements CommandLine.ITypeConverter<List<String>> {
        @Override
        public List<String> convert(String value) {
            return Arrays.stream(value.split(File.pathSeparator))
                    .map(String::trim)
                    .filter(Predicate.not(String::isEmpty))
                    .toList();
        }
    }

    /**
     * Serializer for file path. Ensures a path is serialized as a relative path
     * from the working directory rather than an absolute path, thus
     * preserving the portability of the dumped options file.
     */
    private static class FilePathSerializer extends JsonSerializer<String> {
        @Override
        public void serialize(String value, JsonGenerator gen,
                              SerializerProvider serializers) throws IOException {
            gen.writeString(toSerializedFilePath(value));
        }
    }

    /**
     * Convert a file to a relative path using the "/" (forward slash)
     * from the working directory, thus preserving the portability of
     * the dumped options file.
     *
     * @param file the file to be processed
     * @return a relative path from the working directory
     */
    private static String toSerializedFilePath(String file) {
        Path workingDir = Path.of("").toAbsolutePath();
        Path path = Path.of(file).toAbsolutePath().normalize();
        return workingDir.relativize(path).toString()
                .replace('\\', '/');
    }

    @Override
    public String toString() {
        return "Options{" +
                "optionsFile=" + optionsFile +
                ", printHelp=" + printHelp +
                ", inputFiles=" + inputFiles +
                ", worldBuilderClass=" + worldBuilderClass +
                ", outputDir='" + outputDir + '\'' +
                ", preBuildIR=" + preBuildIR +
                ", worldCacheMode=" + worldCacheMode +
                ", scope=" + scope +
                ", planFile=" + planFile +
                ", analyses=" + analyses +
                ", onlyGenPlan=" + onlyGenPlan +
                ", keepResult=" + keepResult +
                '}';
    }
}
