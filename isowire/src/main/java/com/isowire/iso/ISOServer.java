package com.isowire.iso;

import com.isowire.iso.channel.ASCII4ISOChannel;
import com.isowire.iso.channel.ISOServerChannel;
import com.isowire.iso.packager.ISODefaultPackager;
import com.isowire.iso.packager.PackagerParseException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Supplier;

/**
 * ISO8583 TCP server using Java virtual threads.
 *
 * <p>Each client connection is handled by its own virtual thread.
 * Connections are persistent by default and remain open until the
 * client disconnects, the server is stopped, or an unrecoverable
 * protocol/channel error occurs.</p>
 *
 * <p>The default channel is an {@link ASCII4ISOChannel} using an
 * {@link ISODefaultPackager}.</p>
 *
 * <p>Designed for Java 25.</p>
 */
public class ISOServer {

    private static final Logger logger =
            LoggerFactory.getLogger(ISOServer.class);

    private static final int DEFAULT_BACKLOG = 100;

    private static final long DEFAULT_SHUTDOWN_TIMEOUT_MILLIS = 10_000L;

    private final int port;

    /**
     * Server lifecycle lock.
     */
    private final Object lifecycleLock = new Object();

    /**
     * Server running state.
     */
    private final AtomicBoolean running =
            new AtomicBoolean(false);

    /**
     * Generates unique session IDs.
     */
    private final AtomicLong sessionCounter =
            new AtomicLong(0);

    /**
     * Currently active client sessions.
     */
    private final Set<ClientSession> activeSessions =
            ConcurrentHashMap.newKeySet();

    /**
     * Request listener.
     */
    private volatile ISORequestListener requestListener;

    /**
     * Channel factory.
     */
    private volatile Supplier<ISOServerChannel> channelFactory;

    /**
     * Currently active listening socket.
     */
    private volatile ServerSocket serverSocket;

    /**
     * TCP listen backlog.
     */
    private volatile int backlog = DEFAULT_BACKLOG;

    /**
     * Whether TCP_NODELAY is enabled for client connections.
     */
    private volatile boolean tcpNoDelay = true;

    /**
     * Whether SO_KEEPALIVE is enabled for client connections.
     */
    private volatile boolean keepAlive = true;

    /**
     * Graceful shutdown timeout.
     */
    private volatile long shutdownTimeoutMillis =
            DEFAULT_SHUTDOWN_TIMEOUT_MILLIS;

    public ISOServer(int port) {
        this(port, null);
    }

    public ISOServer(
            int port,
            Supplier<ISOServerChannel> channelFactory
    ) {
        validatePort(port);

        this.port = port;

        this.channelFactory =
                channelFactory != null
                        ? channelFactory
                        : createDefaultChannel();
    }

    /**
     * Set the ISO8583 request listener.
     *
     * @param listener request listener
     */
    public void setRequestListener(
            ISORequestListener listener
    ) {
        this.requestListener = listener;
    }

    /**
     * Set the channel factory.
     *
     * <p>A new channel must be returned for each client connection.</p>
     *
     * @param channelFactory channel factory
     */
    public void setChannelFactory(
            Supplier<ISOServerChannel> channelFactory
    ) {
        this.channelFactory = Objects.requireNonNull(
                channelFactory,
                "channelFactory must not be null"
        );
    }

    /**
     * Configure the TCP listen backlog.
     *
     * <p>Must be configured before the server is started.</p>
     *
     * @param backlog TCP listen backlog
     */
    public void setBacklog(int backlog) {
        if (backlog <= 0) {
            throw new IllegalArgumentException(
                    "backlog must be greater than zero"
            );
        }

        if (isRunning()) {
            throw new IllegalStateException(
                    "Cannot change backlog while server is running"
            );
        }

        this.backlog = backlog;
    }

    /**
     * Configure TCP_NODELAY.
     *
     * @param tcpNoDelay whether TCP_NODELAY should be enabled
     */
    public void setTcpNoDelay(boolean tcpNoDelay) {
        this.tcpNoDelay = tcpNoDelay;
    }

