package org.dhatim.dropwizard.sshd;

import org.apache.sshd.common.util.buffer.Buffer;
import org.apache.sshd.common.util.threads.CloseableExecutorService;
import org.apache.sshd.server.channel.ChannelDataReceiver;
import org.apache.sshd.server.channel.ChannelSession;
import org.apache.sshd.sftp.server.*;

import java.io.IOException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

public class ThrottledSftpSubsystem extends SftpSubsystem {

    private final int capacity;
    private final Lock lock = new ReentrantLock();
    private final Condition hasCapacity = lock.newCondition();

    public ThrottledSftpSubsystem(CloseableExecutorService executorService,
                                  UnsupportedAttributePolicy policy,
                                  SftpFileSystemAccessor accessor,
                                  SftpErrorStatusDataHandler errorStatusDataHandler,
                                  ChannelDataReceiver errorChannelDataReceiver,
                                  ChannelSession channelSession,
                                  int capacity) {
        super(channelSession,
                new SftpSubsystemConfigurator() {

                    @Override
                    public UnsupportedAttributePolicy getUnsupportedAttributePolicy() {
                        return policy;
                    }

                    @Override
                    public SftpFileSystemAccessor getFileSystemAccessor() {
                        return accessor;
                    }

                    @Override
                    public SftpErrorStatusDataHandler getErrorStatusDataHandler() {
                        return errorStatusDataHandler;
                    }

                    @Override
                    public ChannelDataReceiver getErrorChannelDataReceiver() {
                        return errorChannelDataReceiver;
                    }

                    @Override
                    public CloseableExecutorService getExecutorService() {
                        return executorService;
                    }
                });
        this.capacity = capacity;
    }

    @Override
    public int data(ChannelSession channel, byte[] buf, int start, int len) throws IOException {
        lock.lock();
        try {
            while (requests.size() >= capacity) {
                hasCapacity.await(1, TimeUnit.SECONDS);
            }
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            throw new IOException(ex);
        } finally {
            lock.unlock();
        }
        return super.data(channel, buf, start, len);
    }

    @Override
    protected void process(Buffer buffer) throws IOException {
        super.process(buffer);
        lock.lock();
        try {
            if (requests.size() < capacity) {
                hasCapacity.signalAll();
            }
        } finally {
            lock.unlock();
        }
    }

}
