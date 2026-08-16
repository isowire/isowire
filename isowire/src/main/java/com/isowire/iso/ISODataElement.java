package com.isowire.iso;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

public class ISODataElement implements Cloneable, Serializable {

    private Map<Integer, Object> fields;

    public ISODataElement() {
        this(new HashMap<>());
    }

    public ISODataElement(Map<Integer, Object> fields) {
        this.fields = fields;
    }

    public void set(int fieldNumber, Object value) {
        if (value == null || value.toString().isEmpty()) {
            fields.remove(fieldNumber);
        } else {
            fields.put(fieldNumber, value);
        }
    }

    public Object get(int fieldNumber) {
        return fields.get(fieldNumber);
    }

    public String getString(int fieldNumber) {
        Object value = fields.get(fieldNumber);
        return value != null ? value.toString() : null;
    }

    @Override
    public Object clone() {
        try {
            ISODataElement cloned = (ISODataElement) super.clone();
            cloned.fields = new HashMap<>(this.fields);
            return cloned;
        } catch (CloneNotSupportedException e) {
            throw new RuntimeException("Clone not supported", e);
        }
    }

    @Override
    public String toString() {
        return fields.entrySet()
                .stream()
                .map(e -> e.getKey() + "='" + e.getValue() + "'")
                .collect(java.util.stream.Collectors.joining(", ", "[", "]"));
    }
}
