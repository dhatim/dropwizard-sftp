package org.dhatim.fs.virtual;

import java.io.IOException;
import java.nio.file.OpenOption;
import java.util.Collection;

public final class VirtualFileUtils {

    private VirtualFileUtils() {
    }

    public static void unaccept(Collection<? extends OpenOption> options, OpenOption option) throws IOException {
        if (options.contains(option)) {
            throw new IOException("Cannot use " + option);
        }
    }

}
