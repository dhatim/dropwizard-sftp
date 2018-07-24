package org.dhatim.fs.virtual;

import java.io.IOException;
import java.nio.channels.FileChannel;
import java.nio.file.OpenOption;
import java.nio.file.attribute.FileTime;
import java.nio.file.attribute.PosixFilePermission;
import java.util.Optional;
import java.util.Set;

public interface VirtualFile {
    VirtualDirectory getParent();
    String getName();
    FileChannel open(Set<? extends OpenOption> options) throws IOException;
    Optional<FileTime> getLastModifiedTime();
    Set<PosixFilePermission> getPermissions();
    long getSize();
}
