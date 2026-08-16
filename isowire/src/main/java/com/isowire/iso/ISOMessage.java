package com.isowire.iso;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.Serializable;
import java.util.*;

/**
 * ISOMessage - ISO8583 message data holder.
 * Contains MTI, bitmap, and field data. Packing/unpacking is handled by ISOPackager.
 */
public class ISOMessage extends ISODataElement implements Cloneable, Serializable {
    private static final long serialVersionUID = 1L;
    private static final Logger logger = LoggerFactory.getLogger(ISOMessage.class);
    private static final int MAX_FIELDS = 128;

    private String mti;
    private ISOBitMap bitmap;

    private int maxField = 0;

    public ISOMessage() {
        super(new HashMap<>());
        this.bitmap = new ISOBitMap(MAX_FIELDS);
    }

    public void setMTI(String mti) {
        this.mti = mti;
    }

    public String getMTI() {
        return mti;
    }

    public void set(int fieldNumber, Object value) {
        super.set(fieldNumber, value);

        if (value == null || value.toString().isEmpty()) {
            bitmap.clearField(fieldNumber);
            if (fieldNumber == maxField) {
                recalculateMaxField();
            }
        } else {
            bitmap.setField(fieldNumber);
            if (fieldNumber > maxField) {
                maxField = fieldNumber;
            }
        }
    }

    public boolean hasField(int fieldNumber) {
        return bitmap.hasField(fieldNumber);
    }

    public int[] getFieldNumbers() {
        List<Integer> present = new ArrayList<>();
        for (int i = 1; i <= maxField; i++) {
            if (hasField(i)) {
                present.add(i);
            }
        }
        int[] result = new int[present.size()];
        for (int i = 0; i < present.size(); i++) {
            result[i] = present.get(i);
        }
        return result;
    }

    public void unset(int fieldNumber) {
        set(fieldNumber, null);
    }

    public ISOBitMap getBitmap() {
        return bitmap;
    }

    public void setBitmap(ISOBitMap bitmap) {
        this.bitmap = bitmap;
    }

    public void recalculateMaxField() {
        maxField = bitmap.getMaxField();
    }

    public int getMaxField() {
        return maxField;
    }

    @Override
    public Object clone() {
        ISOMessage cloned = (ISOMessage) super.clone();
        cloned.bitmap = this.bitmap.clone();
        cloned.maxField = this.maxField;
        cloned.mti = this.mti;
        return cloned;
    }

    /**
     * Create a response message from this request message.
     * Automatically converts the MTI from request to response format (e.g., 0100 → 0110).
     *
     * @return cloned message with response MTI
     */
    public ISOMessage createResponse() {
        ISOMessage response = (ISOMessage) clone();
        if (mti != null && mti.length() == 4) {
            // Convert request MTI to response MTI (e.g., 0100 → 0110, 0200 → 0210)
            String responseMTI = mti.substring(0, 2) + "10";
            response.setMTI(responseMTI);
        }
        return response;
    }

    /**
     * Create a response message from this request message and copy specified fields.
     *
     * @param fieldsToCopy field numbers to copy from request to response
     * @return cloned response message with specified fields copied
     */
    public ISOMessage createResponse(int... fieldsToCopy) {
        ISOMessage response = createResponse();
        for (int fieldNum : fieldsToCopy) {
            if (hasField(fieldNum)) {
                response.set(fieldNum, get(fieldNum));
            }
        }
        return response;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("ISOMessage{");
        sb.append("MTI='").append(mti).append('\'');

        int[] fieldNumbers = getFieldNumbers();
        if (fieldNumbers.length > 0) {
            sb.append(", fields=[");
            for (int i = 0; i < fieldNumbers.length; i++) {
                if (i > 0) sb.append(", ");
                sb.append(fieldNumbers[i]).append("='").append(get(fieldNumbers[i])).append('\'');
            }
            sb.append("]");
        }

        sb.append("}");
        return sb.toString();
    }
}
