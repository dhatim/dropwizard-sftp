package org.dhatim.fs.writeonly;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.channels.FileLock;
import java.nio.channels.ReadableByteChannel;
import java.nio.channels.WritableByteChannel;

public class WriteOnlyFileChannel extends FileChannel {
    
    private final WritableByteChannel outputChannel;
    private long position;

    public WriteOnlyFileChannel(WritableByteChannel outputChannel) {
        this.outputChannel = outputChannel;
    }
    
    @Override
    public int read(ByteBuffer dst) throws IOException {
        throw cannot("read");
    }

    @Override
    public long read(ByteBuffer[] dsts, int offset, int length) throws IOException {
        throw cannot("read buffers");
    }

    @Override
    public int write(ByteBuffer src) throws IOException {
        int written = outputChannel.write(src);
        position += written;
        return written;
    }

    @Override
    public long write(ByteBuffer[] srcs, int offset, int length) throws IOException {
        long written = 0;
        for (int i=offset; i<offset+length; i++) {
            written += write(srcs[i]);
        }
        return written;
    }

    @Override
    public long position() throws IOException {
        return position;
    }

    @Override
    public FileChannel position(long newPosition) throws IOException {
        throw cannot("set position");
    }

    @Override
    public long size() throws IOException {
        throw cannot("get size");
    }

    @Override
    public FileChannel truncate(long size) throws IOException {
        throw cannot("truncate");
    }

    @Override
    public void force(boolean metaData) throws IOException {
        throw cannot("force");
    }

    @Override
    public long transferTo(long position, long count, WritableByteChannel target) throws IOException {
        throw cannot("transferTo");
    }

    @Override
    public long transferFrom(ReadableByteChannel src, long position, long count) throws IOException {
        throw cannot("transferFrom");
    }

    @Override
    public int read(ByteBuffer dst, long position) throws IOException {
        throw cannot("read");
    }

    @Override
    public int write(ByteBuffer src, long position) throws IOException {
        throw cannot("write at position");
    }

    @Override
    public MappedByteBuffer map(MapMode mode, long position, long size) throws IOException {
        throw cannot("map");
    }

    @Override
    public FileLock lock(long position, long size, boolean shared) throws IOException {
        throw cannot("lock");
    }

    @Override
    public FileLock tryLock(long position, long size, boolean shared) throws IOException {
        throw cannot("tryLock");
    }

    @Override
    protected void implCloseChannel() throws IOException {
        outputChannel.close();
    }
    
    private static IOException cannot(String op) {
        return new IOException("Cannot " + op);
    }
    
}
