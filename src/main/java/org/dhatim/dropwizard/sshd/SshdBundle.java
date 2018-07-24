package org.dhatim.dropwizard.sshd;

import io.dropwizard.Configuration;
import io.dropwizard.ConfiguredBundle;
import io.dropwizard.lifecycle.Managed;
import io.dropwizard.setup.Bootstrap;
import io.dropwizard.setup.Environment;
import org.apache.sshd.server.SshServer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class SshdBundle <T extends Configuration> implements ConfiguredBundle<T> {

    private static final Logger LOG = LoggerFactory.getLogger(SshdBundle.class);

    public abstract SshdConfiguration getSshdConfiguration(T configuration);

    public abstract void configure(T configuration, Environment environment, SshServer server);

    @Override
    public void run(T configuration, Environment environment) throws Exception {
        SshdConfiguration sshConf = getSshdConfiguration(configuration);
        SshServer server = SshServer.setUpDefaultServer();
        server.setPort(sshConf.getPort());
        server.setHost(sshConf.getBindHost());
        configure(configuration, environment, server);

        environment.lifecycle().manage(new Managed() {
            @Override
            public void start() throws Exception {
                StringBuilder sb = new StringBuilder("Starting sshd server: ")
                        .append(System.lineSeparator())
                        .append(System.lineSeparator());
                server.start();
                sb.append(String.format("    SSHD    %s:%s (%s)", hostToString(server.getHost()), server.getPort(), server.getFileSystemFactory().getClass().getName())).append(System.lineSeparator());
                LOG.info(sb.toString());
            }

            @Override
            public void stop() throws Exception {
                server.stop();
                if (LOG.isInfoEnabled()) {
                    LOG.info("SSHD server stopped");
                }
            }
        });
    }

    private static String hostToString(String host) {
        return host == null || host.isEmpty() ? "0.0.0.0" : host;
    }

    @Override
    public void initialize(Bootstrap<?> bootstrap) {
    }

}
