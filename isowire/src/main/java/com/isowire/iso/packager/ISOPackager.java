package com.isowire.iso.packager;

import com.isowire.iso.ISOMessage;

/**
 * ISO8583 Message Packager Interface
 * Handles packing/unpacking of ISO8583 messages with field configurations.
 */
public interface ISOPackager {

    /**
     * Pack ISOMessage to byte array.
     * @param msg message to pack
     * @return packed message bytes
     * @throws ISOException if packing fails
     */
    byte[] pack(ISOMessage msg) throws ISOException;

    /**
     * Unpack byte array to ISOMessage.
     * @param data packed message bytes
     * @return unpacked message
     * @throws ISOException if unpacking fails
     */
    ISOMessage unpack(byte[] data) throws ISOException;
}