    /**
     * Configure SO_KEEPALIVE.
     *
     * @param keepAlive whether SO_KEEPALIVE should be enabled
     */
    public void setKeepAlive(boolean keepAlive) {
        this.keepAlive = keepAlive;
    }

    /**
     * Configure graceful shutdown timeout.
     *
     * @param timeoutMillis timeout in milliseconds
     */
    public void setShutdownTimeoutMillis(long timeoutMillis) {
        if (timeoutMillis < 0) {
            throw new IllegalArgumentException(
                    "shutdown timeout must not be negative"
            );
        }

        this.shutdownTimeoutMillis = timeoutMillis;
    }

    /**
     * Create the default ISO8583 channel.
     */
    private Supplier<ISOServerChannel> createDefaultChannel() {
        return () -> new ASCII4ISOChannel(
                new ISODefaultPackager()
        );
    }

    /**
     * Start the ISO8583 server.
     *
     * <p>This method blocks in the accept loop until {@link #stop()}
     * is called or the listening socket fails.</p>
     *
     * @throws IOException if the server socket cannot be created
     */
    public void start() throws IOException {

        final ServerSocket socket;

        synchronized (lifecycleLock) {

            if (!running.compareAndSet(false, true)) {
                logger.warn(
                        "Server already running on port {}",
                        port
                );
                return;
            }

            try {
                socket = createServerSocket();
                serverSocket = socket;

            } catch (IOException | RuntimeException e) {
                running.set(false);
                serverSocket = null;
                throw e;
            }
        }

        logger.info(
                "IsoWireServer started on port {} " +
                        "(backlog={}, tcpNoDelay={}, keepAlive={}, " +
                        "virtualThreads=true)",
                socket.getLocalPort(),
                backlog,
                tcpNoDelay,
                keepAlive
        );

        try {

            while (running.get()) {

                try {

                    Socket clientSocket = socket.accept();

                    /*
                     * stop() may have happened immediately after
                     * accept() returned.
                     */
                    if (!running.get()) {
                        closeQuietly(clientSocket);
                        break;
                    }

                    configureClientSocket(clientSocket);

                    createAndStartSession(clientSocket);

                } catch (IOException e) {

                    if (running.get()) {
                        logger.error(
                                "Error accepting connection on port {}",
                                port,
                                e
                        );
                    } else {
                        logger.debug(
                                "Server accept loop interrupted by shutdown"
                        );
                    }

                    break;

                } catch (RuntimeException e) {

                    /*
                     * Protect the server accept loop from unexpected
                     * connection/session setup failures.
                     */
                    logger.error(
                            "Unexpected error accepting connection " +
                                    "on port {}",
                            port,
                            e
                    );
                }
            }

        } finally {

            synchronized (lifecycleLock) {

                /*
                 * Only clear the socket belonging to this start()
                 * invocation.
                 */
                if (serverSocket == socket) {
                    serverSocket = null;
                    running.set(false);
                }
            }

            closeServerSocket(socket);

            /*
             * If the accept loop exits unexpectedly, make sure active
             * sessions aren't left behind.
             */
            if (!running.get()) {
                closeActiveSessions();
            }

            logger.info(
                    "IsoWireServer accept loop stopped on port {}",
                    port
            );
        }
    }

    /**
     * Create and bind the listening socket.
     */
    private ServerSocket createServerSocket()
            throws IOException {

        ServerSocket socket = new ServerSocket();

        /*
         * Allows the server to restart more cleanly after shutdown.
         */
        socket.setReuseAddress(true);

        socket.bind(
                new InetSocketAddress(port),
                backlog
        );

        return socket;
    }

    /**
     * Configure an accepted client socket.
     *
     * <p>The read timeout is deliberately left at zero. Persistent
     * ISO8583 connections should not be disconnected simply because
     * they are idle.</p>
     */
    private void configureClientSocket(
            Socket clientSocket
    ) throws IOException {

        clientSocket.setTcpNoDelay(tcpNoDelay);

        clientSocket.setKeepAlive(keepAlive);

        /*
         * Zero means blocking socket reads.
         */
        clientSocket.setSoTimeout(0);
    }

