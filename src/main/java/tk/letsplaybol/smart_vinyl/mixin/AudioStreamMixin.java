package tk.letsplaybol.smart_vinyl.mixin;

import java.util.concurrent.CompletableFuture;

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

@Mixin(AudioStreamManager.class)
public class AudioStreamMixin {
    private static final Logger LOGGER = LogManager.getLogger();

    @Inject(method = "getStream", at = @At("HEAD"), cancellable = true)
    public void preGetStream(ResourceLocation location, boolean looping,
        CallbackInfoReturnable<CompletableFuture<IAudioStream>> callback_info){
        if(!location.getNamespace().equals(SmartVinyl.MOD_ID)) {
            return;
        }
        LOGGER.debug("====================================");
        LOGGER.debug("getting stream for " + location);
        LOGGER.debug("====================================");

        CompletableFuture<IAudioStream> future = CompletableFuture.supplyAsync(() -> new AudioStreamVelvet());
        callback_info.setReturnValue(future);
    }
}
