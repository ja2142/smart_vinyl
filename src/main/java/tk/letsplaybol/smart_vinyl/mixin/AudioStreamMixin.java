package tk.letsplaybol.smart_vinyl.mixin;

import java.util.concurrent.CompletableFuture;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import net.minecraft.client.audio.AudioStreamBuffer;
import net.minecraft.client.audio.AudioStreamManager;
import net.minecraft.client.gui.IngameGui;
import net.minecraft.item.DyeItem;
import net.minecraft.item.Item;
import net.minecraft.item.ItemUseContext;
import net.minecraft.util.ActionResultType;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.datafix.fixes.JukeboxRecordItem;
import net.minecraft.util.text.ITextComponent;
import tk.letsplaybol.smart_vinyl.SmartVinyl;

//@Mixin(AudioStreamManager.class)
@Mixin(IngameGui.class)
public class AudioStreamMixin {
    private static final Logger LOGGER = LogManager.getLogger();

    // doesn't work as well (with IngameGui), but at least breakpoint is marked as red (wheras for AudioStreamManager.getStream it's inactive)
    //@Inject(method = "Lnet/minecraft/client/gui/IngameGui;setNowPlaying(Lnet/minecraft/util/text/ITextComponent)V", at = @At("HEAD"))
    @Inject(method = "setNowPlaying", at = @At("HEAD"))
    public void preSetNowPlaying(ITextComponent text, CallbackInfo callback_info){
        LOGGER.fatal("at mixin");
        LOGGER.error("at mixin");
        LOGGER.info("at mixin");
        LOGGER.debug("at mixin");
        LOGGER.info("at mixin");
        LOGGER.error("at mixin");
        LOGGER.fatal("at mixin");
    }

    // TODO try for other class?
    // @Inject(method = "getStream", at = @At("HEAD"), cancellable = true, remap = false)
    // public void preGetStream(ResourceLocation location, boolean looping,
    //     CallbackInfoReturnable<CompletableFuture<AudioStreamBuffer>> callback_info){
    //     LOGGER.debug("getting stream for " + location);
    //     if(!location.getNamespace().equals(SmartVinyl.MOD_ID)){
    //         return;
    //     }
    //     LOGGER.debug("getting stream for " + location);

    // }
}
