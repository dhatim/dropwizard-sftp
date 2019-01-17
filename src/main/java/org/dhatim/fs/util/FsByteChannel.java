package org.dhatim.fs.util;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.ReadableByteChannel;
import java.nio.channels.WritableByteChannel;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

/**
 * A byte channel allowing a writer and a reader to produce and consume concurrently.
 * Based on the internal buffer size, operations are blocking until read or write capacity is available.
 */
public class FsByteChannel implements WritableByteChannel, ReadableByteChannel {

    private boolean closed;
    private final byte[] transferBuffer;
    private final Lock lock = new ReentrantLock();
    private final Condition canWrite = lock.newCondition();
    private final Condition canRead = lock.newCondition();
    private long writePos;
    private long readPos;

    public FsByteChannel(int capacity) {
        transferBuffer = new byte[capacity];
    }

    @Override
    public boolean isOpen() {
        lock.lock();
        try {
            return !closed;
        } finally {
            lock.unlock();
        }
    }

    @Override
    public void close() {
        lock.lock();
        try {
            closed = true;

            // complete pending operations
            canRead.signalAll();
            canWrite.signalAll();
        } finally {
            lock.unlock();
        }
    }

    private int writeCapacity() {
        if (writePos < readPos + transferBuffer.length) {
            return (int) (readPos + transferBuffer.length - writePos);
        } else {
            return 0;
        }
    }

    @Override
    public int write(ByteBuffer src) throws IOException {
        lock.lock();
        try {
            int result = 0;
            while (src.remaining() > 0) {
                result += doWrite(src);
            }
            return result;
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            throw new IOException(ex);
        } finally {
            lock.unlock();
        }
    }

    private int doWrite(ByteBuffer src) throws IOException, InterruptedException {
        // wait until we can write into transfer buffer
        int capacity;
        while ((capacity = writeCapacity()) == 0 && !closed) {
            canWrite.await(1, TimeUnit.SECONDS);
        }
        if (closed) {
            throw new IOException("cannot write to a closed channel");
        }

        // write what we can and bump write position
        int toWrite = Integer.min(src.remaining(), capacity);
        int l = transferBuffer.length;
        int pos = (int) (writePos % l);
        if (pos + toWrite <= l) {
            src.get(transferBuffer, pos, toWrite);
        } else {
            src.get(transferBuffer, pos, l - pos);
            src.get(transferBuffer, 0, toWrite - (l - pos));
        }
        writePos += toWrite;
        if (readCapacity() > 0) {
            canRead.signal();
        }
        return toWrite;
    }

    private int readCapacity() {
        if (readPos < writePos) {
            return (int) (writePos - readPos);
        } else {
            return 0;
        }
    }

    @Override
    public int read(ByteBuffer dst) throws IOException {
        lock.lock();
        try {
            return doRead(dst);
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            throw new IOException(ex);
        } finally {
            lock.unlock();
        }
    }

    private int doRead(ByteBuffer dst) throws IOException, InterruptedException {
        if (dst.remaining() == 0) {
            return 0;
        }

        // wait until we can read from transfer buffer
        int capacity;
        while ((capacity = readCapacity()) == 0 && !closed) {
            canRead.await(1, TimeUnit.SECONDS);
        }

        // nothing to read and channel closed: EOF
        if (closed && capacity == 0) {
            return -1;
        }

        // Read what we can and bump read position
        int toRead = Integer.min(dst.remaining(), capacity);
        int l = transferBuffer.length;
        int pos = (int) (readPos % l);
        if (pos + toRead <= l) {
            dst.put(transferBuffer, pos, toRead);
        } else {
            dst.put(transferBuffer, pos, l - pos);
            dst.put(transferBuffer, 0, toRead - (l - pos));
        }
        readPos += toRead;
        if (writeCapacity() > 0) {
            canWrite.signal();
        }
        return toRead;
    }

    public long getReadPos() {
        lock.lock();
        try {
            return readPos;
        } finally {
            lock.unlock();
        }
    }

    public long getWritePos() {
        lock.lock();
        try {
            return writePos;
        } finally {
            lock.unlock();
        }
    }
}
