package org.dhatim.fs.util;

import java.io.IOException;

public class UnsupportedIOException extends IOException {

    public UnsupportedIOException(String operation) {
        super(operation + " is not supported");
    }

}
