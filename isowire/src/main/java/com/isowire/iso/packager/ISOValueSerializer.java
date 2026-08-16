package com.isowire.iso.packager;

import com.isowire.iso.ISOFieldConfig;

public interface ISOValueSerializer<T> {
    byte[] serialize(T value, ISOFieldConfig config);
    T deserialize(byte[] value, ISOFieldConfig config);
}
