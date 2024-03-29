package org.dhatim.dropwizard.sshd;

import io.dropwizard.core.Configuration;
import io.dropwizard.core.ConfiguredBundle;
import io.dropwizard.core.setup.Bootstrap;
import io.dropwizard.core.setup.Environment;
import io.dropwizard.lifecycle.Managed;
import org.apache.sshd.client.ClientBuilder;
import org.apache.sshd.common.NamedFactory;
import org.apache.sshd.common.NamedResource;
import org.apache.sshd.common.kex.BuiltinDHFactories;
import org.apache.sshd.common.kex.KeyExchangeFactory;
import org.apache.sshd.common.signature.BuiltinSignatures;
import org.apache.sshd.common.signature.Signature;
import org.apache.sshd.common.util.GenericUtils;
import org.apache.sshd.server.SshServer;
import org.apache.sshd.server.channel.ChannelSession;
import org.apache.sshd.server.command.Command;
import org.apache.sshd.sftp.server.SftpSubsystem;
import org.apache.sshd.sftp.server.SftpSubsystemFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Arrays;
import java.util.List;

import static java.util.stream.Collectors.joining;
import static java.util.stream.Collectors.toList;

public abstract class SshdBundle<T extends Configuration> implements ConfiguredBundle<T> {

    private static final Logger LOG = LoggerFactory.getLogger(SshdBundle.class);

    public abstract SshdConfiguration getSshdConfiguration(T configuration);

    public abstract void configure(T configuration, Environment environment, SshServer server);

    @Override
    public void run(T configuration, Environment environment) throws Exception {
        SshdConfiguration sshConf = getSshdConfiguration(configuration);
        if (!sshConf.enable) {
            LOG.info("SSHD server disabled");
            return;
        }

        SshServer server = SshServer.setUpDefaultServer();
        server.setSubsystemFactories(Arrays.asList(new SftpSubsystemFactory() {

            @Override
            public Command createSubsystem(ChannelSession channel) {
                SftpSubsystem subsystem = new ThrottledSftpSubsystem(resolveExecutorService(),
                        getUnsupportedAttributePolicy(), getFileSystemAccessor(),
                        getErrorStatusDataHandler(), getErrorChannelDataReceiver(), channel, sshConf.capacity);
                GenericUtils.forEach(getRegisteredListeners(), subsystem::addSftpEventListener);
                return subsystem;
            }
        }));
        server.setPort(sshConf.port);
        server.setHost(sshConf.bindHost);
        if (sshConf.sigAlgorithms != null && !sshConf.sigAlgorithms.trim().isEmpty()) {
            BuiltinSignatures.ParseResult result = BuiltinSignatures.parseSignatureList(sshConf.sigAlgorithms);
            List<NamedFactory<Signature>> list = result.getParsedFactories().stream().collect(toList());
            server.setSignatureFactories(list);
            LOG.info("SSHD: configure signature algorithms to {}", list.stream().map(NamedResource::getName).collect(joining(", ")));
        }
        if (sshConf.encAlgorithms != null && !sshConf.encAlgorithms.trim().isEmpty()) {
            server.setCipherFactoriesNameList(sshConf.encAlgorithms);
            LOG.info("SSHD: configure cipher algorithms to {}", server.getCipherFactories().stream().map(NamedResource::getName).collect(joining(", ")));
        }
        if (sshConf.macAlgorithms != null && !sshConf.macAlgorithms.trim().isEmpty()) {
            server.setMacFactoriesNameList(sshConf.macAlgorithms);
            LOG.info("SSHD: configure message digest algorithms to {}", server.getMacFactories().stream().map(NamedResource::getName).collect(joining(", ")));
        }
        if (sshConf.kexAlgorithms != null && !sshConf.kexAlgorithms.trim().isEmpty()) {
            BuiltinDHFactories.ParseResult result = BuiltinDHFactories.parseDHFactoriesList(sshConf.kexAlgorithms);
            List<KeyExchangeFactory> factories = NamedFactory.setUpTransformedFactories(false, result.getParsedFactories(), ClientBuilder.DH2KEX);
            server.setKeyExchangeFactories(factories);
            LOG.info("SSHD: configure key exchange algorithms to {}", server.getKeyExchangeFactories().stream().map(NamedResource::getName).collect(joining(", ")));
        }
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
