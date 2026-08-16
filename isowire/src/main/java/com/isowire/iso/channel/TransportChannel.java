package com.isowire.iso.channel;

import java.io.IOException;

/**
 * Interface for transport channels.
 * Modern Java interface for network communication abstraction.
 */
public interface TransportChannel {

    void connect() throws IOException;

    void disconnect() throws IOException;

    boolean isConnected();

    void send(ISOMessageHeader header, byte[] data) throws IOException;

    byte[] receive(ISOMessageHeader header) throws IOException;

    void setTimeout(int timeout);

    int getTimeout();
}
