package org.dhatim.fs.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.MappedByteBuffer;
import java.nio.channels.*;

public class FsFileChannelForWrite extends FileChannel {

    private static final Logger LOG = LoggerFactory.getLogger(FsFileChannelForWrite.class);
    private final FsByteChannel channel;

    public FsFileChannelForWrite() {
        this(1024 * 1024);
    }

    public FsFileChannelForWrite(int capacity) {
        channel = new FsByteChannel(capacity);
    }

    public Thread transferFrom(String threadName, ThrowingConsumer<InputStream> reader) {
        Thread thread = new Thread(() -> {
            try (InputStream is = Channels.newInputStream(channel)) {
                reader.accept(is);
            } catch (IOException e) {
                LOG.error("cannot transfer from channel", e);
            } finally {
                channel.close();
            }
        }, threadName);
        thread.start();
        return thread;
    }

    @Override
    public int read(ByteBuffer dst) throws IOException {
        throw new UnsupportedOperationException("read");
    }

    @Override
    public long read(ByteBuffer[] dsts, int offset, int length) throws IOException {
        throw new UnsupportedOperationException("read buffers");
    }

    @Override
    public int write(ByteBuffer src) throws IOException {
        return channel.write(src);
    }

    @Override
    public long write(ByteBuffer[] srcs, int offset, int length) throws IOException {
        throw new UnsupportedOperationException("write buffers");
    }

    @Override
    public long position() throws IOException {
        return channel.getWritePos();
    }

    @Override
    public FileChannel position(long newPosition) throws IOException {
        if (newPosition != size()) {
            throw new UnsupportedOperationException("position");
        }
        // no-op
        return this;
    }

    @Override
    public long size() throws IOException {
        return channel.getWritePos();
    }

    @Override
    public FileChannel truncate(long size) throws IOException {
        throw new UnsupportedOperationException("truncate");
    }

    @Override
    public void force(boolean metaData) throws IOException {
        // do nothing
    }

    @Override
    public long transferTo(long position, long count, WritableByteChannel target) throws IOException {
        throw new UnsupportedOperationException("transferTo");
    }

    @Override
    public long transferFrom(ReadableByteChannel src, long position, long count) throws IOException {
        throw new UnsupportedOperationException("transferFrom");
    }

    @Override
    public int read(ByteBuffer dst, long position) throws IOException {
        throw new UnsupportedOperationException("read at position");
    }

    @Override
    public int write(ByteBuffer src, long position) throws IOException {
        throw new UnsupportedOperationException("write at position");
    }

    @Override
    public MappedByteBuffer map(MapMode mode, long position, long size) throws IOException {
        throw new UnsupportedOperationException("map");
    }

    @Override
    public FileLock lock(long position, long size, boolean shared) throws IOException {
        throw new UnsupportedOperationException("lock");
    }

    @Override
    public FileLock tryLock(long position, long size, boolean shared) throws IOException {
        throw new UnsupportedOperationException("tryLock");
    }

    @Override
    protected void implCloseChannel() throws IOException {
        channel.close();
    }
}
