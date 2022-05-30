package tk.letsplaybol.smart_vinyl;

import java.io.File;
import java.io.IOError;
import java.io.IOException;
import java.nio.file.Files;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

import com.zakgof.velvetvideo.ISeekableInput;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import tk.letsplaybol.smart_vinyl.util.AsyncIOExecutorProvider;
import tk.letsplaybol.smart_vinyl.util.ResourceLocationCoder;

public class YoutubeCache {
    public static YoutubeCache instance;

    private static final Logger LOGGER = LogManager.getLogger();

    private static final long MAX_CACHE_SIZE_BYTES = 128 * 1024 * 1024;

    private CacheDb cacheDb = new CacheDb();

    private Map<String, Future<FileBufferedInputStream>> startedLookups = new HashMap<>();
    private Map<String, FileBufferedInputStream> startedDownloads = new HashMap<>();

    public static YoutubeCache getCache() {
        if (instance == null) {
            instance = new YoutubeCache();
        }
        return instance;
    }

    private YoutubeCache() {
        try {
            Files.createDirectories(ResourceLocationCoder.CACHE_PATH);
        } catch (IOError | IOException e) {
            LOGGER.debug("couldn't create cache directory (possibly because it already exists)");
        }
    }

    public void startDownload(String songName) {
        startDownload(songName, false);
    }

    public synchronized void startDownload(String songName, boolean force) {
        if (startedLookups.containsKey(songName) || startedDownloads.containsKey(songName)) {
            LOGGER.debug("download for " + songName + " already started");
            // download/lookup is already in progress or finished
            return;
        }

        if (cacheDb.has(songName) && !force) {
            LOGGER.debug("not starting download for " + songName + ", already in db");
            return;
        }

        LOGGER.debug("starting download for " + songName);
        startedLookups.put(songName, CompletableFuture.supplyAsync(() -> {
            try {
                return YoutubeDl.getYoutubeStream(songName);
            } catch (IOException e) {
                LOGGER.error("lookup failed", e);
                return null;
            }
        }, AsyncIOExecutorProvider.getExecutor()));
    }

    public ISeekableInput getInputStream(String songName) throws InterruptedException, ExecutionException {
        LOGGER.debug("getting stream for " + songName);
        Future<FileBufferedInputStream> streamFuture;
        synchronized (this) {
            if (cacheDb.has(songName)) {
                LOGGER.debug(songName + " already downloaded");
                try {
                    return new FileBufferedInputStream(ResourceLocationCoder.getTrackPath(songName));
                } catch (IOException e) {
                    LOGGER.warn("failed to get file for " + songName);
                }
            }

            if (startedDownloads.containsKey(songName)) {
                LOGGER.debug(songName + " download already started");
                return startedDownloads.get(songName).slice();
            }

            if (!startedLookups.containsKey(songName)) {
                LOGGER.debug(songName + " lookup not started yet, starting");
                startDownload(songName, true);
            }

            streamFuture = startedLookups.get(songName);
        }

        LOGGER.debug("waiting for " + songName);
        FileBufferedInputStream input = streamFuture.get();
        LOGGER.debug("done waiting");

        synchronized (this) {
            startedDownloads.put(songName, input);
        }
        input.downloadAsync().thenRun(() -> {
            downloadCompleted(songName);
        });

        return input.slice();
    }

    // remove partially downloaded, and shrink cache to size if needed
    public void cleanup() {
        // TODO async?
        shrinkToSize();
        cleanupOrphans();
    }

    public void cleanupOrphans() {
        LOGGER.info("cleaning orphans");
        File cacheDir = ResourceLocationCoder.CACHE_PATH.toFile();
        for (File file : cacheDir.listFiles()) {
            String toRemoveFileName = file.getName();
            String toRemoveSongName = ResourceLocationCoder.locationToString(toRemoveFileName);
            if (!cacheDb.has(toRemoveSongName)) {
                LOGGER.debug("removing " + toRemoveFileName + " (not in db)");
                try {
                    file.delete();
                } catch (IOError e) {
                    LOGGER.warn("couldn't remove file not recorded in db: " + toRemoveFileName);
                }
            }
        }
        LOGGER.info("done clearing orphans");
    }

    public void shrinkToSize() {
        LOGGER.info("shrinking cache");
        while (cacheDb.getSize() > MAX_CACHE_SIZE_BYTES) {
            String toRemove = cacheDb.removeLeastRecentlyUsed();
            LOGGER.debug("removing " + toRemove + " (cache shrinking)");
            try {
                ResourceLocationCoder.getTrackPath(toRemove).toFile().delete();
            } catch (IOError e) {
                LOGGER.warn("couldn't remove " + toRemove + ", was cache modified externally?");
            }
        }
        LOGGER.info("done shrinking cache");
    }

    private void downloadCompleted(String songName) {
        // write that song to something persistent?
        FileBufferedInputStream stream = startedDownloads.remove(songName);
        cacheDb.add(songName, stream.size());
    }
}
