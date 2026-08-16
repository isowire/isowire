package com.isowire.example;

import com.isowire.iso.ISODataElement;
import com.isowire.iso.ISOMessage;
import com.isowire.iso.channel.*;
import com.isowire.iso.packager.ISODefaultPackager;
import com.isowire.iso.packager.ISOException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigDecimal;

public class SampleIsoWireClient {
    private static final Logger logger = LoggerFactory.getLogger(SampleIsoWireClient.class);

    public static void main(String[] args) {
        String host = "localhost";
        int port = 9999;
        String channel = "ASCII4";

        if (args.length > 0) {
            host = args[0];
        }
        if (args.length > 1) {
            port = Integer.parseInt(args[1]);
        }
        if (args.length > 2) {
            channel = args[2];
        }

        BaseISOChannel isoChannel = createChannel(host, port, channel);
        isoChannel.setHeader("6000000000");

        try {
            logger.info("Connecting to {}:{} with channel {}", host, port, channel);
            isoChannel.connect();

            ISOMessage request = createSampleRequest();
            logger.info("Sending request: {}", request);

            ISOMessage response = isoChannel.sendAndWait(request, 30000);
            logger.info("Received response: {}", response);

            Thread.sleep(1000);

            isoChannel.disconnect();
            logger.info("Test completed successfully");

        } catch (ISOException | InterruptedException e) {
            logger.error("Test failed", e);
        }
    }

    private static ISOMessage createSampleRequest() {
        ISOMessage msg = new ISOMessage();
        msg.setMTI("0100");

        ISODataElement processingCode = new ISODataElement();
        processingCode.set(1,"00");
        processingCode.set(2,"00");
        processingCode.set(3,"00");

        msg.set(2, "1234567890123456");
        msg.set(3, processingCode);
        msg.set(4, "10");
        msg.set(7, "0802123456");
        msg.set(11, "000001");
        msg.set(12, "123456");
        msg.set(13, "0802");
        msg.set(32, "123456");
        msg.set(37, "123456789012");
        msg.set(41, "TERM001");
        msg.set(42, "MERCHANT01");
        msg.set(49, "840");

        return msg;
    }

    private static BaseISOChannel createChannel(String host, int port, String channelType) {
        switch (channelType) {
            case "BINARY2":
                return new Binary2ISOChannel(new ISODefaultPackager(), new TCPTransportChannel(host, port));
            case "ASCII4":
            default:
                return new ASCII4ISOChannel(new ISODefaultPackager(), new TCPTransportChannel(host, port));
        }
    }
}
