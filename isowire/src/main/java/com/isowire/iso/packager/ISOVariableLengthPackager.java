package com.isowire.iso.packager;

import java.nio.ByteBuffer;

public interface ISOVariableLengthPackager {
    int decodeLength(ByteBuffer buffer, int lengthBytes);
}
