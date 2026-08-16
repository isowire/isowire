package com.isowire.iso;

import java.io.Serializable;
import java.util.BitSet;

/**
 * BitMap - Wrapper around BitSet for ISO8583 field presence tracking.
 * Handles conversion between field numbers (1-based) and bit indices (0-based).
 *
 * Field numbering: 1 = Bitmap, 2-128 = Data fields
 * Bit indexing: bit 0 = field 1, bit 1 = field 2, etc.
 */
public class ISOBitMap implements Serializable, Cloneable {
    private static final long serialVersionUID = 1L;

    private final int maxFields;
    private final BitSet bitSet;

    /**
     * Create empty bitmap with all bits cleared.
     * @param maxFields maximum number of fields
     */
    public ISOBitMap(int maxFields) {
        this.maxFields = maxFields;
        this.bitSet = new BitSet(maxFields);
    }

    /**
     * Create bitmap from existing BitSet.
     * @param bitSet existing BitSet (assumed 0-based for field 1)
     * @param maxFields maximum number of fields
     */
    public ISOBitMap(BitSet bitSet, int maxFields) {
        this.maxFields = maxFields;
        this.bitSet = bitSet;
    }

    public void setField(int fieldNumber) {
        if (fieldNumber < 1 || fieldNumber > maxFields) {
            throw new IllegalArgumentException("Field number must be 1-" + maxFields + ": " + fieldNumber);
        }
        bitSet.set(fieldNumber - 1);
    }

    public boolean hasField(int fieldNumber) {
        if (fieldNumber < 1 || fieldNumber > maxFields) {
            return false;
        }
        return bitSet.get(fieldNumber - 1);
    }

    public void clearField(int fieldNumber) {
        if (fieldNumber < 1 || fieldNumber > maxFields) {
            throw new IllegalArgumentException("Field number must be 1-" + maxFields + ": " + fieldNumber);
        }
        bitSet.clear(fieldNumber - 1);
    }

    public BitSet getBitSet() {
        return bitSet;
    }

    public int getMaxField() {
        // BitSet.length() returns the index of the highest set bit plus one (0 if none).
        int len = bitSet.length();
        return Math.min(len, maxFields);
    }

    @Override
    public ISOBitMap clone() {
        return new ISOBitMap((BitSet) bitSet.clone(), maxFields);
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder("BitMap[");
        boolean first = true;
        for (int i = 1; i <= maxFields; i++) {
            if (hasField(i)) {
                if (!first) sb.append(", ");
                sb.append(i);
                first = false;
            }
        }
        sb.append("]");
        return sb.toString();
    }
}
