package com.bt.pi.core.util;

import org.apache.commons.lang.builder.EqualsBuilder;
import org.apache.commons.lang.builder.HashCodeBuilder;

public class MutableString {
    private String value;

    public MutableString() {
        value = null;
    }

    public MutableString(String aString) {
        value = aString;
    }

    public String get() {
        return value;
    }

    public void set(String newValue) {
        value = newValue;
    }

    @Override
    public String toString() {
        return value;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == null || !(obj instanceof String || obj instanceof MutableString))
            return false;

        return new EqualsBuilder().append(value, obj.toString()).isEquals();
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder().append(value).toHashCode();
    };
}
