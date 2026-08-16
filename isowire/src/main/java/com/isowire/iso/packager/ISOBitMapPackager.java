package com.isowire.iso.packager;

import com.isowire.iso.ISOBitMap;
import java.util.BitSet;

public abstract class ISOBitMapPackager implements ISOFieldPackager {

    protected static final int BITMAP_SIZE = 16;
    protected static final int MAX_FIELDS = 128;

    /**
     * Convert BitMap directly to byte array (optimized)
     * @param bitMap BitMap where bit N-1 represents field N (0-based: bit 0 = field 1)
     * @return 16-byte bitmap array
     */
    public byte[] bitMapToBytes(ISOBitMap bitMap) {
        byte[] bitmap = new byte[BITMAP_SIZE];
        BitSet bitSet = bitMap.getBitSet();

        for (int bitIndex = 0; bitIndex < MAX_FIELDS; bitIndex++) {
            if (bitSet.get(bitIndex)) {
                int byteIndex = bitIndex / 8;
                int bitPosition = bitIndex % 8;
                bitmap[byteIndex] |= (byte) (1 << (7 - bitPosition));
            }
        }

        return bitmap;
    }

    /**
     * Convert byte array directly to BitMap (optimized)
     * @param bytes bitmap bytes
     * @return BitMap where bit N-1 represents field N (0-based: bit 0 = field 1, bit 1 = field 2)
     */
    public ISOBitMap bytesToBitMap(byte[] bytes) {
        BitSet bitSet = new BitSet(MAX_FIELDS);

        for (int byteNum = 0; byteNum < bytes.length && byteNum < BITMAP_SIZE; byteNum++) {
            for (int bitNum = 0; bitNum < 8; bitNum++) {
                int bitIndex = byteNum * 8 + bitNum; // 0-based bit index
                int fieldNum = bitIndex + 1; // 1-based field number for validation
                if (fieldNum <= MAX_FIELDS && ((bytes[byteNum] & (1 << (7 - bitNum))) != 0)) {
                    bitSet.set(bitIndex); // Set 0-based bit index
                }
            }
        }

        return new ISOBitMap(bitSet, MAX_FIELDS);
    }
}
