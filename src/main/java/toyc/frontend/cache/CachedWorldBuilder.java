package toyc.frontend.cache;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import toyc.World;
import toyc.WorldBuilder;
import toyc.config.AnalysisConfig;
import toyc.config.Options;
import toyc.util.Timer;

import java.io.*;
import java.util.List;

/**
 * A {@link WorldBuilder} that loads the cached world if it exists, or delegates to the
 * underlying {@link WorldBuilder} otherwise.
 */
public class CachedWorldBuilder implements WorldBuilder {

    private static final Logger logger = LogManager.getLogger(CachedWorldBuilder.class);

    private static final String CACHE_DIR = "cache";

    private final WorldBuilder delegate;

    public CachedWorldBuilder(WorldBuilder delegate) {
        this.delegate = delegate;
        logger.info("The world cache mode is enabled.");
    }

    public static File getWorldCacheFile(Options options) {
        File cacheDir = new File(CACHE_DIR);
        if (!cacheDir.exists()) {
            cacheDir.mkdirs();
        }
        return new File(cacheDir,
                "world-cache-" + getWorldCacheHash(options) + ".bin").getAbsoluteFile();
    }

    private static int getWorldCacheHash(Options options) {
        int result = options.getInputFile().hashCode();
        result = 31 * result + (options.getWorldBuilderClass() != null
                ? options.getWorldBuilderClass().getName().hashCode() : 0);
        result = 31 * result + (options.isPreBuildIR() ? 1 : 0);
        result = 31 * result + (options.isWorldCacheMode() ? 1 : 0);
        // add the timestamp to the cache key calculation
        String path = options.getInputFile();
        File file = new File(path);
        if (file.exists()) {
            result = 31 * result + (int) file.lastModified();
        }
        result = Math.abs(result);
        return result;
    }

    @Override
    public void build(Options options, List<AnalysisConfig> analyses) {
        if (!options.isWorldCacheMode()) {
            logger.error("Using CachedWorldBuilder,"
                    + " but world cache mode option is not enabled");
            System.exit(-1);
        }
        File worldCacheFile = getWorldCacheFile(options);
        if (loadCache(options, worldCacheFile)) {
            return;
        }
        runWorldBuilder(options, analyses);
        saveCache(worldCacheFile);
    }

    private boolean loadCache(Options options, File worldCacheFile) {
        if (!worldCacheFile.exists()) {
            logger.info("World cache not found in {}", worldCacheFile);
            return false;
        }
        logger.info("Loading the world cache from {}", worldCacheFile);
        Timer timer = new Timer("Load the world cache");
        timer.start();
        try (ObjectInputStream ois = new ObjectInputStream(
                new BufferedInputStream(new FileInputStream(worldCacheFile)))) {
            World world = (World) ois.readObject();
            World.set(world);
            world.setOptions(options);
            return true;
        } catch (Exception e) {
            logger.error("Failed to load world cache from {} due to {}",
                    worldCacheFile, e);
        } finally {
            timer.stop();
            logger.info(timer);
        }
        return false;
    }

    private void runWorldBuilder(Options options, List<AnalysisConfig> analyses) {
        logger.info("Running the WorldBuilder ...");
        Timer timer = new Timer("Run the WorldBuilder");
        timer.start();
        delegate.build(options, analyses);
        timer.stop();
        logger.info(timer);
    }

    private void saveCache(File worldCacheFile) {
        logger.info("Saving the world cache to {}", worldCacheFile);
        Timer timer = new Timer("Save the world cache");
        timer.start();
        try (ObjectOutputStream oos = new ObjectOutputStream(
                new BufferedOutputStream(new FileOutputStream(worldCacheFile)))) {
            oos.writeObject(World.get());
        } catch (Exception e) {
            logger.error("Failed to save world cache from {} due to {}",
                    worldCacheFile, e);
        } finally {
            timer.stop();
            logger.info(timer);
        }
    }
}
