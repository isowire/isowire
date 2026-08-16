package com.isowire.iso;


import com.isowire.iso.packager.ISOException;
import com.isowire.iso.packager.ISOBasePackager;
import com.isowire.iso.packager.ISOFixedValuePackager;

import java.io.DataOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;

/**
 * CHAR - Fixed length alphanumeric string
 * Extends ISOFixedValuePackager for character field processing
 */
public class CHAR extends ISOFixedValuePackager<String> {

    @Override
    public byte[] serialize(String value, ISOFieldConfig config) {
        value = String.format("%-" + config.getLength() + "s", value);
        return value.toString().getBytes(StandardCharsets.US_ASCII);
    }

    @Override
    public String deserialize(byte[] value, ISOFieldConfig config) {
        return new String(value, StandardCharsets.US_ASCII);
    }
}
