package com.isowire.iso.channel;

import com.isowire.iso.ISOMessage;
import com.isowire.iso.packager.ISOException;
import com.isowire.iso.packager.ISOPackager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

public abstract class BaseISOChannel implements ISOServerChannel, ISOClientChannel, ISOMessageHeader {
    private static final Logger logger = LoggerFactory.getLogger(BaseISOChannel.class);

    private String name;
    private volatile boolean usable;
    private byte[] header = new byte[0];

    private final ConcurrentMap<String, Object> context;
    private final Lock sendLock;
    private final Lock receiveLock;
    private TransportChannel transportChannel;
    private ISOPackager packager;

    private static final int DEFAULT_TIMEOUT = 30000; // 30 seconds
    private int timeout = DEFAULT_TIMEOUT;

    public BaseISOChannel(ISOPackager packager) {
        this(packager, new TCPTransportChannel(null, 0));
    }

    public BaseISOChannel(ISOPackager packager, TransportChannel transportChannel) {
        this.context = new ConcurrentHashMap<>();
        this.sendLock = new ReentrantLock();
        this.receiveLock = new ReentrantLock();
        this.usable = false;
        this.packager = packager;
        this.transportChannel = transportChannel;
        this.name = this.getClass().getSimpleName();
    }

    @Override
    public void connect() throws ISOException {
        try {
            logger.debug("{}: Connecting", name);
            transportChannel.connect();
            usable = true;
            logger.info("{}: Connected", name);
        } catch (IOException e) {
            throw new ISOException("Connection failed", e);
        }
    }

    @Override
    public void disconnect() throws ISOException {
        try {
            logger.debug("{}: Disconnecting", name);
            usable = false;
            transportChannel.disconnect();
            logger.info("{}: Disconnected", name);
        } catch (IOException e) {
            throw new ISOException("Disconnection failed", e);
        }
    }

    @Override
    public boolean isConnected() {
        return usable && transportChannel != null && transportChannel.isConnected();
    }

    @Override
    public void send(ISOMessage msg) throws ISOException {
        sendLock.lock();
        try {
            if (!isConnected()) {
                throw new ISOException("Not connected");
            }

            logger.debug("{}: Sending message: {}", name, msg.getMTI());
            byte[] data = packager.pack(msg);
            transportChannel.send(this, data);
            logger.debug("{}: Message sent successfully", name);
        } catch (IOException e) {
            usable = false;
            throw new ISOException("Send failed", e);
        } finally {
            sendLock.unlock();
        }
    }

    @Override
    public ISOMessage receive() throws ISOException {
        receiveLock.lock();
        try {
            if (!isConnected()) {
                throw new ISOException("Not connected");
            }

            logger.debug("{}: Waiting for message", name);
            byte[] data = transportChannel.receive(this);
            ISOMessage msg = packager.unpack(data);
            logger.debug("{}: Message received: {}", name, msg);
            return msg;
        } catch (IOException e) {
            usable = false;
            throw new ISOException("Receive failed", e);
        } finally {
            receiveLock.unlock();
        }
    }

    @Override
    public ISOMessage sendAndWait(ISOMessage msg, long timeout) throws ISOException {
        sendLock.lock();
        try {
            send(msg);

            long startTime = System.currentTimeMillis();
            long remainingTime = timeout;

            while (remainingTime > 0) {
                try {
                    transportChannel.setTimeout((int) remainingTime);
                    return receive();
                } catch (ISOException e) {
                    if (e.getMessage().contains("Timeout")) {
                        remainingTime = timeout - (System.currentTimeMillis() - startTime);
                        if (remainingTime <= 0) {
                            throw new ISOException("Response timeout", e);
                        }
                        continue;
                    }
                    throw e;
                }
            }

            throw new ISOException("Response timeout");
        } finally {
            sendLock.unlock();
        }
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public void setName(String name) {
        this.name = name;
    }

    @Override
    public int[] getSupportedFieldNumbers() {
        // Default implementation - override in specific channels if needed
        return new int[0];
    }

    public void setTimeout(int timeout) {
        this.timeout = timeout;
        if (transportChannel != null) {
            transportChannel.setTimeout(timeout);
        }
    }

    public int getTimeout() {
        return timeout;
    }

    public void setPackager(ISOPackager packager) {
        this.packager = packager;
    }

    public ISOPackager getPackager() {
        return packager;
    }

    public void setContext(String key, Object value) {
        context.put(key, value);
    }

    public Object getContext(String key) {
        return context.get(key);
    }

    public boolean isUsable() {
        return usable;
    }

    /**
     * Configure channel with an accepted server socket.
     * Used by MyPosServer for handling client connections.
     */
    @Override
    public void setSocket(java.net.Socket socket) throws java.io.IOException {
        if (transportChannel instanceof TCPTransportChannel) {
            ((TCPTransportChannel) transportChannel).setSocket(socket);
            usable = true;
        }
    }

    @Override
    public byte[] getHeader() {
        return this.header;
    }

    @Override
    public void setHeader(byte[] header) {
        this.header = header;
    }

    @Override
    public void setTransportChannel(TransportChannel transportChannel) {
        this.transportChannel = transportChannel;
    }
}
