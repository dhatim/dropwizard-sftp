package org.dhatim.fs.util;

import java.io.IOException;
import java.io.InputStream;

public class SeekableInputStream extends InputStream {

    private final CheckedSupplier<InputStream, IOException> supplier;
    private long pos;
    private InputStream in;

    private long markPosition;

    public SeekableInputStream(CheckedSupplier<InputStream, IOException> supplier) {
        this.supplier = supplier;
    }

    @Override
    public int read() throws IOException {
        ensureOpen();
        int b = in.read();
        if (b >= 0) {
            pos++;
        }
        return b;
    }

    @Override
    public int read(byte[] b) throws IOException {
        ensureOpen();
        int c = in.read(b);
        if (c >= 0) {
            pos += c;
        }
        return c;
    }

    @Override
    public int read(byte[] b, int off, int len) throws IOException {
        ensureOpen();
        int c = in.read(b, off, len);
        if (c >= 0) {
            pos += c;
        }
        return c;
    }

    @Override
    public long skip(long n) throws IOException {
        ensureOpen();
        long c = in.skip(n);
        pos += c;
        return c;
    }

    @Override
    public int available() throws IOException {
        return in.available();
    }

    @Override
    public void close() throws IOException {
        closeInternal();
    }

    @Override
    public synchronized void mark(int readlimit) {
        markPosition = pos;
    }

    @Override
    public synchronized void reset() throws IOException {
        reopen(markPosition);
    }

    @Override
    public boolean markSupported() {
        return true;
    }

    public long getPosition() {
        return pos;
    }

    /**
     * Change current position of the input stream
     * @param newPosition new absolute position
     * @return new position
     * @throws IOException if an I/O error occurs
     */
    public long seek(long newPosition) throws IOException {
        if (newPosition > pos) {
            long toSkip = newPosition - pos;
            long skipped = 0;
            while (toSkip > 0 && (skipped = skip(toSkip)) > 0) {
                toSkip -= skipped;
            }
            return pos;
        } else if (newPosition < pos) {
            reopen(newPosition);
            return pos;
        } else {
            return pos;
        }
    }

    private void ensureOpen() throws IOException {
        if (in == null) {
            in = supplier.get();
            pos = 0;
        }
    }

    private void closeInternal() throws IOException {
        if (in != null) {
            in.close();
            in = null;
        }
    }

    private void reopen(long newPos) throws IOException {
        if (pos != newPos) {
            closeInternal();
            ensureOpen();
            if (in.skip(newPos) != newPos) {
                throw new IOException("Cannot skip " + newPos + " bytes");
            }
        }

    }

}
