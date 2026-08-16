package com.isowire.iso;


import com.isowire.iso.packager.ISOFixedValuePackager;
import java.nio.charset.StandardCharsets;

/**
 * NUMERIC - Fixed length numeric string
 * Extends ISOFixedValuePackager for numeric field processing
 */
public class NUMERIC extends ISOFixedValuePackager<String> {

    @Override
    public byte[] serialize(String value, ISOFieldConfig config) {
        if (value == null) {
            value = "0";
        }

        String result = String.format(
                "%0" + config.getLength() + "d",
                Long.valueOf(value)
        );

        return result.getBytes(StandardCharsets.US_ASCII);
    }

    @Override
    public String deserialize(byte[] data, ISOFieldConfig config) {
        return new String(data, StandardCharsets.US_ASCII);
    }
}
