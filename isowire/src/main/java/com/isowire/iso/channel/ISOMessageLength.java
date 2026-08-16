package com.isowire.iso.channel;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

public interface ISOMessageLength {

    int readLength(InputStream inputStream) throws IOException;

    void writeLength(OutputStream outputStream, int length) throws IOException;

    int getMessageLengthBytes();

    int getMaxMessageLength();
}
