package org.dhatim.fs.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.nio.MappedByteBuffer;
import java.nio.channels.Channels;
import java.nio.channels.FileChannel;
import java.nio.channels.FileLock;
import java.nio.channels.ReadableByteChannel;
import java.nio.channels.WritableByteChannel;

public class FsFileChannelForRead extends FileChannel {

    private static final Logger LOG = LoggerFactory.getLogger(FsFileChannelForRead.class);
    private final FsByteChannel channel;

    public FsFileChannelForRead() {
        this(1024 * 1024);
    }

    public FsFileChannelForRead(int capacity) {
        channel = new FsByteChannel(capacity);
    }

    public void transferTo(String threadName, ThrowingConsumer<OutputStream> writer) {
        new Thread(() -> {
            try (OutputStream os = Channels.newOutputStream(channel)) {
                writer.accept(os);
            } catch (IOException e) {
                LOG.error("cannot transfer to channel", e);
            } finally {
                channel.close();
            }
        }, threadName).start();
    }

    @Override
    public int read(ByteBuffer dst) throws IOException {
        return channel.read(dst);
    }

    @Override
    public long read(ByteBuffer[] dsts, int offset, int length) throws IOException {
        throw new UnsupportedOperationException("read buffers");
    }

    @Override
    public int write(ByteBuffer src) throws IOException {
        throw new UnsupportedOperationException("write");
    }

    @Override
    public long write(ByteBuffer[] srcs, int offset, int length) throws IOException {
        throw new UnsupportedOperationException("write buffers");
    }

    @Override
    public long position() throws IOException {
        return channel.getReadPos();
    }

    @Override
    public FileChannel position(long newPosition) throws IOException {
        // no-op
        return this;
    }

    @Override
    public long size() throws IOException {
        return channel.getReadPos();
    }

    @Override
    public FileChannel truncate(long size) throws IOException {
        throw new UnsupportedOperationException("truncate");
    }

    @Override
    public void force(boolean metaData) throws IOException {
        throw new UnsupportedOperationException("force");
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
