package com.isowire.iso.packager;

import com.isowire.iso.ISOMessage;
import com.isowire.iso.ISOFieldConfig;
import com.isowire.iso.packager.PackagerParseException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.Map;

/**
 * Default ISO8583 Message Packager.
 * Loads field configurations from YAML and handles packing/unpacking of messages.
 */
public class ISODefaultPackager implements ISOPackager {
    private static final Logger logger = LoggerFactory.getLogger(ISODefaultPackager.class);
    private static final int MAX_FIELDS = 128;

    private final Map<Integer, ISOFieldConfig> fieldConfigs;

    /**
     * Create packager with default configuration from classpath.
     * Loads iso8583-fields.yaml from classpath resources.
     */
    public ISODefaultPackager() {
        this.fieldConfigs = ISOFieldConfig.Loader.load();
        logger.debug("Loaded {} field configurations from classpath", fieldConfigs.size());
    }

    /**
     * Create packager with pre-loaded field configurations.
     * Useful for testing and dynamic configuration.
     * @param fieldConfigs field configuration map
     */
    public ISODefaultPackager(Map<Integer, ISOFieldConfig> fieldConfigs) {
        this.fieldConfigs = fieldConfigs;
        logger.debug("Using provided field configurations: {} fields", fieldConfigs.size());
    }

    @Override
    public byte[] pack(ISOMessage msg) throws ISOException {
        try {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            DataOutputStream dos = new DataOutputStream(baos);

            String mti = msg.getMTI();
            if (mti != null) {
                dos.write(mti.getBytes(StandardCharsets.US_ASCII));
            }

            int maxField = msg.getMaxField();
            if (maxField > 64) {
                msg.getBitmap().setField(1);
            } else {
                msg.getBitmap().clearField(1);
            }

            packField(dos, 1, msg.getBitmap());

            for (int i = 2; i <= maxField; i++) {
                if (msg.hasField(i)) {
                    Object value = msg.get(i);
                    packField(dos, i, value);
                }
            }

            return baos.toByteArray();
        } catch (IOException e) {
            throw new ISOException("Error packing message", e);
        }
    }

    @Override
    public ISOMessage unpack(byte[] data) throws ISOException {
        try {
            ISOMessage msg = new ISOMessage();

            if (data.length < 4) {
                throw new ISOException("Message too short");
            }

            ByteBuffer buffer = ByteBuffer.wrap(data);

            byte[] mtiBytes = new byte[4];
            buffer.get(mtiBytes);
            msg.setMTI(new String(mtiBytes, StandardCharsets.US_ASCII));

            logger.debug("Unpacking message: MTI={}, total bytes={}, remaining after MTI={}",
                msg.getMTI(), data.length, buffer.remaining());

            if (buffer.remaining() < 8) {
                throw new ISOException("Missing bitmap");
            }

            unpackField(msg ,buffer, 1);

            for (int i = 2; i <= MAX_FIELDS; i++) {
                if (msg.getBitmap().hasField(i)) {
                    try {
                        unpackField(msg, buffer, i);
                    } catch (ISOException e) {
                        logger.error("Failed to unpack field {}: {}", i, e.getMessage());
                        throw new PackagerParseException("Failed to unpack field " + i, e, msg, data);
                    }
                }
            }

            msg.recalculateMaxField();
            return msg;
        } catch (PackagerParseException e) {
            throw e;
        } catch (Exception e) {
            throw new PackagerParseException("Error unpacking message", e, null, data);
        }
    }

    private void packField(DataOutputStream dos, int fieldNumber, Object value) throws IOException, ISOException {
        ISOFieldConfig config = fieldConfigs.get(fieldNumber);
        if (config == null) {
            throw new ISOException("No configuration for field " + fieldNumber);
        }

        ISOFieldPackager packager = config.getPackager();
        if (packager == null) {
            throw new ISOException("No packager configured for field " + fieldNumber);
        }

        packager.pack(dos, config, value);
    }

    private void unpackField(ISOMessage message, ByteBuffer buffer, int fieldNumber) throws ISOException {
        ISOFieldConfig config = fieldConfigs.get(fieldNumber);
        if (config == null) {
            throw new ISOException("No configuration for field " + fieldNumber);
        }

        logger.debug("Unpacking field {}: type={}, remaining bytes={}",
            fieldNumber, config.getType(), buffer.remaining());

        ISOFieldPackager packager = config.getPackager();
        if (packager == null) {
            throw new ISOException("No packager configured for field " + fieldNumber);
        }
        packager.unpack(message, buffer, config);
    }
}