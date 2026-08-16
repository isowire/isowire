package com.isowire.iso.packager;

import com.isowire.iso.ISODataElement;
import com.isowire.iso.ISOFieldConfig;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;

public abstract class ISOBaseVariableLengthPackager<T>  extends ISOBasePackager<T> implements ISOVariableLengthPackager {

    private final int LENGTH_INDICATOR_SIZE;
    private final int MAX_LENGTH;

    public ISOBaseVariableLengthPackager(int lengthIndicatorSize) {
        this.LENGTH_INDICATOR_SIZE = lengthIndicatorSize;
        this.MAX_LENGTH = (int) Math.pow(10, LENGTH_INDICATOR_SIZE) - 1;
    }

    @Override
    public int decodeLength(ByteBuffer buffer, int byteSize) {
        byte[] lengthBytes = new byte[byteSize];
        buffer.get(lengthBytes);
        return Integer.parseInt(new String(lengthBytes, StandardCharsets.US_ASCII));
    }

    @Override
    public void pack(DataOutputStream dos, ISOFieldConfig config, Object value) throws IOException, ISOException {

        byte[] data;

        if(config.hasSubFields()) {
            ByteArrayOutputStream ados = new ByteArrayOutputStream();
            DataOutputStream sdos = new DataOutputStream(ados);

            packSubfields(sdos, config, (ISODataElement) value);

            data = ados.toByteArray();
        } else {
            data = serialize((T) value, config);
        }

        if(data.length > MAX_LENGTH) {
            throw new ISOException("Invalid data length: " + LENGTH_INDICATOR_SIZE);
        }

        String lengthStr = String.format("%0" + LENGTH_INDICATOR_SIZE + "d", data.length);

        dos.write(lengthStr.getBytes(StandardCharsets.US_ASCII));
        dos.write(data);
    }

    @Override
    public void unpack(ISODataElement message, ByteBuffer buffer, ISOFieldConfig config) throws ISOException {
        int length = decodeLength(buffer, LENGTH_INDICATOR_SIZE);

        if (length < 0 || length > MAX_LENGTH) {
            throw new ISOException("Invalid variable length: " + LENGTH_INDICATOR_SIZE);
        }

        if (buffer.remaining() < length) {
            throw new ISOException("Not enough bytes for field " + config.getId() + ": need " + length + ", have " + buffer.remaining());
        }

        if (config.hasSubFields()) {
            int startPos = buffer.position();
            ByteBuffer fieldBuf = buffer.slice();
            fieldBuf.limit(length);
            unpackSubfields(message, fieldBuf, config);
            // Advance the original buffer by the field length
            buffer.position(startPos + length);
        } else {
            byte[] data = new byte[length];
            buffer.get(data);
            message.set(config.getId(), deserialize(data, config));
        }
    }

}