    /**
     * Create and start a client session.
     */
    private void createAndStartSession(
            Socket clientSocket
    ) {

        final String clientAddress =
                String.valueOf(
                        clientSocket.getRemoteSocketAddress()
                );

        final long sessionId =
                sessionCounter.incrementAndGet();

        final String sessionName =
                "server-session-" + sessionId;

        logger.info(
                "New connection {} from {}",
                sessionName,
                clientAddress
        );

        final ISOServerChannel channel;

        try {

            channel = Objects.requireNonNull(
                    channelFactory.get(),
                    "channelFactory returned null"
            );

        } catch (Exception e) {

            logger.error(
                    "Failed to create channel for {}",
                    clientAddress,
                    e
            );

            closeQuietly(clientSocket);
            return;
        }

        final ClientSession session;

        try {

            session = new ClientSession(
                    sessionId,
                    sessionName,
                    clientSocket,
                    channel
            );

        } catch (Exception e) {

            logger.error(
                    "Failed to initialize session {} for {}",
                    sessionName,
                    clientAddress,
                    e
            );

            closeChannelQuietly(
                    channel,
                    clientSocket
            );

            return;
        }

        activeSessions.add(session);

        /*
         * One virtual thread per client connection.
         */
        Thread.ofVirtual()
                .name(sessionName)
                .start(() -> {

                    try {
                        session.run();

                    } finally {
                        activeSessions.remove(session);
                    }
                });
    }

    /**
     * Stop the server.
     *
     * <p>Shutdown proceeds as follows:</p>
     *
     * <ol>
     *     <li>Stop accepting new connections.</li>
     *     <li>Close active client sessions.</li>
     *     <li>Wait for sessions to terminate.</li>
     * </ol>
     */
    public void stop() {

        synchronized (lifecycleLock) {

            if (!running.compareAndSet(true, false)) {
                logger.debug(
                        "IsoWireServer on port {} is already stopped",
                        port
                );
                return;
            }

            logger.info(
                    "Stopping IsoWireServer on port {}",
                    port
            );

            /*
             * Closing ServerSocket interrupts accept().
             */
            closeServerSocket(serverSocket);
        }

        /*
         * Do not hold lifecycleLock while waiting.
         */
        closeActiveSessions();

        waitForSessionsToStop();

        logger.info(
                "IsoWireServer stopped on port {}",
                port
        );
    }

    /**
     * Wait for active sessions to terminate.
     */
    private void waitForSessionsToStop() {

        final long timeoutMillis =
                shutdownTimeoutMillis;

        if (timeoutMillis == 0) {
            return;
        }

        final long deadline =
                System.nanoTime()
                        + TimeUnit.MILLISECONDS.toNanos(
                        timeoutMillis
                );

        while (!activeSessions.isEmpty()) {

            final long remainingNanos =
                    deadline - System.nanoTime();

            if (remainingNanos <= 0) {

                logger.warn(
                        "Shutdown timeout reached with {} " +
                                "active session(s) remaining",
                        activeSessions.size()
                );

                return;
            }

            try {

                TimeUnit.NANOSECONDS.sleep(
                        Math.min(
                                remainingNanos,
                                TimeUnit.MILLISECONDS.toNanos(50)
                        )
                );

            } catch (InterruptedException e) {

                Thread.currentThread().interrupt();

                logger.warn(
                        "Interrupted while waiting for client " +
                                "sessions to stop"
                );

                return;
            }
        }
    }

    /**
     * Close all active client sessions.
     */
    private void closeActiveSessions() {

        for (ClientSession session : activeSessions) {

            try {
                session.close();

            } catch (Exception e) {

                logger.debug(
                        "Error closing active session",
                        e
                );
            }
        }
    }

