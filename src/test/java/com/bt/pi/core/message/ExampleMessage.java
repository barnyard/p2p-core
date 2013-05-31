//(c) British Telecommunications plc, 2009, All Rights Reserved
package com.bt.pi.core.message;

import com.bt.pi.core.entity.EntityResponseCode;

@SuppressWarnings("serial")
public class ExampleMessage extends KoalaMessageBase {
    private String moo;

    public ExampleMessage() {
        super("test");
        moo = "se";
    }

    public String getMoo() {
        return moo;
    }

    @Override
    public EntityResponseCode getResponseCode() {
        return null;
    }
}