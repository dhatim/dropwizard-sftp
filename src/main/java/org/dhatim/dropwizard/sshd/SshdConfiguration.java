package org.dhatim.dropwizard.sshd;

import io.dropwizard.validation.PortRange;

public class SshdConfiguration {

    private boolean enable;

    @PortRange
    private int port = 2222;

    private String bindHost = null;

    private int capacity = 256;

    public void setPort(int port) {
        this.port = port;
    }

    public int getPort() {
        return port;
    }

    public void setCapacity(int capacity) {
        this.capacity = capacity;
    }

    public int getCapacity() {
        return capacity;
    }

    public String getBindHost() {
        return bindHost;
    }

    public void setBindHost(String host) {
        this.bindHost = host;
    }

    public boolean isEnable() {
        return enable;
    }

    public void setEnable(boolean enable) {
        this.enable = enable;
    }

}
