package org.dhatim.fs.virtual;

import java.nio.file.attribute.FileTime;
import java.nio.file.attribute.GroupPrincipal;
import java.nio.file.attribute.PosixFileAttributes;
import java.nio.file.attribute.PosixFilePermission;
import java.nio.file.attribute.UserPrincipal;
import java.util.Collections;
import java.util.Set;

public final class VirtualPosixFileAttributes implements PosixFileAttributes {

    private static final FileTime NO_TIME = FileTime.fromMillis(0);

    private final VirtualFile file;
    private final UserPrincipal owner;
    private final GroupPrincipal group;

    VirtualPosixFileAttributes(VirtualFile file, UserPrincipal owner, GroupPrincipal group) {
        this.file = file;
        this.owner = owner;
        this.group = group;
    }

    @Override
    public UserPrincipal owner() {
        return owner;
    }

    @Override
    public GroupPrincipal group() {
        return group;
    }

    @Override
    public Set<PosixFilePermission> permissions() {
        return Collections.unmodifiableSet(file.getPermissions());
    }

    @Override
    public FileTime lastModifiedTime() {
        return file.getLastModifiedTime().orElse(NO_TIME);
    }

    @Override
    public FileTime lastAccessTime() {
        return file.getLastAccessTime().orElse(NO_TIME);
    }

    @Override
    public FileTime creationTime() {
        return file.getCreationTime().orElse(NO_TIME);
    }

    @Override
    public boolean isRegularFile() {
        return !isDirectory();
    }

    @Override
    public boolean isDirectory() {
        return file instanceof VirtualDirectory;
    }

    @Override
    public boolean isSymbolicLink() {
        return false;
    }

    @Override
    public boolean isOther() {
        return false;
    }

    @Override
    public long size() {
        return file.getSize();
    }

    @Override
    public Object fileKey() {
        return null;
    }
}
