package org.dhatim.fs.util;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.Channels;
import java.nio.channels.WritableByteChannel;
import java.util.function.Consumer;

public class BufferedByteChannel implements WritableByteChannel {
    
    private boolean open = true;
    private final ByteArrayOutputStream buffer = new ByteArrayOutputStream();
    private final WritableByteChannel channel = Channels.newChannel(buffer);
    private final Consumer<byte[]> terminateOperation;

    public BufferedByteChannel(Consumer<byte[]> terminateOperation) {
        this.terminateOperation = terminateOperation;
    }

    @Override
    public boolean isOpen() {
        return open;
    }

    @Override
    public void close() throws IOException {
        if (open) {
            open = false;
            terminateOperation.accept(buffer.toByteArray());
        }
    }

    @Override
    public int write(ByteBuffer src) throws IOException {
        if (!open) {
            throw new IOException("WritableByteChannel closed");
        }
        return channel.write(src);
    }

}
