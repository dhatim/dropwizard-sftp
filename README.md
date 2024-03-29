# dropwizard-sftp
[![Build Status](https://github.com/dhatim/dropwizard-sftp/workflows/build/badge.svg)](https://github.com/dhatim/dropwizard-sftp/actions)
[![Maven Central](https://maven-badges.herokuapp.com/maven-central/org.dhatim/dropwizard-sftp/badge.svg)](https://maven-badges.herokuapp.com/maven-central/org.dhatim/dropwizard-sftp)
[![Javadocs](https://www.javadoc.io/badge/org.dhatim/dropwizard-sftp.svg)](https://www.javadoc.io/doc/org.dhatim/dropwizard-sftp)

SFTP Server (SSH File Transfer Protocol) based on [Apache MINA SSHD](https://mina.apache.org/) for [Dropwizard](https://www.dropwizard.io).
**Please note version 2 requires Dropwizard 2.**

## Usage


### Maven Artifacts

This project is available in the [Central Repository](http://search.maven.org/#search%7Cgav%7C1%7Cg%3A%22org.dhatim%22%20AND%20a%3A%22dropwizard-sftp%22). To add it to your project simply add the following dependency to your POM:

```xml
<dependency>
  <groupId>org.dhatim</groupId>
  <artifactId>dropwizard-sftp</artifactId>
  <version>2.1.1</version>
</dependency>
```

### Define SSHD configuration
```yaml
sshd:
  enable: true
  port: 2222
  bindHost: localhost
  capacity: 256
```  

The capacity (defaults to 256) caps the amount of in-flight buffers during a transfer session by throttling the producer and the consumer.

### Add the bundle to your Dropwizard application
```java
bootstrap.addBundle(new SshdBundle<YourConfiguration>() {
    @Override
    public SshdConfiguration getSshdConfiguration(YourConfiguration configuration) {
        return configuration.getSshd();
    }

    @Override
    public void configure(YourConfiguration configuration, Environment environment, SshServer server) {
        // Init your SSH Server
    }
});
```

## Support
Please file bug reports and feature requests in [GitHub issues](https://github.com/dhatim/dropwizard-sftp/issues).
