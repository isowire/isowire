package com.isowire.iso.packager;

public class ISOException extends Exception {
    public ISOException(String message) {
        super(message);
    }

    public ISOException(String message, Throwable cause) {
        super(message, cause);
    }
}
