package org.dhatim.fs.util;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.DirectoryStream;
import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.function.Function;
import java.util.stream.Stream;

public class BasicDirectoryStream<T, R> implements DirectoryStream<R> {

    private final Stream<T> stream;
    private final Filter<? super R> filter;
    private final Function<T, R> mapper;

    private volatile boolean closed;
    private volatile Iterator<R> itr;

    public BasicDirectoryStream(Stream<T> stream, Filter<? super R> filter, Function<T, R> mapper) {
        this.stream = stream;
        this.filter = filter;
        this.mapper = mapper;
    }

    @Override
    public Iterator<R> iterator() {
        if (closed || itr != null) {
            throw new IllegalStateException();
        }
        Stream<R> str = stream.map(mapper);
        if (filter != null) {
            str = str.filter(this::filterEntry);
        }
        itr = str.iterator();
        return new Iterator() {
            @Override
            public boolean hasNext() {
                if (closed) {
                    return false;
                }
                return itr.hasNext();
            }

            @Override
            public R next() {
                if (closed) {
                    throw new NoSuchElementException();
                }
                return itr.next();
            }
        };
    }

    @Override
    public void close() {
        closed = true;
    }

    private boolean filterEntry(R entry) {
        try {
            return filter.accept(entry);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

}