    /**
     * Returns whether the server is running.
     */
    public boolean isRunning() {
        return running.get();
    }

    /**
     * Returns the number of active client sessions.
     */
    public int getActiveSessionCount() {
        return activeSessions.size();
    }

    /**
     * Returns the configured server port.
     */
    public int getPort() {
        return port;
    }

    /**
     * Returns the configured TCP backlog.
     */
    public int getBacklog() {
        return backlog;
    }

    /**
     * Returns whether TCP_NODELAY is enabled.
     */
    public boolean isTcpNoDelay() {
        return tcpNoDelay;
    }

    /**
     * Returns whether SO_KEEPALIVE is enabled.
     */
    public boolean isKeepAlive() {
        return keepAlive;
    }

    /**
     * Returns the configured shutdown timeout.
     */
    public long getShutdownTimeoutMillis() {
        return shutdownTimeoutMillis;
    }

    /**
     * Close a ServerSocket without throwing.
     */
    private void closeServerSocket(
            ServerSocket socket
    ) {

        if (socket == null) {
            return;
        }

        try {

            if (!socket.isClosed()) {
                socket.close();
            }

        } catch (IOException e) {

            logger.debug(
                    "Error closing server socket",
                    e
            );
        }
    }

    /**
     * Close a Socket without throwing.
     */
    private void closeQuietly(Socket socket) {

        if (socket == null) {
            return;
        }

        try {

            if (!socket.isClosed()) {
                socket.close();
            }

        } catch (IOException e) {

            logger.debug(
                    "Error closing client socket",
                    e
            );
        }
    }

    /**
     * Disconnect a channel and fall back to closing the socket.
     */
    private void closeChannelQuietly(
            ISOServerChannel channel,
            Socket socket
    ) {

        try {

            if (channel != null && channel.isConnected()) {
                channel.disconnect();
                return;
            }

        } catch (Exception e) {

            logger.debug(
                    "Error disconnecting channel",
                    e
            );
        }

        closeQuietly(socket);
    }

    /**
     * Validate server port.
     */
    private static void validatePort(int port) {

        if (port < 0 || port > 65535) {
            throw new IllegalArgumentException(
                    "Invalid server port: " + port
            );
        }
    }

