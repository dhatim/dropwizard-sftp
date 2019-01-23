package org.dhatim.fs.util;

import java.io.IOException;
import java.io.OutputStream;

public class DelegateOutputStream extends OutputStream {

    private final OutputStream os;

    public DelegateOutputStream(OutputStream os) {
        this.os = os;
    }

    protected void bytesWritten(int n) throws IOException {
    }

    @Override
    public void write(byte[] b) throws IOException {
        os.write(b);
        bytesWritten(b.length);
    }

    @Override
    public void write(byte[] b, int off, int len) throws IOException {
        os.write(b, off, len);
        bytesWritten(len);
    }

    @Override
    public void write(int b) throws IOException {
        os.write(b);
        bytesWritten(1);
    }

    @Override
    public void flush() throws IOException {
        os.flush();
    }

    @Override
    public void close() throws IOException {
        os.close();
    }
}
