package tk.letsplaybol.smart_vinyl;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

import javax.xml.bind.DatatypeConverter;

import com.zakgof.velvetvideo.ISeekableInput;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class YoutubeCache {
    public static YoutubeCache cache = new YoutubeCache();

    private static final Logger LOGGER = LogManager.getLogger();

    private Map<String, Future<FileBufferedInputStream>> startedLookups = new HashMap<>();
    private Map<String, FileBufferedInputStream> startedDownloads = new HashMap<>();
    private Map<String, FileBufferedInputStream> finishedDownloads = new HashMap<>();

    public synchronized void startDownload(String songName) {
        if (startedLookups.containsKey(songName) || startedDownloads.containsKey(songName)
                || finishedDownloads.containsKey(songName)) {
            LOGGER.debug("download for " + songName + " already started");
            // download/lookup is already in progress or finished
            return;
        }

        LOGGER.debug("starting download for " + songName);
        startedLookups.put(songName, CompletableFuture.supplyAsync(() -> {
            try {
                return YoutubeDl.getYoutubeStream(songName);
            } catch (IOException e) {
                LOGGER.debug("lookup failed", e);
                return null;
            }
        }));
    }

    public ISeekableInput getInputStream(String songName) throws InterruptedException, ExecutionException {
        LOGGER.debug("getting stream for " + songName);
        Future<FileBufferedInputStream> streamFuture;
        synchronized (this) {
            if (finishedDownloads.containsKey(songName)) {
                LOGGER.debug(songName + " already downloaded");
                // TODO to also make files downloaded in other sessions work, there has to be
                // some conversion from File to ISeekableInput
                return finishedDownloads.get(songName).slice();
            }

            if (startedDownloads.containsKey(songName)) {
                LOGGER.debug(songName + " download already started");
                return startedDownloads.get(songName).slice();
            }

            if (!startedLookups.containsKey(songName)) {
                LOGGER.debug(songName + " lookup not started yet, starting");
                startDownload(songName);
            }

            streamFuture = startedLookups.get(songName);
        }

        LOGGER.debug("waiting for " + songName);
        FileBufferedInputStream input = streamFuture.get();
        LOGGER.debug("done waiting");

        synchronized(this){
            startedDownloads.put(songName, input);
        }
        input.downloadAsync().thenRun(() -> {
            downloadCompleted(songName);
        });

        return input.slice();
    }

    private void downloadCompleted(String songName) {
        // write that song to something persistent?
        FileBufferedInputStream stream = startedDownloads.remove(songName);
        finishedDownloads.put(songName, stream);
    }
}
