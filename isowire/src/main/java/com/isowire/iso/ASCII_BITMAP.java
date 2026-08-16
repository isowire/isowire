package com.isowire.iso;

import com.isowire.iso.packager.ISOException;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;

public final class ASCII_BITMAP extends BITMAP {

    @Override
    public void pack(DataOutputStream dos, ISOFieldConfig config, Object value) throws IOException, ISOException {
        ByteArrayOutputStream ados = new ByteArrayOutputStream();
        super.pack(new DataOutputStream(ados), config, value);
        dos.write(toHexBytes(ados.toByteArray()));
    }

    @Override
    public void unpack(ISODataElement message, ByteBuffer buffer, ISOFieldConfig config) throws ISOException {
        if (buffer.remaining() < 16) {
            throw new ISOException("Not enough data for bitmap: need at least 16 bytes, have " + buffer.remaining());
        }

        byte[] firstHex = new byte[16];
        buffer.get(firstHex);

        byte[] first = toRawBytes(firstHex);

        ISOBitMap bitMap = bytesToBitMap(first);

        if(!bitMap.hasField(1)) {
            ((ISOMessage) message).setBitmap(bitMap);
        }

        if (buffer.remaining() < 6) {
            throw new ISOException("Bitmap indicates extended bitmap but no additional 8 bytes available");
        }

        byte[] secondHex = new byte[16];
        buffer.get(secondHex);

        byte[] second = toRawBytes(secondHex);

        byte[] extendedBitmapBytes = new byte[BITMAP_SIZE];

        System.arraycopy(first, 0, extendedBitmapBytes, 0, first.length);
        System.arraycopy(second, 0, extendedBitmapBytes, first.length, second.length);

        ((ISOMessage) message).setBitmap(bytesToBitMap(extendedBitmapBytes));
    }

    /**
     * Convert hex string to byte array (for parseBitmap)
     */
    public byte[] toRawBytes(byte[] hexBytes) {
        if (hexBytes.length % 2 != 0) {
            throw new IllegalArgumentException("Hex byte array length must be even");
        }

        byte[] out = new byte[hexBytes.length / 2];
        for (int i = 0; i < hexBytes.length; i += 2) {
            int high = decodeNibble(hexBytes[i]);
            int low  = decodeNibble(hexBytes[i + 1]);
            out[i / 2] = (byte) ((high << 4) | low);
        }
        return out;
    }

    private int decodeNibble(byte b) {
        if (b >= '0' && b <= '9') return b - '0';
        if (b >= 'A' && b <= 'F') return b - 'A' + 10;
        if (b >= 'a' && b <= 'f') return b - 'a' + 10;
        throw new IllegalArgumentException("Invalid ASCII Hex byte: " + b);
    }

    /**
     * Convert byte array to hex string
     */
    private byte[] toHexBytes(byte[] bytes) {
        StringBuilder sb = new StringBuilder(BITMAP_SIZE * 2);
        for (byte b : bytes) {
            sb.append(String.format("%02X", b));
        }
        return sb.toString().getBytes(StandardCharsets.US_ASCII);
    }
}
