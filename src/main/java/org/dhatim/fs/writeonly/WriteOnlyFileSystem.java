package org.dhatim.fs.writeonly;

import java.io.IOException;
import java.net.URI;
import java.nio.channels.FileChannel;
import java.nio.channels.WritableByteChannel;
import java.nio.file.CopyOption;
import java.nio.file.DirectoryStream;
import java.nio.file.DirectoryStream.Filter;
import java.nio.file.LinkOption;
import java.nio.file.OpenOption;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.nio.file.attribute.FileAttribute;
import java.nio.file.attribute.GroupPrincipal;
import java.nio.file.attribute.PosixFileAttributeView;
import java.nio.file.attribute.UserPrincipal;
import java.util.Set;
import org.dhatim.fs.base.AbstractVirtualFileSystem;
import org.dhatim.fs.base.AbstractVirtualFileSystemProvider;
import org.dhatim.fs.base.VirtualPath;
import org.dhatim.fs.util.EmptyDirectoryStream;
import org.dhatim.fs.util.Lazy;

public abstract class WriteOnlyFileSystem extends AbstractVirtualFileSystem {

    private final Lazy<UserPrincipal> defaultUser = new Lazy<>(() -> getUserPrincipalLookupService().lookupPrincipalByName("root"));
    private final Lazy<GroupPrincipal> defaultGroup  = new Lazy<>(() -> getUserPrincipalLookupService().lookupPrincipalByGroupName("root"));
    
    public WriteOnlyFileSystem(AbstractVirtualFileSystemProvider fileSystemProvider, URI uri) {
        super(fileSystemProvider, uri);
    }

    @Override
    protected DirectoryStream<Path> newDirectoryStream(VirtualPath vDir, Filter<? super Path> filter) throws IOException {
        log.trace("newDirectoryStream {}", vDir);
        return new EmptyDirectoryStream<>();
    }

    @Override
    protected void createDirectory(VirtualPath vDir, FileAttribute<?>... attrs) throws IOException {
        log.trace("createDirectory {}", vDir);
        throw new IOException("Cannot create directory");
    }

    @Override
    protected void delete(VirtualPath vPath) throws IOException {
        log.trace("delete {}", vPath);
        throw new IOException("Cannot delete");
    }

    @Override
    protected void copy(VirtualPath src, VirtualPath dst, CopyOption... options) throws IOException {
        log.trace("copy {} {}", src, dst);
        throw new IOException("Cannot copy");
    }

    @Override
    protected void move(VirtualPath src, VirtualPath dst, CopyOption... options) throws IOException {
        log.trace("move {} {}", src, dst);
        throw new IOException("Cannot move");
    }

    @Override
    protected FileChannel newFileChannel(VirtualPath vPath, Set<? extends OpenOption> options, FileAttribute<?>... attrs) throws IOException {
        log.trace("newByteChannel {}", vPath);
        unaccept(options, StandardOpenOption.APPEND);
        unaccept(options, StandardOpenOption.DELETE_ON_CLOSE);
        unaccept(options, StandardOpenOption.READ);
        unaccept(options, StandardOpenOption.SPARSE);
        unaccept(options, LinkOption.NOFOLLOW_LINKS);
        return new WriteOnlyFileChannel(open(vPath));
    }
    
    @Override
    protected PosixFileAttributeView getFileAttributeView(VirtualPath path, LinkOption... options) {
        log.trace("getPosixFileAttributeView {}", path);
        return new WriteOnlyFileAttributeView(this, path);
    }

    private static void unaccept(Set<? extends OpenOption> options, OpenOption option) throws IOException {
        if (options.contains(option)) {
            throw new IOException("Cannot use " + option);
        }
    }
    
    UserPrincipal getDefaultUser() throws IOException {
        return defaultUser.get();
    }
    
    GroupPrincipal getDefaultGroup() throws IOException {
        return defaultGroup.get();
    }
    
    protected abstract WritableByteChannel open(VirtualPath path);
    
}
