package tk.letsplaybol.smart_vinyl;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.net.URLConnection;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.SystemUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import tk.letsplaybol.smart_vinyl.util.AsyncIOExecutorProvider;
import tk.letsplaybol.smart_vinyl.util.ResourceLocationCoder;

public class YoutubeDl {

    private static final Logger LOGGER = LogManager.getLogger();
    private static File youtubeDlPath;

    public static void getYoutubeDlBinary() {
        CompletableFuture.runAsync(
                () -> downloadOrUpdateYoutubeDl(),
                AsyncIOExecutorProvider.getExecutor());
    }

    private static void downloadOrUpdateYoutubeDl() {
        // TODO make this async?
        String youtubeDlBinName = (SystemUtils.IS_OS_WINDOWS ? "youtube-dl.exe" : "youtube-dl");
        youtubeDlPath = new File(SmartVinyl.MOD_ID + "/" + youtubeDlBinName);

        if (youtubeDlPath.exists()) {
            youtubeDlPath.setExecutable(true, true);
            LOGGER.info("youtube-dl binary exists, updating");
            try {
                LOGGER.info("youtube update result: " + runCommand(youtubeDlPath.getPath(), "--update"));
            } catch (IOException e) {
                LOGGER.error("youtube-dl update failed", e);
            }
        } else {
            LOGGER.info("youtube-dl binary doesn't exists, downloading");
            try {
                URL youtubeDlDownloadURL = new URL(
                        SystemUtils.IS_OS_WINDOWS ? "https://yt-dl.org/downloads/latest/youtube-dl.exe"
                                : "https://yt-dl.org/downloads/latest/youtube-dl");

                FileUtils.copyURLToFile(youtubeDlDownloadURL, youtubeDlPath);
                youtubeDlPath.setExecutable(true, true);
            } catch (IOException e) {
                LOGGER.error("youtube-dl download failed", e);
            }
        }
    }

    public static String runCommand(String... args) throws IOException {
        Process proc = Runtime.getRuntime().exec(args);
        String output = IOUtils.toString(proc.getInputStream(), StandardCharsets.UTF_8);
        try {
            proc.waitFor();
        } catch (InterruptedException e) {
            throw new IOException(e);
        }
        if (proc.exitValue() != 0) {
            String error = IOUtils.toString(proc.getErrorStream(), StandardCharsets.UTF_8);
            LOGGER.error("youtube-dl failed with: " + error);
            throw new IOException(error);
        }
        return output;
    }

    public static JsonObject getYoutubeDlOutput(String trackName) throws IOException {
        LOGGER.info("searching for " + trackName);
        // e.g. youtube-dl --dump-json --format bestaudio 'ytsearch:Darude - Sandstorm'
        String ytDlOutput = runCommand(
                youtubeDlPath.getPath(),
                "--dump-json",
                "--format", "bestaudio",
                String.format("ytsearch:%s", trackName));
        return new JsonParser().parse(ytDlOutput).getAsJsonObject();
    }

    public static FileBufferedInputStream getYoutubeStream(String trackName) throws IOException {
        JsonObject ytOutputJson = getYoutubeDlOutput(trackName);

        LOGGER.info("found " + ytOutputJson.get("title").getAsString());

        JsonObject headers = ytOutputJson.get("http_headers").getAsJsonObject();
        String url = ytOutputJson.get("url").getAsString();

        LOGGER.info("opening connection");

        URLConnection connection = new URL(url).openConnection();
        for (Map.Entry<String, JsonElement> header : headers.entrySet()) {
            connection.setRequestProperty(header.getKey(), header.getValue().getAsString());
        }

        FileBufferedInputStream seekable = new FileBufferedInputStream(connection.getInputStream(),
                ResourceLocationCoder.getTrackPath(trackName),
                connection.getContentLength());

        return seekable;
    }

}
