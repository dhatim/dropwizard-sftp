package org.dhatim.fs.virtual;

import java.io.IOException;
import java.nio.channels.FileChannel;
import java.nio.file.OpenOption;
import java.nio.file.attribute.FileTime;
import java.nio.file.attribute.GroupPrincipal;
import java.nio.file.attribute.PosixFilePermission;
import java.nio.file.attribute.UserPrincipal;
import java.util.Optional;
import java.util.Set;

public interface VirtualFile {
    VirtualDirectory getParent();
    String getName();
    FileChannel open(Set<? extends OpenOption> options) throws IOException;
    Optional<FileTime> getLastModifiedTime();
    Optional<FileTime> getCreationTime();
    Optional<FileTime> getLastAccessTime();
    Set<PosixFilePermission> getPermissions();
    long getSize();

    void setOwner(UserPrincipal owner) throws IOException;
    void setTimes(FileTime lastModifiedTime, FileTime lastAccessTime, FileTime createTime) throws IOException;
    void setPermissions(Set<PosixFilePermission> perms) throws IOException;
    void setGroup(GroupPrincipal group) throws IOException;
}
