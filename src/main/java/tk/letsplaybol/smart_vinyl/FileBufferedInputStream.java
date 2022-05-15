package tk.letsplaybol.smart_vinyl;

import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.channels.FileChannel.MapMode;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.concurrent.CompletableFuture;

import com.zakgof.velvetvideo.ISeekableInput;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

class FileBufferedInputStream implements ISeekableInput {

    private static final Logger LOGGER = LogManager.getLogger();

    private static final int READ_BUFFER_SIZE = 4096;

    private InputStream input;
    private int size;
    private ByteBuffer writeBuffer;
    private ByteBuffer readBuffer;
    private Object lock;
    private boolean beganConsuming;

    private String downloadName;

    public FileBufferedInputStream(InputStream input, Path backingFilePath, int size) throws IOException {
        LOGGER.info("constructor " + backingFilePath);

        downloadName = backingFilePath.toString();

        this.input = input;
        this.size = size;
        this.writeBuffer = FileChannel.open(backingFilePath,
                StandardOpenOption.CREATE, StandardOpenOption.WRITE, StandardOpenOption.READ)
                .map(MapMode.READ_WRITE, 0, size);
        this.readBuffer = this.writeBuffer.slice();
        this.lock = new Object();
        this.beganConsuming = false;
    }

    private FileBufferedInputStream(InputStream input, int size, ByteBuffer writeBuffer, ByteBuffer readBuffer,
            Object lock) {
        this.input = input;
        this.writeBuffer = writeBuffer;
        this.size = size;
        this.readBuffer = readBuffer;
        this.lock = lock;
        this.beganConsuming = true;
    }

    public FileBufferedInputStream slice() {
        return new FileBufferedInputStream(input, size, writeBuffer, readBuffer.slice(), lock);
    }

    public void downloadAsync(){
        CompletableFuture.runAsync(() -> {
            try {
                download();
            } catch (IOException e) {
                LOGGER.error("downloading failed for " + downloadName, e);
            }
        });
    }

    // This should be called on a separate producer thread
    public void download() throws IOException {
        if (beganConsuming) {
            throw new IllegalStateException("Already began consuming");
        }
        beganConsuming = true;

        byte[] buf = new byte[READ_BUFFER_SIZE];
        int len = input.read(buf);
        int read = 0;

        while (len != -1) {
            synchronized (lock) {
                read += len;
                writeBuffer.put(buf, 0, len);
                len = input.read(buf);
                lock.notifyAll();
            }
            if (read / READ_BUFFER_SIZE % 100 == 0) {
                LOGGER.debug(downloadName + ": downloaded " + ((double) writeBuffer.position()) / size);
            }
        }
        LOGGER.info("download finished for " + downloadName);
    }

    public int read(byte[] bytes) {
        return read(bytes, readBuffer.position(), bytes.length);
    }

    public int read(byte[] bytes, int offset, int len) {
        LOGGER.trace("read at offset: " + offset + " len: " + len);
        readBuffer.position(offset);
        if (readBuffer.position() >= size) {
            return -1;
        }

        // There's more than "len" left in the input stream, but it hasn't been produced
        // yet
        if (readBuffer.position() + len > writeBuffer.position() && (readBuffer.position() + len <= size)) {
            synchronized (lock) {
                while ((readBuffer.position() + len > writeBuffer.position())
                        && (readBuffer.position() + len <= size)) {
                    LOGGER.trace("Tried to read too much, need to read to " + (readBuffer.position() + len)
                            + ", available: " + writeBuffer.position() + ", waiting");
                    try {
                        lock.wait();
                    } catch (InterruptedException e) {
                        LOGGER.trace("interrupted");
                        return -1;
                    }
                }

                LOGGER.trace("actually reading at offset: " + offset + ", len: " + len);
                // Read it
                readBuffer.get(bytes, 0, len);
                return len;
            }
        } else {
            int remaining = Math.min(size - readBuffer.position(), len);
            readBuffer.get(bytes, 0, remaining);
            return remaining;
        }
    }

    public void close() {
        try {
            input.close();
        } catch (IOException e) {
        }
    }

    public void seek(long position) {
        readBuffer.position((int) position);
    }

    public long size() {
        return size;
    }
}
