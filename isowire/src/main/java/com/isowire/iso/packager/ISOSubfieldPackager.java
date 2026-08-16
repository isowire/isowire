package com.isowire.iso.packager;

import com.isowire.iso.ISODataElement;
import com.isowire.iso.ISOFieldConfig;

import java.io.DataOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;

public interface ISOSubfieldPackager {
    void packSubfields(DataOutputStream dos, ISOFieldConfig config, ISODataElement value) throws IOException, ISOException;

    void unpackSubfields(ISODataElement message, ByteBuffer buffer, ISOFieldConfig config)  throws ISOException;
}
