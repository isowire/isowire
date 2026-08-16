package com.isowire.iso;

import com.isowire.iso.packager.ISOFixedValuePackager;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;

/**
 * AMOUNT - Fixed length numeric amount field (typically 12 digits)
 * Used for transaction amounts in ISO8583 messages
 * Amounts are stored as cents (multiplied by 100 for packing, divided by 100 for unpacking)
 * Extends NUMERIC for amount field processing
 */
public class AMOUNT extends ISOFixedValuePackager<String> {
    @Override
    public byte[] serialize(String value, ISOFieldConfig config) {
        if (value == null) {
            value = "0";
        }

        long number = new BigDecimal(value)
                .movePointRight(2)
                .longValueExact();

        String result = String.format(
                "%0" + config.getLength() + "d",
                number
        );

        return result.getBytes(StandardCharsets.US_ASCII);
    }

    @Override
    public String deserialize(byte[] data, ISOFieldConfig config) {
        String value = new String(data, StandardCharsets.US_ASCII);
        return new BigDecimal(value).movePointLeft(2).toString();
    }
}
