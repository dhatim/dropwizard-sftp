package org.dhatim.dropwizard.sshd;

import io.dropwizard.validation.PortRange;

public class SshdConfiguration {

    public boolean enable;

    @PortRange
    public int port = 2222;

    public String bindHost = null;

    public int capacity = 256;

    public String cipherAlgorithms = "aes128-ctr,aes192-ctr,aes256-ctr,aes128-cbc,blowfish-cbc,aes192-cbc,aes256-cbc";

    public String macAlgorithms = "hmac-sha2-256,hmac-sha2-512";
}
