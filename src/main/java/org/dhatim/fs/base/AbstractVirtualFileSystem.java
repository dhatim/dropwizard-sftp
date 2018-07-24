package org.dhatim.fs.base;

import java.io.IOException;
import java.net.URI;
import java.nio.channels.FileChannel;
import java.nio.file.CopyOption;
import java.nio.file.DirectoryStream;
import java.nio.file.DirectoryStream.Filter;
import java.nio.file.LinkOption;
import java.nio.file.OpenOption;
import java.nio.file.Path;
import java.nio.file.attribute.FileAttribute;
import java.nio.file.attribute.GroupPrincipal;
import java.nio.file.attribute.PosixFileAttributeView;
import java.nio.file.attribute.UserPrincipal;
import java.nio.file.attribute.UserPrincipalLookupService;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.apache.sshd.common.file.util.BaseFileSystem;

public abstract class AbstractVirtualFileSystem extends BaseFileSystem<VirtualPath> {
    
    private static class DefaultUserPrincipalLookupService extends UserPrincipalLookupService {
        
        public static final DefaultUserPrincipalLookupService INSTANCE = new DefaultUserPrincipalLookupService();

        @Override
        public UserPrincipal lookupPrincipalByName(String name) throws IOException {
            return new DefaultUserPrincipal(name);
        }

        @Override
        public GroupPrincipal lookupPrincipalByGroupName(String group) throws IOException {
            return new DefaultGroupPrincipal(group);
        }
        
    }
    
    private static class DefaultUserPrincipal extends AbstractPrincipal implements UserPrincipal {

        public DefaultUserPrincipal(String name) {
            super(name);
        }
        
    }
    
    private static class DefaultGroupPrincipal extends AbstractPrincipal implements GroupPrincipal {

        protected DefaultGroupPrincipal(String name) {
            super(name);
        }
        
    }
    
    private boolean open;
    private final URI uri;
    
    public AbstractVirtualFileSystem(AbstractVirtualFileSystemProvider fileSystemProvider, URI uri) {
        super(fileSystemProvider);
        this.uri = uri;
        open = true;
    }

    @Override
    protected VirtualPath create(String root, List<String> names) {
        return new VirtualPath(this, root, names);
    }

    @Override
    public final void close() throws IOException {
        if (isOpen()) {
            open = false;
            provider().removeFileSystem(this);
            free();
        }
    }
    
    protected abstract void free() throws IOException;
    
    protected final URI getURI() {
        return uri;
    }

    @Override
    public final boolean isOpen() {
        return open;
    }

    @Override
    public Set<String> supportedFileAttributeViews() {
        return new HashSet<>(Arrays.asList("basic", "owner", "posix"));
    }

    @Override
    public UserPrincipalLookupService getUserPrincipalLookupService() {
        return DefaultUserPrincipalLookupService.INSTANCE;
    }
    
    @Override
    public AbstractVirtualFileSystemProvider provider() {
        return (AbstractVirtualFileSystemProvider) super.provider();
    }

    protected abstract DirectoryStream<Path> newDirectoryStream(VirtualPath vDir, Filter<? super Path> filter) throws IOException;

    protected abstract void createDirectory(VirtualPath vDir, FileAttribute<?>... attrs) throws IOException;

    protected abstract void delete(VirtualPath vPath) throws IOException;

    protected abstract void copy(VirtualPath src, VirtualPath dst, CopyOption... options) throws IOException;

    protected abstract void move(VirtualPath src, VirtualPath dst, CopyOption... options) throws IOException;

    protected abstract FileChannel newFileChannel(VirtualPath vPath, Set<? extends OpenOption> options, FileAttribute<?>... attrs) throws IOException;
    
    protected abstract PosixFileAttributeView getFileAttributeView(VirtualPath path, LinkOption... options);

}
