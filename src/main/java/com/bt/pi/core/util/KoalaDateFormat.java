//(c) British Telecommunications plc, 2009, All Rights Reserved
package com.bt.pi.core.util;

import java.text.SimpleDateFormat;

import org.springframework.stereotype.Component;

@Component
public class KoalaDateFormat extends SimpleDateFormat {
    public static final String DATE_FORMAT = "yyyy.MM.dd";
    private static final long serialVersionUID = 8599101542606499722L;

    public KoalaDateFormat() {
        super(DATE_FORMAT);
    }
}
