package tk.letsplaybol.smart_vinyl;

import net.minecraft.client.audio.ISound;
import net.minecraft.client.audio.Sound;
import net.minecraft.client.audio.SoundEventAccessor;
import net.minecraft.client.audio.SoundHandler;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.math.BlockPos;

public class YoutubeSound implements ISound {

    private BlockPos position;
    private Sound sound;
    private ResourceLocation location;
    private SoundEventAccessor eventAccessor = new SoundEventAccessor(location, null);

    public YoutubeSound(BlockPos position, String songName){
        this.position = position;
        location = new ResourceLocation(SmartVinyl.MOD_ID, songName);
        sound = new Sound(location.toString(),
            getVolume(), getPitch(), getWeight(),
            Sound.Type.SOUND_EVENT,
            true, // stream = true
            false, // preload = false
            getAttenuationDistance());
    }

    @Override
    public ResourceLocation getLocation() {
        return location;
    }

    @Override
    public SoundEventAccessor resolve(SoundHandler handler) {
        return eventAccessor;
    }

    @Override
    public Sound getSound() {
        return sound;
    }

    @Override
    public SoundCategory getSource() {
        return SoundCategory.RECORDS;
    }

    @Override
    public boolean isLooping() {
        return false;
    }

    @Override
    public boolean isRelative() {
        return false;
    }

    @Override
    public int getDelay() {
        return 0;
    }

    @Override
    public float getVolume() {
        return 1f;
    }

    @Override
    public float getPitch() {
        return 1f;
    }

    @Override
    public double getX() {
        return position.getX();
    }

    @Override
    public double getY() {
        return position.getY();
    }

    @Override
    public double getZ() {
        return position.getZ();
    }

    @Override
    public AttenuationType getAttenuation() {
        return AttenuationType.LINEAR;
    }

    public int getAttenuationDistance(){
        return 16;
    }

    public int getWeight(){
        return 1; // whatever this means
    }
}
