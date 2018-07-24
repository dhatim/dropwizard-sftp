package org.dhatim.fs.util;

import java.nio.file.DirectoryStream;
import java.util.Iterator;

public class EmptyDirectoryStream<T> implements DirectoryStream<T> {

    @Override
    public void close() {
        // Nothing to close
    }

    @Override
    public Iterator<T> iterator() {
        return new EmptyIterator<>();
    }

}
