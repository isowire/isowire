package com.isowire.iso.packager;

import com.isowire.iso.ISODataElement;
import com.isowire.iso.ISOFieldConfig;

import java.io.DataOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;

/**
 * Sealed interface for ISO field packagers. Permits a limited set of implementations
 * to make reasoning about packager types and exhaustive switches possible.
 */
public interface ISOFieldPackager {
    void pack(DataOutputStream dos, ISOFieldConfig config, Object value) throws IOException, ISOException;

    void unpack(ISODataElement message, ByteBuffer buffer, ISOFieldConfig config) throws ISOException;
}
