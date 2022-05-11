package tk.letsplaybol.smart_vinyl;

import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.Iterator;

import javax.sound.sampled.AudioFormat;

import com.zakgof.velvetvideo.IAudioDecoderStream;
import com.zakgof.velvetvideo.IAudioFrame;
import com.zakgof.velvetvideo.IDemuxer;
import com.zakgof.velvetvideo.ISeekableInput;
import com.zakgof.velvetvideo.impl.VelvetVideoLib;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import net.minecraft.client.audio.IAudioStream;

public class AudioStreamVelvet implements IAudioStream {
    private static final Logger LOGGER = LogManager.getLogger();

    private IDemuxer demuxer;
    private IAudioDecoderStream stream;

    private AudioFormat audioFormat;

    public AudioStreamVelvet() {
        // get piano2.wav from the project root (minecraft is run in run/ directory)
        File wavFile = new File("../piano2.wav");
        demuxer = VelvetVideoLib.getInstance().demuxer(wavFile);
        stream = demuxer.audioStream(0);

        audioFormat = stream.properties().format();
    }

    public AudioStreamVelvet(ISeekableInput input) {
        demuxer = VelvetVideoLib.getInstance().demuxer(input);
        stream = demuxer.audioStream(0);

        audioFormat = stream.properties().format();
    }

    @Override
    public void close() throws IOException {
        demuxer.close();
    }

    @Override
    public AudioFormat getFormat() {
        return audioFormat;
    }

    @Override
    public ByteBuffer read(int size) throws IOException {
        LOGGER.debug("read size: " + size);
        Iterator<IAudioFrame> iter = stream.iterator();

        ByteBuffer buff = ByteBuffer.allocateDirect(size + audioFormat.getFrameSize());
        while (buff.position() + audioFormat.getFrameSize() < size && iter.hasNext()) {
            IAudioFrame frame = iter.next();
            byte[] samples = frame.samples();
            size -= samples.length;
            LOGGER.debug("left to read: " + size);
            buff.put(samples);
        }

        buff.flip();

        return buff;
    }
}
