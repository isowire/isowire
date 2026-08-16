package com.isowire.iso.channel;

public interface ISOServerChannel extends ISOChannel{
    void setSocket(java.net.Socket socket) throws java.io.IOException;
}
