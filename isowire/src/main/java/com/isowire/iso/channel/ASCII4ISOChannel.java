package com.isowire.iso.channel;

import com.isowire.iso.packager.ISOPackager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

/**
 * ASCII4Channel - uses 4-digit ASCII message length encoding.
 * Commonly used in legacy ISO8583 implementations and ASCII-based protocols.
 */
public class ASCII4ISOChannel extends BaseISOChannel {

    private static final Logger logger = LoggerFactory.getLogger(ASCII4ISOChannel.class);

    private static final int LENGTH_BYTES = 4;
    private static final int MAX_LENGTH = 9999;

    public ASCII4ISOChannel(ISOPackager packager) {
        super(packager);
    }

    public ASCII4ISOChannel(ISOPackager packager, TransportChannel transportChannel) {
        super(packager, transportChannel);
    }

    @Override
    public int readLength(InputStream inputStream) throws IOException {
        byte[] lengthBytes = new byte[LENGTH_BYTES];
        int totalRead = 0;

        while (totalRead < LENGTH_BYTES) {
            int read = inputStream.read(lengthBytes, totalRead, LENGTH_BYTES - totalRead);
            if (read == -1) {
                throw new IOException("End of stream while reading message length");
            }
            totalRead += read;
        }

        String lengthStr = new String(lengthBytes);
        int length;
        try {
            length = Integer.parseInt(lengthStr);
        } catch (NumberFormatException e) {
            throw new IOException("Invalid message length format: " + lengthStr, e);
        }

        logger.trace("Read ASCII4 message length: {}", length);

        if (length <= 0 || length > MAX_LENGTH) {
            throw new IOException("Invalid message length: " + length);
        }
        return length;
    }

    @Override
    public void writeLength(OutputStream outputStream, int length) throws IOException {
        if (length <= 0 || length > MAX_LENGTH) {
            throw new IOException("Message length out of range: " + length);
        }

        String lengthStr = String.format("%0" + LENGTH_BYTES + "d", length);
        outputStream.write(lengthStr.getBytes());

        logger.trace("Wrote ASCII4 message length: {}", length);
    }

    @Override
    public int getMessageLengthBytes() {
        return LENGTH_BYTES;
    }

    @Override
    public int getMaxMessageLength() {
        return MAX_LENGTH;
    }
}
