package org.dhatim.dropwizard.sshd;

import io.dropwizard.validation.PortRange;

public class SshdConfiguration {

    public boolean enable;

    @PortRange
    public int port = 2222;

    public String bindHost = null;

    public int capacity = 256;

    public String encAlgorithms = "";

    public String macAlgorithms = "";

    public String kexAlgorithms = "";
}
