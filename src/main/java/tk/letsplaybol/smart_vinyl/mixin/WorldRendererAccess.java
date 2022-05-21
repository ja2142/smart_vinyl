package tk.letsplaybol.smart_vinyl.mixin;

import java.util.Map;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
import org.spongepowered.asm.mixin.gen.Invoker;

import net.minecraft.client.audio.ISound;
import net.minecraft.client.renderer.WorldRenderer;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

@Mixin(WorldRenderer.class)
public interface WorldRendererAccess {
    @Accessor
    Map<BlockPos, ISound> getPlayingRecords();

    @Accessor
    ClientWorld getLevel();

    @Invoker
    void callNotifyNearbyEntities(World world, BlockPos blockPos, boolean bl);
}
