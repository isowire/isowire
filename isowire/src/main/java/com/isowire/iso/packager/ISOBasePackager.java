package com.isowire.iso.packager;

import com.isowire.iso.ISODataElement;
import com.isowire.iso.ISOFieldConfig;

import java.io.DataOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.HashMap;

public abstract class ISOBasePackager<T> implements ISOFieldPackager, ISOSubfieldPackager, ISOValueSerializer<T> {

    @Override
    public void packSubfields(DataOutputStream dos, ISOFieldConfig config, ISODataElement value) throws IOException, ISOException {
        for(ISOFieldConfig sfc: config.getSubFields()) {
            sfc.getPackager().pack(dos, sfc, value.get(sfc.getId()));
        }
    }

    @Override
    public void unpackSubfields(ISODataElement message, ByteBuffer buffer, ISOFieldConfig config)  throws ISOException {
        final ISODataElement subFieldDataElement = new ISODataElement(new HashMap<>());

        for (ISOFieldConfig conf : config.getSubFields()) {
            ISOFieldPackager packager = conf.getPackager();
            packager.unpack(subFieldDataElement, buffer, conf);
        }

        message.set(config.getId(), subFieldDataElement);
    }
}
