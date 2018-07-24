package org.dhatim.fs.util;

@FunctionalInterface
public interface CheckedSupplier<T, X extends Throwable> {
    T get() throws X;
}
