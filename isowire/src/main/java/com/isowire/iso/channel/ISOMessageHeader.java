package com.isowire.iso.channel;

public interface ISOMessageHeader extends ISOMessageLength {
    void setHeader(byte[] header);

    default void setHeader(String header) {
        this.setHeader(header.getBytes());
    }

    /**
     * Get additional header bytes that are sent after the message length indicator
     * and before the actual message data.
     * @return header bytes, empty array if no header is set
     */
    default byte[] getHeader() {
        return new byte[0];
    }

    /**
     * Get header as string for easy logging and debugging.
     * @return header as string, empty string if no header is set
     */
    default String getHeaderAsString() {
        byte[] header = getHeader();
        return header.length > 0 ? new String(header) : "";
    }

}
