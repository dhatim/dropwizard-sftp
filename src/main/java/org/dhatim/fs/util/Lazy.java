package org.dhatim.fs.util;

import java.io.IOException;
import java.util.Objects;

public final class Lazy<T> {

    private volatile T value;
    private final CheckedSupplier<T, IOException> supplier;

    public Lazy(CheckedSupplier<T, IOException> supplier) {
        this.supplier = supplier;
    }

    public T get() throws IOException {
        final T result = value;
        return result == null ? initialize() : result;
    }

    private synchronized T initialize() throws IOException {
        if (value == null) {
            value = Objects.requireNonNull(supplier.get());
        }
        return value;
    }

}
