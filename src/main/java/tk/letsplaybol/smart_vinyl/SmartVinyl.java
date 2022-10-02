package tk.letsplaybol.smart_vinyl;

import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.core.config.Configurator;

import com.zakgof.velvetvideo.ISeekableInput;
import com.zakgof.velvetvideo.VelvetVideoException;
import com.zakgof.velvetvideo.impl.VelvetVideoLib;

import net.minecraft.item.Item;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.RegistryEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;

// The value here should match an entry in the META-INF/mods.toml file
@Mod(SmartVinyl.MOD_ID)
public class SmartVinyl {
    public static final String MOD_ID = "smart_vinyl";
    // Directly reference a log4j logger.
    private static final Logger LOGGER = LogManager.getLogger();

    public SmartVinyl() {
        // Register the setup method for modloading
        FMLJavaModLoadingContext.get().getModEventBus().addListener(this::setup);

        // Register ourselves for server and other game events we are interested in
        MinecraftForge.EVENT_BUS.register(this);
    }

    private void silenceVelvetVideo() {
        Configurator.setLevel("velvet-video", Level.INFO);
    }

    public static void loadVelvetVideo() {
        LOGGER.info("loading velvet-video");        
        try {
            VelvetVideoLib.getInstance().demuxer(new ISeekableInput() {

                @Override
                public void close() {
                }

                @Override
                public int read(byte[] arg0) {
                    return 0;
                }

                @Override
                public void seek(long arg0) {
                }

                @Override
                public long size() {
                    return 0;
                }
            });
        } catch (VelvetVideoException e) {
            // failure is expected for empty stream, but at this point libraries should be
            // extracted
        }
        LOGGER.info("done loading velvet-video");        
    }

    private void setup(final FMLCommonSetupEvent event) {
        silenceVelvetVideo();

        SmartVinylPlayPacket.registerPackets();
        YoutubeDl.getYoutubeDlBinary();

        YoutubeCache.getCache().cleanup();
        loadVelvetVideo();
    }

    // You can use EventBusSubscriber to automatically subscribe events on the
    // contained class (this is subscribing to the MOD
    // Event bus for receiving Registry Events)
    @Mod.EventBusSubscriber(bus = Mod.EventBusSubscriber.Bus.MOD)
    public static class RegistryEvents {
        @SubscribeEvent
        public static void onItemRegistry(final RegistryEvent.Register<Item> itemRegistryEvent) {
            // Register a new block here
            LOGGER.info("registering items");
            itemRegistryEvent.getRegistry().register(new DynamicDiscItem(new Item.Properties()));
        }
    }
}
