package tk.letsplaybol.smart_vinyl;

import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
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
    private boolean monoingSamples = false;

    private ByteBuffer leftoverSamples;

    public AudioStreamVelvet(File file) {
        this(file, false);
    }

    public AudioStreamVelvet(File file, boolean convertToMono) {
        this(velvetVideoLib.demuxer(file), convertToMono);
    }

    public AudioStreamVelvet(ISeekableInput input) {
        this(input, false);
    }

    public AudioStreamVelvet(ISeekableInput input, boolean convertToMono) {
        this(velvetVideoLib.demuxer(input), convertToMono);
    }

    private AudioStreamVelvet(IDemuxer demuxer, boolean convertToMono) {
        this.demuxer = demuxer;
        this.monoingSamples = convertToMono;
        stream = demuxer.audioStream(0);
        streamIterator = stream.iterator();

        unconvertedAudioFormat = stream.properties().format();
        if (unconvertedAudioFormat.getEncoding() != AudioFormat.Encoding.PCM_SIGNED) {
            throw new UnsupportedOperationException(
                    "only signed pcm audio implemented, was " + unconvertedAudioFormat.getEncoding());
        }
        if (unconvertedAudioFormat.getChannels() != 2) {
            throw new UnsupportedOperationException(
                    "only stereo audio input implemented, channel count was " + unconvertedAudioFormat.getChannels());
        }

        if (unconvertedAudioFormat.getSampleSizeInBits() <= 16) {
            convertedAudioFormat = unconvertedAudioFormat;
        } else if (unconvertedAudioFormat.getSampleSizeInBits() == 32) {
            // resize samples 32 -> 16 (minecraft doesn't seem to accept 32 bit samples)
            downsizingSamples = true;
            int convertedSampleSizeBits = 16;
            int convertedFrameSize = 2 * unconvertedAudioFormat.getChannels();
            int convertedChannels = monoingSamples ? 1 : 2;
            convertedAudioFormat = new AudioFormat(
                    unconvertedAudioFormat.getEncoding(),
                    unconvertedAudioFormat.getSampleRate(),
                    convertedSampleSizeBits,
                    convertedChannels,
                    convertedFrameSize,
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
        if (!downsizingSamples && !monoingSamples) {
            return samples.length;
        }

        ByteBuffer readBuff = ByteBuffer.wrap(samples);
        ByteBuffer writeBuff = ByteBuffer.wrap(samples);
        readBuff.order(unconvertedAudioFormat.isBigEndian() ? ByteOrder.BIG_ENDIAN : ByteOrder.LITTLE_ENDIAN);
        writeBuff.order(convertedAudioFormat.isBigEndian() ? ByteOrder.BIG_ENDIAN : ByteOrder.LITTLE_ENDIAN);
        int sample0, sample1;

        int inputSizeBytes = unconvertedAudioFormat.getSampleSizeInBits() / 8;

        while (readBuff.remaining() >= inputSizeBytes * 2) {
            if (downsizingSamples) {
                sample0 = readBuff.getInt() >> 16;
                sample1 = readBuff.getInt() >> 16;
            } else {
                sample0 = readBuff.getShort();
                sample1 = readBuff.getShort();
            }

            if (monoingSamples) {
                writeBuff.putShort((short) ((sample0 + sample1) >> 1));
            } else {
                writeBuff.putShort((short) sample0);
                writeBuff.putShort((short) sample1);
            }
        }
        int resultLength = samples.length;
        if (downsizingSamples){
            resultLength /= 2;
        }
        if(monoingSamples){
            resultLength /= 2;
        }
        return resultLength;
    }
}
