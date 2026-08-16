package com.isowire.iso;

import com.isowire.iso.packager.ISOBaseVariableLengthPackager;

import java.nio.charset.StandardCharsets;

/**
 * LLLCHAR - Variable length alphanumeric field with 3-digit length indicator
 * Extends ISOBaseVariableLengthPackager for LLLCHAR field processing
 */
public class LLLCHAR extends ISOBaseVariableLengthPackager<String> {

    public LLLCHAR() {
        super(3);
    }

    @Override
    public byte[] serialize(String value, ISOFieldConfig config) {
        return value.toString().getBytes(StandardCharsets.US_ASCII);
    }

    @Override
    public String deserialize(byte[] value, ISOFieldConfig config) {
        return new String(value, StandardCharsets.US_ASCII);
    }
}
