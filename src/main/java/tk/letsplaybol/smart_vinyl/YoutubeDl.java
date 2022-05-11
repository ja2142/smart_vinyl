package tk.letsplaybol.smart_vinyl;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.nio.charset.StandardCharsets;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.SystemUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class YoutubeDl {

    private static final Logger LOGGER = LogManager.getLogger();
    private static File youtubeDlPath;

    public static void getYoutubeDlBinary() {
        // TODO make this async?
        String youtubeDlBinName = (SystemUtils.IS_OS_WINDOWS ? "youtube-dl.exe" : "youtube-dl");
        youtubeDlPath = new File(SmartVinyl.MOD_ID + "/" + youtubeDlBinName);

        if (youtubeDlPath.exists()) {
            youtubeDlPath.setExecutable(true, true);
            LOGGER.info("youtube-dl binary exists, updating");
            try {
                runYoutubeDlCmd("--update");
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

    public static String runYoutubeDlCmd(String... args) throws IOException {
        Process proc = Runtime.getRuntime().exec(youtubeDlPath.getPath(), args);
        return IOUtils.toString(proc.getInputStream(), StandardCharsets.UTF_8);
    }

}
