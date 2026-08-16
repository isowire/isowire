package com.isowire.iso.channel;

import com.isowire.iso.packager.ISOPackager;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

public class Binary2ISOChannel extends BaseISOChannel {

    public Binary2ISOChannel(ISOPackager packager) {
        super(packager);
    }

    public Binary2ISOChannel(ISOPackager packager, TransportChannel transportChannel) {
        super(packager, transportChannel);
    }

    @Override
    public int readLength(InputStream inputStream) throws IOException {
        int b1 = inputStream.read();
        int b2 = inputStream.read();

        if (b1 < 0 || b2 < 0) {
            throw new IOException("Unexpected end of stream while reading message length");
        }

        int length = (b1 << 8) | b2;

        if (length < 0 || length > getMaxMessageLength()) {
            throw new IOException("Invalid message length: " + length);
        }

        return length;
    }

    @Override
    public void writeLength(OutputStream outputStream, int length) throws IOException {
        if (length < 0 || length > 0xFFFF) {
            throw new IllegalArgumentException(
                    "Message length must be between 0 and 65535: " + length);
        }

        outputStream.write((length >>> 8) & 0xFF);
        outputStream.write(length & 0xFF);
    }

    @Override
    public int getMessageLengthBytes() {
        return 2;
    }

    @Override
    public int getMaxMessageLength() {
        return 65535;
    }
}
