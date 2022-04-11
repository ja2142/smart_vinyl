package tk.letsplaybol.smart_vinyl;

import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.JukeboxBlock;
import net.minecraft.item.Item;
import net.minecraft.item.ItemGroup;
import net.minecraft.item.ItemStack;
import net.minecraft.item.ItemUseContext;
import net.minecraft.stats.Stats;
import net.minecraft.util.ActionResultType;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraftforge.fml.network.PacketDistributor;

public class DynamicDiscItem extends Item {

    public static final String name = "dynamic_disc";

    public DynamicDiscItem(Properties props) {
        super(props.stacksTo(1).tab(ItemGroup.TAB_MISC));
		setRegistryName(name);
    }

    public void playSound(BlockPos position, String songName){
        SmartVinylPlayPacket.NETWORK_CHANNEL.send(PacketDistributor.ALL.noArg(), new SmartVinylPlayPacket(position, songName));
    }

    @Override
    public ActionResultType useOn(ItemUseContext context) {
        World level = context.getLevel();
        BlockPos clickedPos = context.getClickedPos();
        BlockState usedOnBlockState = level.getBlockState(context.getClickedPos());
        if (usedOnBlockState.is(Blocks.JUKEBOX) && !usedOnBlockState.getValue(JukeboxBlock.HAS_RECORD)){
            if (!level.isClientSide()){
                ItemStack recordStack = context.getItemInHand();
                JukeboxBlock jukebox = (JukeboxBlock)usedOnBlockState.getBlock();
                jukebox.setRecord(level, clickedPos, usedOnBlockState, recordStack);

                // TODO actually play sound
                playSound(clickedPos, "Darude - Sandstorm");

                recordStack.shrink(1);

                context.getPlayer().awardStat(Stats.PLAY_RECORD);
            }
            return ActionResultType.sidedSuccess(level.isClientSide());
        } else {
            return ActionResultType.PASS;
        }
    }

}
