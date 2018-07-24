package org.dhatim.fs.virtual;

import java.io.IOException;
import java.nio.channels.FileChannel;
import java.nio.file.OpenOption;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Stream;
import org.dhatim.fs.base.VirtualPath;

public interface VirtualDirectory extends VirtualFile {
    Optional<VirtualFile> find(String name);
    Stream<VirtualFile> getChildren();
    FileChannel createFile(VirtualPath path, Set<? extends OpenOption> options) throws IOException;
}
