package com.isowire.iso.packager;

import com.isowire.iso.ISODataElement;
import com.isowire.iso.ISOFieldConfig;

import java.io.DataOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.Arrays;

public abstract class ISOFixedValuePackager<T> extends ISOBasePackager<T> {

    @Override
    public void pack(DataOutputStream dos, ISOFieldConfig config, Object value) throws IOException, ISOException {
        if(config.hasSubFields()) {
            packSubfields(dos, config, (ISODataElement) value);
        } else {

            if (value == null) {
                value = "";
            }

            byte[] data = serialize((T) value, config);

            if (data.length > config.getLength()) {
                data = Arrays.copyOf(data, config.getLength());
            }

            dos.write(data);
        }
    }

    @Override
    public void unpack(ISODataElement message, ByteBuffer buffer, ISOFieldConfig config) throws ISOException {
        if(config.hasSubFields()) {
            unpackSubfields(message, buffer, config);
        } else {
            byte[] data = new byte[config.getLength()];
            buffer.get(data);

            message.set(config.getId(), deserialize(data, config));
        }
    }
}
