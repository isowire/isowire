package com.isowire.iso.packager;

import com.isowire.iso.ISOMessage;

/**
 * Exception thrown when a parsing/unpacking error occurs and a partially-parsed
 * ISOMessage is available along with the raw bytes.
 */
public class PackagerParseException extends ISOException {
    private final ISOMessage partialMessage;
    private final byte[] rawData;

    public PackagerParseException(String message, Throwable cause, ISOMessage partialMessage, byte[] rawData) {
        super(message, cause);
        this.partialMessage = partialMessage;
        this.rawData = rawData;
    }

    public ISOMessage getPartialMessage() {
        return partialMessage;
    }

    public byte[] getRawData() {
        return rawData;
    }
}
