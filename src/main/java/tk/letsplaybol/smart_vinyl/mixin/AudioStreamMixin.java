package tk.letsplaybol.smart_vinyl.mixin;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import net.minecraft.client.audio.AudioStreamManager;
import net.minecraft.client.audio.IAudioStream;
import net.minecraft.util.ResourceLocation;
import tk.letsplaybol.smart_vinyl.AudioStreamVelvet;
import tk.letsplaybol.smart_vinyl.SmartVinyl;
import tk.letsplaybol.smart_vinyl.YoutubeCache;
import tk.letsplaybol.smart_vinyl.util.ResourceLocationCoder;

@Mixin(AudioStreamManager.class)
public class AudioStreamMixin {
    private static final Logger LOGGER = LogManager.getLogger();

    private String getOriginalStringFromLocation(ResourceLocation location) {
        Pattern pattern = Pattern.compile("[a-z0-9]+/([a-z0-9]+)\\..+");
        Matcher match = pattern.matcher(location.getPath());
        if (!match.find()) {
            String problem = "wrong ResourceLocation, expected format: .*/<encoded songname>.<extension>," +
                " probably sounds/<encoded>.ogg, got: " + location.getPath();
            LOGGER.error(problem);
            throw new IllegalArgumentException(problem);
        }
        return match.group(1);
    }

    @Inject(method = "getStream", at = @At("HEAD"), cancellable = true)
    public void preGetStream(ResourceLocation location, boolean looping,
            CallbackInfoReturnable<CompletableFuture<IAudioStream>> callback_info) {
        if (!location.getNamespace().equals(SmartVinyl.MOD_ID)) {
            return;
        }
        String origEncoded = getOriginalStringFromLocation(location);
        String songName = ResourceLocationCoder.locationToString(origEncoded);
        LOGGER.debug("getting stream for " + location + " (" + songName + ")");

        CompletableFuture<IAudioStream> future = CompletableFuture
                .supplyAsync(() -> {
                    try {
                        return new AudioStreamVelvet(YoutubeCache.getCache().getInputStream(songName), true);
                    } catch (InterruptedException | ExecutionException e) {
                        LOGGER.error("failed to get velvet stream for " + location, e);
                        return null;
                    }
                });
        callback_info.setReturnValue(future);
    }
}
