package org.dhatim.fs.util;

import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.MappedByteBuffer;
import java.nio.channels.ClosedChannelException;
import java.nio.channels.FileChannel;
import java.nio.channels.FileLock;
import java.nio.channels.ReadableByteChannel;
import java.nio.channels.WritableByteChannel;
import java.util.Arrays;
import java.util.List;
import org.apache.sshd.common.util.io.IoUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ReadableFileChannel extends FileChannel {

    private static final Logger LOG = LoggerFactory.getLogger(ReadableFileChannel.class);

    private static final int DEFAULT_BUFFER_SIZE = 8192;

    private static final int TRANSFER_SIZE = 8192;

    private final CheckedSupplier<InputStream, IOException> supplier;

    private final SeekableInputStream in;
    private long size = -1;

    private final Object sizeLock = new Object();
    private final Object readLock = new Object();

    public ReadableFileChannel(CheckedSupplier<InputStream, IOException> supplier) {
        LOG.debug("create ReasableFileChannel with inputstream supplier");
        this.supplier = supplier;
        this.in = new SeekableInputStream(supplier);
    }

    @Override
    public int read(ByteBuffer dst) throws IOException {
        checkClosed();
        LOG.debug("fc.read");
        synchronized (readLock) {
            return read(in, dst);
        }
    }

    private int read(InputStream in, ByteBuffer dst) throws IOException {
        int len = dst.remaining();
        int totalRead = 0;
        int bytesRead = 0;
        byte[] buf = new byte[0];
        while (totalRead < len) {
            int bytesToRead = Math.min((len - totalRead), TRANSFER_SIZE);
            if (buf.length < bytesToRead) {
                buf = new byte[bytesToRead];
            }
            if ((totalRead > 0) && in.available() <= 0) {
                break; // block at most once
            }
            try {
                begin();
                bytesRead = in.read(buf, 0, bytesToRead);
            } finally {
                end(bytesRead > 0);
            }
            if (bytesRead < 0) {
                break;
            } else {
                totalRead += bytesRead;
            }
            dst.put(buf, 0, bytesRead);
        }
        if ((bytesRead < 0) && (totalRead == 0)) {
            return -1;
        }
        return totalRead;
    }

    // From SftpRemotePathChannel
    private long doRead(List<ByteBuffer> buffers) throws IOException {
        boolean completed = false;
        boolean eof = false;
        try {
            long totalRead = 0;
            begin();
            loop:
            for (ByteBuffer buffer : buffers) {
                while (buffer.remaining() > 0) {
                    ByteBuffer wrap = buffer;
                    if (!buffer.hasArray()) {
                        wrap = ByteBuffer.allocate(Math.min(IoUtils.DEFAULT_COPY_SIZE, buffer.remaining()));
                    }
                    int read = in.read(wrap.array(), wrap.arrayOffset() + wrap.position(), wrap.remaining());
                    if (read > 0) {
                        if (wrap == buffer) {
                            wrap.position(wrap.position() + read);
                        } else {
                            buffer.put(wrap.array(), wrap.arrayOffset(), read);
                        }
                        totalRead += read;
                    } else {
                        eof = read == -1;
                        break loop;
                    }
                }
            }
            completed = true;
            if (totalRead > 0) {
                return totalRead;
            }
            if (eof) {
                return -1;
            } else {
                return 0;
            }
        } finally {
            end(completed);
        }
    }

    @Override
    public long read(ByteBuffer[] dsts, int offset, int length) throws IOException {
        checkClosed();
        LOG.debug("read 2");
        synchronized (readLock) {
            return doRead(Arrays.asList(dsts).subList(offset, offset + length));
        }
    }

    @Override
    public int write(ByteBuffer src) throws IOException {
        LOG.debug("fc.write");
        throw cannot("write");
    }

    @Override
    public long write(ByteBuffer[] srcs, int offset, int length) throws IOException {
        LOG.debug("fc.write2");
        throw cannot("write");
    }

    @Override
    public long position() {
        LOG.debug("fc.getposition");
        return in.getPosition();
    }

    @Override
    public FileChannel position(long newPosition) throws IOException {
        checkClosed();
        LOG.debug("fc.setposition {} [position is {}]", newPosition, in.getPosition());
        synchronized (readLock) {
            in.seek(newPosition);
        }
        return this;
    }

    @Override
    public long size() throws IOException {
        checkClosed();
        LOG.debug("fc.size");
        synchronized (sizeLock) {
            if (size == -1) {
                try (InputStream is = supplier.get()) {
                    size = computeSize(is);
                }
            }
            return size;
        }
    }

    private static long computeSize(InputStream is) throws IOException {
        byte[] buf = new byte[DEFAULT_BUFFER_SIZE];
        long nread = 0;
        int n;
        for (;;) {
            // read to EOF which may read more or less than initial buffer size
            while ((n = is.read(buf)) > 0) {
                if (n >= 0) {
                    nread += n;
                }
            }
            // if the last call to read returned -1, then we're done
            if (n < 0) {
                break;
            }
        }
        return nread;
    }

    @Override
    public FileChannel truncate(long size) throws IOException {
        LOG.debug("fc.truncate");
        throw cannot("truncate");
    }

    @Override
    public void force(boolean metaData) throws IOException {
        LOG.debug("fc.force");
    }

    @Override
    public long transferTo(long position, long count, WritableByteChannel target) throws IOException {
        LOG.debug("fc.transferTo");
        return 0;
    }

    @Override
    public long transferFrom(ReadableByteChannel src, long position, long count) throws IOException {
        LOG.debug("fc.transferFrom");
        throw cannot("transferFrom");
    }

    @Override
    public int read(ByteBuffer dst, long position) throws IOException {
        checkClosed();
        LOG.debug("fc.read3");
        try (SeekableInputStream other = new SeekableInputStream(supplier)) {
            other.seek(position);
            return read(other, dst);
        }
    }

    @Override
    public int write(ByteBuffer src, long position) throws IOException {
        LOG.debug("fc.write3");
        throw cannot("write");
    }

    @Override
    public MappedByteBuffer map(MapMode mode, long position, long size) throws IOException {
        LOG.debug("fc.map");
        throw cannot("map");
    }

    @Override
    public FileLock lock(long position, long size, boolean shared) throws IOException {
        LOG.debug("fc.lock");
        throw cannot("lock");
    }

    @Override
    public FileLock tryLock(long position, long size, boolean shared) throws IOException {
        LOG.debug("fc.tryLock");
        throw cannot("tryLock");
    }

    @Override
    protected void implCloseChannel() throws IOException {
        if (in != null) {
            in.close();
        }
    }

    private static IOException cannot(String op) {
        return new IOException("Cannot " + op);
    }

    private void checkClosed() throws ClosedChannelException {
        if (!isOpen()) {
            throw new ClosedChannelException();
        }
    }

}
