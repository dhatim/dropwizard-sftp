package org.dhatim.fs.util;

import java.io.IOException;

public interface ThrowingConsumer<T> {

    void accept(T t) throws IOException;

}
