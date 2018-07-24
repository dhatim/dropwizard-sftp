package org.dhatim.fs.base;

import java.io.IOException;
import java.nio.file.FileSystem;
import java.nio.file.LinkOption;
import java.nio.file.spi.FileSystemProvider;
import java.util.List;
import org.apache.sshd.common.file.util.BasePath;

public class VirtualPath extends BasePath<VirtualPath, AbstractVirtualFileSystem> {

    public VirtualPath(AbstractVirtualFileSystem fileSystem, String root, List<String> names) {
        super(fileSystem, root, names);
    }

    @Override
    public VirtualPath toRealPath(LinkOption... options) throws IOException {
        VirtualPath absolute = toAbsolutePath();
        FileSystem fs = getFileSystem();
        FileSystemProvider provider = fs.provider();
        provider.checkAccess(absolute);
        return absolute;
    }
    
    
}
