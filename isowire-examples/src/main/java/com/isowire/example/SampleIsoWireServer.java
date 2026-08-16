package com.isowire.example;

import com.isowire.iso.ISOServer;
import com.isowire.iso.channel.*;
import com.isowire.iso.packager.ISODefaultPackager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.function.Supplier;

public class SampleIsoWireServer {
    private static final Logger logger = LoggerFactory.getLogger(SampleIsoWireServer.class);

    /**
     * Main launcher using virtual threads (Java 21+).
     * No need to specify thread pool size - virtual threads scale automatically.
     */
    public static void main(String[] args) {
        int port = 9999;
        String channel = "ASCII4";
        if (args.length > 0) {
            try {
                port = Integer.parseInt(args[0]);
            } catch (NumberFormatException e) {
                System.err.println("Invalid port number: " + args[0]);
                System.exit(1);
            }
        }

        int threads = 0;
        if (args.length >= 2) {
            try {
                threads = Integer.parseInt(args[1]); // optional, currently unused
            } catch (NumberFormatException e) {
                System.err.println("Invalid thread count: " + args[1]);
            }
        }
        if (args.length >= 3) {
            channel = args[2];
        }

        ISOServer server = new ISOServer(port, channelSupplier(channel));
        server.setRequestListener(new SampleRequestListener());

        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            logger.info("Shutdown hook triggered");
            server.stop();
        }));

        try {
            logger.info("Starting IsoWire Server on port {} using virtual threads", port);
            server.start();
        } catch (IOException e) {
            logger.error("Failed to start server", e);
            System.exit(1);
        }
    }

    public static Supplier<ISOServerChannel> channelSupplier(String channelType) {
        return () -> {
            BaseISOChannel channel = switch (channelType) {
                case "BINARY2" -> new Binary2ISOChannel(new ISODefaultPackager(), new TCPTransportChannel());
                default        -> new ASCII4ISOChannel(new ISODefaultPackager(), new TCPTransportChannel());
            };

            channel.setHeader("6000000000");
            return channel;
        };
    }
}
