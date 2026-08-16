package com.isowire.iso;

import com.isowire.iso.packager.*;

import java.io.DataOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.BitSet;

/**
 * BITMAP - ISO8583 Bitmap field packager
 *
 * The bitmap indicates which fields are present in the ISO8583 message.
 * - Standard bitmap: 16 bytes (128 bits) for fields 1-128
 * - Each bit represents a field: bit 0 = field 1, bit 1 = field 2, etc.
 * - If bit 0 is set, there's an extended bitmap for fields 65-128
 *
 * Format: Binary (16 bytes)
 */
public class BITMAP extends ISOBitMapPackager {

    @Override
    public void pack(DataOutputStream dos, ISOFieldConfig config, Object value) throws IOException, ISOException {
        ISOBitMap bitMap = (ISOBitMap) value;
        int maxField = bitMap.getMaxField();
        // If any field > 64 is present or the extended indicator (field 1) is set, write 16 bytes; otherwise write 8.
        int bytesToWrite = (maxField > 64 || bitMap.hasField(1)) ? 16 : 8;
        byte[] fullBytes = bitMapToBytes(bitMap);
        dos.write(fullBytes, 0, bytesToWrite);
    }

    @Override
    public void unpack(ISODataElement message, ByteBuffer buffer, ISOFieldConfig config) throws ISOException {
        if (buffer.remaining() < 8) {
            throw new ISOException("Not enough data for bitmap: need at least 8 bytes, have " + buffer.remaining());
        }

        byte[] first = new byte[8];
        buffer.get(first);

        ISOBitMap bitMap = bytesToBitMap(first);

        if(!bitMap.hasField(1)) {
            ((ISOMessage) message).setBitmap(bitMap);
        }

        if (buffer.remaining() < 8) {
            throw new ISOException("Bitmap indicates extended bitmap but no additional 8 bytes available");
        }

        byte[] second = new byte[8];
        buffer.get(second);

        byte[] extendedBitmapBytes = new byte[BITMAP_SIZE];

        System.arraycopy(first, 0, extendedBitmapBytes, 0, first.length);
        System.arraycopy(second, 0, extendedBitmapBytes, first.length, second.length);

        ((ISOMessage) message).setBitmap(bytesToBitMap(extendedBitmapBytes));
    }
}
