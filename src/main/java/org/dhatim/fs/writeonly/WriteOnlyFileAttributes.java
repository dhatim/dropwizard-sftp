package org.dhatim.fs.writeonly;

import java.nio.file.attribute.FileTime;
import java.nio.file.attribute.GroupPrincipal;
import java.nio.file.attribute.PosixFileAttributes;
import java.nio.file.attribute.PosixFilePermission;
import java.nio.file.attribute.UserPrincipal;
import java.util.EnumSet;
import java.util.Set;

public class WriteOnlyFileAttributes implements PosixFileAttributes {
    
    private static final FileTime NO_TIME = FileTime.fromMillis(0);
    
    private final boolean isFile;
    private final UserPrincipal user;
    private final GroupPrincipal group;

    public WriteOnlyFileAttributes(boolean isFile, UserPrincipal user, GroupPrincipal group) {
        this.isFile = isFile;
        this.user = user;
        this.group = group;
    }

    @Override
    public FileTime lastModifiedTime() {
        return NO_TIME;
    }

    @Override
    public FileTime lastAccessTime() {
        return NO_TIME;
    }

    @Override
    public FileTime creationTime() {
        return NO_TIME;
    }

    @Override
    public boolean isRegularFile() {
        return isFile;
    }

    @Override
    public boolean isDirectory() {
        return !isFile;
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
        return 0;
    }

    @Override
    public Object fileKey() {
        return null;
    }

    @Override
    public UserPrincipal owner() {
        return user;
    }

    @Override
    public GroupPrincipal group() {
        return group;
    }

    @Override
    public Set<PosixFilePermission> permissions() {
        return EnumSet.allOf(PosixFilePermission.class);
    }

}
