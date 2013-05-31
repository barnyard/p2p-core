//(c) British Telecommunications plc, 2009, All Rights Reserved
package com.bt.pi.core.messaging;

import com.bt.pi.core.id.PId;
import com.bt.pi.core.message.KoalaMessage;

public interface RequestWrapper<ResponseType extends KoalaMessage> {
    boolean messageReceived(PId id, ResponseType message);
}
