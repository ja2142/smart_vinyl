package tk.letsplaybol.smart_vinyl;

import java.util.function.Supplier;

import net.minecraft.network.PacketBuffer;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.BlockPos;
import net.minecraftforge.fml.network.NetworkEvent;
import net.minecraftforge.fml.network.NetworkRegistry;
import net.minecraftforge.fml.network.simple.SimpleChannel;
import tk.letsplaybol.smart_vinyl.util.Utf8Coder;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class SmartVinylPlayPacket {
    private static final Logger LOGGER = LogManager.getLogger();

    private static final String PROTOCOL_VERSION = "1";
    public static final SimpleChannel NETWORK_CHANNEL = NetworkRegistry.newSimpleChannel(
        new ResourceLocation(SmartVinyl.MOD_ID, "play_packet"),
        () -> PROTOCOL_VERSION,
        PROTOCOL_VERSION::equals,
        PROTOCOL_VERSION::equals
    );

    private BlockPos pos;
    private String songName;

    public SmartVinylPlayPacket() {
    }

    public SmartVinylPlayPacket(BlockPos pos, String songName) {
        this.pos = pos;
        this.songName = songName;
    }

    public static void registerPackets(){
        NETWORK_CHANNEL.registerMessage(0, SmartVinylPlayPacket.class,
            SmartVinylPlayPacket::encode,
            SmartVinylPlayPacket::decode,
            SmartVinylPlayPacket::handle
            );
    }

    public static SmartVinylPlayPacket decode(PacketBuffer buffer) {
        SmartVinylPlayPacket message = new SmartVinylPlayPacket();
        message.pos = buffer.readBlockPos();
        message.songName = Utf8Coder.utf8decode(buffer.readByteArray());
        return message;
    }

    public static void encode(SmartVinylPlayPacket message, PacketBuffer buffer) {
        buffer.writeBlockPos(message.pos);
        buffer.writeByteArray(Utf8Coder.utf8encode(message.songName));
    }

    public static void handle(SmartVinylPlayPacket message, Supplier<NetworkEvent.Context> ctx) {
        LOGGER.debug("handling SmartVinylPlayPacket: \"" + message.songName + "\" at " + message.pos);
    }

}
