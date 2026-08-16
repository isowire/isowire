package com.isowire.iso;

import com.isowire.iso.packager.ISOBaseVariableLengthPackager;

import java.nio.charset.StandardCharsets;

/**
 * LLCHAR - Variable length alphanumeric field with 2-digit length indicator
 * Extends ISOBaseVariableLengthPackager for LLCHAR field processing
 */
public class LLCHAR extends ISOBaseVariableLengthPackager<String> {

    public LLCHAR() {
        super(2);
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
