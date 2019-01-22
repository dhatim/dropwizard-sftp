package org.dhatim.fs.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.nio.MappedByteBuffer;
import java.nio.channels.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import static java.nio.file.StandardOpenOption.READ;
import static java.nio.file.StandardOpenOption.WRITE;

public class FsFileChannelForRead extends FileChannel {

    private static final Logger LOG = LoggerFactory.getLogger(FsFileChannelForRead.class);

    private final Path temp;
    private final FileChannel writeChannel;
    private final FileChannel readChannel;

    private final Lock lock = new ReentrantLock();
    private final Condition canRead = lock.newCondition();

    public FsFileChannelForRead() throws IOException {
        temp = Files.createTempFile("sftp-read", ".tmp");
        writeChannel = FileChannel.open(temp, WRITE);
        readChannel = FileChannel.open(temp, READ);
    }

    public void transferTo(String threadName, ThrowingConsumer<OutputStream> writer) {
        new Thread(() -> {
            try (OutputStream os = Channels.newOutputStream(writeChannel)) {
                writer.accept(os);
            } catch (IOException e) {
                LOG.error("cannot transfer to channel", e);
            }
        }, threadName).start();
    }

    @Override
    protected void implCloseChannel() throws IOException {
        lock.lock();
        try {
            writeChannel.close();
            readChannel.close();
            canRead.signalAll();
            Files.delete(temp.toAbsolutePath());
        } finally {
            lock.unlock();
        }
    }

    @Override
    public int read(ByteBuffer dst) throws IOException {
        ensureRead();
        return readChannel.read(dst);
    }

    @Override
    public long read(ByteBuffer[] dsts, int offset, int length) throws IOException {
        ensureRead();
        return readChannel.read(dsts, offset, length);
    }

    @Override
    public int write(ByteBuffer src) throws IOException {
        throw new UnsupportedOperationException("write");
    }

    @Override
    public long write(ByteBuffer[] srcs, int offset, int length) throws IOException {
        throw new UnsupportedOperationException("write");
    }

    @Override
    public long position() throws IOException {
        return readChannel.position();
    }

    @Override
    public FileChannel position(long newPosition) throws IOException {
        ensureRead(newPosition);
        return readChannel.position(newPosition);
    }

    @Override
    public long size() throws IOException {
        return readChannel.size();
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
        ensureRead(position);
        return readChannel.read(dst, position);
    }

    @Override
    public int write(ByteBuffer src, long position) throws IOException {
        throw new UnsupportedOperationException("write");
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

    private void ensureRead(long position) throws IOException {
        lock.lock();
        try {
            while (position >= readChannel.size() && writeChannel.isOpen()) {
                canRead.await(100, TimeUnit.MILLISECONDS);
            }
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            throw new IOException(ex);
        } finally {
            lock.unlock();
        }
    }

    private void ensureRead() throws IOException {
        ensureRead(readChannel.position());
    }
}
