package com.isowire.iso.channel;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.net.Socket;
import java.net.SocketTimeoutException;

/**
 * TCP transport channel implementation.
 * Uses modern Java features for network communication.
 */
public class TCPTransportChannel implements TransportChannel {
    private static final Logger logger = LoggerFactory.getLogger(TCPTransportChannel.class);

    private String host;
    private int port;
    private Socket socket;
    private InputStream inputStream;
    private OutputStream outputStream;
    private int timeout = 30000; // 30 seconds default

    public TCPTransportChannel(String host, int port) {
        this.host = host;
        this.port = port;
    }

    @Override
    public void connect() throws IOException {
        logger.debug("Connecting to {}:{}", host, port);
        socket = new Socket(host, port);
        socket.setSoTimeout(timeout);
        socket.setTcpNoDelay(true);
        inputStream = new BufferedInputStream(socket.getInputStream());
        outputStream = new BufferedOutputStream(socket.getOutputStream());
        logger.debug("Connected to {}:{}", host, port);
    }

    @Override
    public void disconnect() throws IOException {
        logger.debug("Disconnecting from {}:{}", host, port);
        if (outputStream != null) {
            try {
                outputStream.close();
            } catch (IOException e) {
                logger.warn("Error closing output stream", e);
            }
        }
        if (inputStream != null) {
            try {
                inputStream.close();
            } catch (IOException e) {
                logger.warn("Error closing input stream", e);
            }
        }
        if (socket != null) {
            try {
                socket.close();
            } catch (IOException e) {
                logger.warn("Error closing socket", e);
            }
        }
        logger.debug("Disconnected from {}:{}", host, port);
    }

    @Override
    public boolean isConnected() {
        return socket != null && socket.isConnected() && !socket.isClosed();
    }

    @Override
    public void send(ISOMessageHeader messageHeader ,byte[] data) throws IOException {
        if (!isConnected()) {
            throw new IOException("Not connected");
        }

        byte[] header = messageHeader.getHeader();
        int totalLength = header.length + data.length;

        if (totalLength > messageHeader.getMaxMessageLength()) {
            throw new IOException("Message too large: " + totalLength + " > " + messageHeader.getMaxMessageLength());
        }

        logger.debug("Sending {} bytes (header: {} + data: {})", totalLength, header.length, data.length);

        // Write message length header (includes header + message data)
        messageHeader.writeLength(outputStream, totalLength);

        // Write additional header bytes
        if (header.length > 0) {
            outputStream.write(header);
        }

        // Write message data
        outputStream.write(data);
        outputStream.flush();
    }

    @Override
    public byte[] receive(ISOMessageHeader messageHeader) throws IOException {
        if (!isConnected()) {
            throw new IOException("Not connected");
        }

        try {
            // Read message length header (this includes header bytes + message data)
            int totalLength = messageHeader.readLength(inputStream);

            byte[] header = messageHeader.getHeader();
            int headerLength = header.length;

            if (totalLength < headerLength) {
                throw new IOException("Invalid message length: " + totalLength + " < header length: " + headerLength);
            }

            // Read additional header bytes if present
            byte[] receivedHeader = new byte[headerLength];
            if (headerLength > 0) {
                int totalRead = 0;
                while (totalRead < headerLength) {
                    int read = inputStream.read(receivedHeader, totalRead, headerLength - totalRead);
                    if (read == -1) {
                        throw new IOException("End of stream while reading header");
                    }
                    totalRead += read;
                }
            }

            // Calculate actual message data length
            int dataLength = totalLength - headerLength;

            logger.debug("Receiving {} bytes (header: {} + data: {})", totalLength, headerLength, dataLength);

            // Read message data
            byte[] data = new byte[dataLength];
            int totalRead = 0;
            while (totalRead < dataLength) {
                int read = inputStream.read(data, totalRead, dataLength - totalRead);
                if (read == -1) {
                    throw new IOException("End of stream while reading data");
                }
                totalRead += read;
            }

            return data;

        } catch (SocketTimeoutException e) {
            throw new IOException("Timeout while receiving data", e);
        }
    }

    @Override
    public void setTimeout(int timeout) {
        this.timeout = timeout;
        if (socket != null) {
            try {
                socket.setSoTimeout(timeout);
            } catch (IOException e) {
                logger.warn("Error setting socket timeout", e);
            }
        }
    }

    @Override
    public int getTimeout() {
        return timeout;
    }

    public String getHost() {
        return host;
    }

    public int getPort() {
        return port;
    }


    /**
     * Set an already-connected socket (for server-side accepted connections).
     * This is used by MyPosServer when accepting client connections.
     */
    public void setSocket(Socket socket) throws IOException {
        this.socket = socket;
        socket.setSoTimeout(timeout);
        socket.setTcpNoDelay(true);
        inputStream = new BufferedInputStream(socket.getInputStream());
        outputStream = new BufferedOutputStream(socket.getOutputStream());
        logger.debug("Socket set for server-side connection from {}", socket.getRemoteSocketAddress());
    }
}
