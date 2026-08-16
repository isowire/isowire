package com.isowire.iso.channel;

import com.isowire.iso.ISOMessage;
import com.isowire.iso.packager.ISOException;

public interface ISOChannel {

    void connect() throws ISOException;

    void disconnect() throws ISOException;

    boolean isConnected();

    void send(ISOMessage msg) throws ISOException;

    ISOMessage receive() throws ISOException;

    ISOMessage sendAndWait(ISOMessage msg, long timeout) throws ISOException;

    String getName();

    void setName(String name);

    int[] getSupportedFieldNumbers();

}
