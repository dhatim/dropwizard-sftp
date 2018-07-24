package org.dhatim.fs.writeonly;

import java.io.IOException;
import java.nio.file.NoSuchFileException;
import java.nio.file.attribute.FileTime;
import java.nio.file.attribute.GroupPrincipal;
import java.nio.file.attribute.PosixFileAttributeView;
import java.nio.file.attribute.PosixFileAttributes;
import java.nio.file.attribute.PosixFilePermission;
import java.nio.file.attribute.UserPrincipal;
import java.util.Set;
import org.dhatim.fs.base.VirtualPath;
import org.dhatim.fs.util.UnsupportedIOException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class WriteOnlyFileAttributeView implements PosixFileAttributeView {
    
    private static final Logger LOG = LoggerFactory.getLogger(WriteOnlyFileAttributeView.class);
    
    private final VirtualPath path;
    private final WriteOnlyFileSystem fileSystem;
    
    public WriteOnlyFileAttributeView(WriteOnlyFileSystem fileSystem, VirtualPath path) {
        this.fileSystem = fileSystem;
        this.path = path;
    }

    @Override
    public UserPrincipal getOwner() throws IOException {
        LOG.trace("view.getOwner {}", path);
        return readAttributes().owner();
    }

    @Override
    public void setOwner(UserPrincipal owner) throws IOException {
        LOG.trace("view.setOwner {}", path);
        throw new UnsupportedIOException("setOwner");
    }

    @Override
    public String name() {
        return "posix";
    }

    @Override
    public PosixFileAttributes readAttributes() throws IOException {
        LOG.trace("view.readAttributes for {}", path);
        if (fileSystem.getDefaultDir().equals(path)) {
            return new WriteOnlyFileAttributes(false, fileSystem.getDefaultUser(), fileSystem.getDefaultGroup());
        } else {
            throw new NoSuchFileException(path.toString());
        }
    }

    @Override
    public void setTimes(FileTime lastModifiedTime, FileTime lastAccessTime, FileTime createTime) throws IOException {
        LOG.trace("view.setTimes {}", path);
        throw new UnsupportedIOException("setTimes");
    }

    @Override
    public void setPermissions(Set<PosixFilePermission> perms) throws IOException {
        LOG.trace("view.setPermissions {}", path);
        throw new UnsupportedIOException("setPermissions");
    }

    @Override
    public void setGroup(GroupPrincipal group) throws IOException {
        LOG.trace("view.setGroup {}", path);
        throw new UnsupportedIOException("setGroup");
    }

}