    /**
     * One persistent ISO8583 client session.
     */
    private final class ClientSession
            implements Runnable {

        private final long sessionId;
        private final String sessionName;
        private final Socket socket;
        private final ISOServerChannel channel;
        private final String clientAddress;

        private final AtomicBoolean closed =
                new AtomicBoolean(false);

        private ClientSession(
                long sessionId,
                String sessionName,
                Socket socket,
                ISOServerChannel channel
        ) throws IOException {

            this.sessionId = sessionId;

            this.sessionName =
                    Objects.requireNonNull(
                            sessionName,
                            "sessionName must not be null"
                    );

            this.socket =
                    Objects.requireNonNull(
                            socket,
                            "socket must not be null"
                    );

            this.channel =
                    Objects.requireNonNull(
                            channel,
                            "channel must not be null"
                    );

            this.clientAddress =
                    String.valueOf(
                            socket.getRemoteSocketAddress()
                    );

            /*
             * Channel initialization must complete successfully before
             * the session is added to activeSessions.
             */
            channel.setSocket(socket);
            channel.setName(sessionName);
        }

        @Override
        public void run() {

            logger.info(
                    "Session {} started for {}",
                    sessionName,
                    clientAddress
            );

            try {

                while (
                        !closed.get()
                                && !socket.isClosed()
                                && running.get()
                ) {

                    try {

                        ISOMessage request =
                                channel.receive();

                        /*
                         * Defensive handling for custom channel
                         * implementations.
                         */
                        if (request == null) {

                            logger.debug(
                                    "Channel returned null request " +
                                            "for {}",
                                    clientAddress
                            );

                            break;
                        }

                        logger.debug(
                                "Received request from {}: MTI={}",
                                clientAddress,
                                request.getMTI()
                        );

                        /*
                         * Resolve the listener for every request.
                         * This allows the application to replace the
                         * listener while the server is running.
                         */
                        ISORequestListener listener =
                                requestListener;

                        if (listener == null) {

                            logger.warn(
                                    "No request listener configured " +
                                            "for request from {}",
                                    clientAddress
                            );

                            continue;
                        }

                        ISOMessage response =
                                listener.processRequest(request);

                        if (response != null) {

                            channel.send(response);

                            logger.debug(
                                    "Sent response to {}: MTI={}",
                                    clientAddress,
                                    response.getMTI()
                            );

                        } else {

                            logger.warn(
                                    "No response generated for " +
                                            "request from {}",
                                    clientAddress
                            );
                        }

                    } catch (PackagerParseException e) {

                        /*
                         * Give the application an opportunity to
                         * generate a protocol-level error response.
                         */
                        handlePackagerParseError(e);

                        /*
                         * A malformed message can leave the stream
                         * framing in an unknown state. Close the
                         * connection rather than attempting to parse
                         * the next message from an uncertain boundary.
                         */
                        break;

                    } catch (Exception e) {

                        /*
                         * Important:
                         *
                         * We intentionally catch Exception here rather
                         * than SocketException / SocketTimeoutException /
                         * IOException individually.
                         *
                         * The ISOServerChannel API owns the socket I/O
                         * contract. If receive() does not declare those
                         * checked exceptions, Java correctly rejects
                         * individual checked-exception catch blocks.
                         *
                         * This also ensures a custom channel implementation
                         * cannot terminate the server's accept loop.
                         */
                        if (!closed.get()) {

                            logger.error(
                                    "Error processing request from {}",
                                    clientAddress,
                                    e
                            );
                        }

                        break;
                    }
                }

            } catch (Exception e) {

                if (!closed.get()) {

                    logger.error(
                            "Session error for {}",
                            clientAddress,
                            e
                    );
                }

            } finally {

                close();

                logger.info(
                        "Session {} closed for {}",
                        sessionName,
                        clientAddress
                );
            }
        }

        /**
         * Handle a packager parsing error.
         */
        private void handlePackagerParseError(
                PackagerParseException exception
        ) {

            logger.warn(
                    "ISO8583 parse error from {}: {}",
                    clientAddress,
                    exception.getMessage()
            );

            ISORequestListener listener =
                    requestListener;

            if (listener == null) {

                logger.warn(
                        "No request listener configured to handle " +
                                "parse error from {}",
                        clientAddress
                );

                return;
            }

            try {

                ISOMessage response =
                        listener.processError(
                                clientAddress,
                                exception.getRawData(),
                                exception.getPartialMessage(),
                                exception
                        );

                if (response != null) {

                    channel.send(response);

                    logger.debug(
                            "Sent parse-error response to {}: MTI={}",
                            clientAddress,
                            response.getMTI()
                    );

                } else {

                    logger.warn(
                            "No parse-error response generated " +
                                    "for {}",
                            clientAddress
                    );
                }

            } catch (Exception e) {

                logger.error(
                        "Listener threw while handling parse error " +
                                "from {}",
                        clientAddress,
                        e
                );
            }
        }

        /**
         * Close this session.
         */
        private void close() {

            if (!closed.compareAndSet(false, true)) {
                return;
            }

            try {

                if (channel.isConnected()) {

                    channel.disconnect();

                } else {

                    closeQuietly(socket);
                }

            } catch (Exception e) {

                logger.debug(
                        "Error disconnecting session {} for {}",
                        sessionName,
                        clientAddress,
                        e
                );

                /*
                 * Always attempt to close the underlying socket.
                 */
                closeQuietly(socket);
            }
        }

        @Override
        public String toString() {

            return "ClientSession{" +
                    "sessionId=" + sessionId +
                    ", sessionName='" + sessionName + '\'' +
                    ", clientAddress='" + clientAddress + '\'' +
                    '}';
        }
    }
}