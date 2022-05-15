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
import com.zakgof.velvetvideo.IVelvetVideoLib;
import com.zakgof.velvetvideo.impl.VelvetVideoLib;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import net.minecraft.client.audio.IAudioStream;

public class AudioStreamVelvet implements IAudioStream {
    private static final Logger LOGGER = LogManager.getLogger();
    private static final IVelvetVideoLib velvetVideoLib = VelvetVideoLib.getInstance();

    private IDemuxer demuxer;
    private IAudioDecoderStream stream;
    private Iterator<IAudioFrame> streamIterator;

    private AudioFormat unconvertedAudioFormat;
    private AudioFormat convertedAudioFormat;

    private boolean downsizingSamples = false;

    private ByteBuffer leftoverSamples;

    public AudioStreamVelvet(File file) {
        this(velvetVideoLib.demuxer(file));
    }

    public AudioStreamVelvet(ISeekableInput input) {
        this(velvetVideoLib.demuxer(input));
    }

    private AudioStreamVelvet(IDemuxer demuxer) {
        this.demuxer = demuxer;
        stream = demuxer.audioStream(0);
        streamIterator = stream.iterator();

        unconvertedAudioFormat = stream.properties().format();
        if (unconvertedAudioFormat.getEncoding() != AudioFormat.Encoding.PCM_SIGNED) {
            throw new UnsupportedOperationException(
                    "only signed pcm audio implemented, was " + unconvertedAudioFormat.getEncoding());
        }

        if (unconvertedAudioFormat.getSampleSizeInBits() <= 16) {
            convertedAudioFormat = unconvertedAudioFormat;
        } else if (unconvertedAudioFormat.getSampleSizeInBits() == 32) {
            // resize samples 32 -> 16 (minecraft doesn't seem to accept 32 bit samples)
            downsizingSamples = true;
            int converted_sample_size_bits = 16;
            int converted_frame_size = 2 * unconvertedAudioFormat.getChannels();
            convertedAudioFormat = new AudioFormat(
                    unconvertedAudioFormat.getEncoding(),
                    unconvertedAudioFormat.getSampleRate(),
                    converted_sample_size_bits,
                    unconvertedAudioFormat.getChannels(),
                    converted_frame_size,
                    unconvertedAudioFormat.getFrameRate(),
                    unconvertedAudioFormat.isBigEndian());
        } else {
            throw new UnsupportedOperationException(
                    "sample size in bits should be <= 16, or 32, was " + unconvertedAudioFormat.getSampleSizeInBits());
        }
        LOGGER.info("format before conversion: " + unconvertedAudioFormat + ", after: " + convertedAudioFormat);
    }

    @Override
    public void close() throws IOException {
        demuxer.close();
    }

    @Override
    public AudioFormat getFormat() {
        return convertedAudioFormat;
    }

    @Override
    public ByteBuffer read(int size) throws IOException {
        LOGGER.trace("read size: " + size + ", downsizingSamples:" + downsizingSamples);

        ByteBuffer buff = ByteBuffer.allocateDirect(size);
        while (size > 0) {
            ByteBuffer nextBuff = readUpTo(size);
            if (!nextBuff.hasRemaining()) {
                break;
            }
            size -= nextBuff.remaining();
            LOGGER.trace("size after: " + size + ", remaining: " + nextBuff.remaining());
            buff.put(nextBuff);
        }
        LOGGER.trace("buff posistion: " + buff.position() + ", requested size: " + size);

        buff.flip();

        return buff;
    }

    private ByteBuffer readUpTo(int size) throws IOException {
        LOGGER.trace("readUpTo: " + size);

        ByteBuffer newBuffer;
        if (leftoverSamples != null) {
            LOGGER.trace("using leftovers");
            newBuffer = leftoverSamples;
            leftoverSamples = null;
        } else {
            LOGGER.trace("using new frame");
            newBuffer = nextFrame();
        }

        if (newBuffer.remaining() <= size) {
            LOGGER.trace("returning whole frame (" + newBuffer.remaining() + ")");
            return newBuffer;
        }

        byte[] samples = new byte[size];
        newBuffer.get(samples);
        ByteBuffer retBuffer = ByteBuffer.wrap(samples);
        leftoverSamples = newBuffer.slice();

        LOGGER.trace("returning part of frame (size: " + retBuffer.remaining() + ", left over: "
                + leftoverSamples.remaining() + ")");

        return retBuffer;
    }

    // get frame (downsized if necessary) from audio stream
    private ByteBuffer nextFrame() throws IOException {
        if (!streamIterator.hasNext()) {
            return ByteBuffer.allocate(0);
        }

        byte[] samples = streamIterator.next().samples();
        int samplesSizeDownsized = downsizeSamples(samples);
        LOGGER.trace("size before downsizing: " + samples.length + ", after: " + samplesSizeDownsized);
        return ByteBuffer.wrap(samples, 0, samplesSizeDownsized);
    }

    // downsize sample buffer in place
    // only downsizes from 32 to 16 bits
    // it'd be better if it was possible to ask velvet a specified format, but this
    // doesn't seem to be possible
    private int downsizeSamples(byte[] samples) {
        if (!downsizingSamples) {
            return samples.length;
        }
        int inputSizeBytes = unconvertedAudioFormat.getSampleSizeInBits() / 8;
        // most significant bytes are first two of an int if buffer is big endian, and
        // last two if it's little
        // thare's likely some better, more abstract way to do this, probably ByteBuffer
        // based, but I can't be bothered to redo that right now
        int msbOffset = unconvertedAudioFormat.isBigEndian() ? 0 : 2;

        for (int i = 0; i < samples.length / inputSizeBytes; i++) {
            int sample32MsbOffset = i * 4 + msbOffset;
            int sample16Offset = i * 2;
            samples[sample16Offset] = samples[sample32MsbOffset];
            samples[sample16Offset + 1] = samples[sample32MsbOffset + 1];
        }
        return samples.length / 2;
    }
}
