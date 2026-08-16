package com.isowire.iso.channel;

public interface ISOClientChannel extends ISOChannel{
    void setTransportChannel(TransportChannel rawChannel);
}
