package com.bt.pi.core.messaging;

import javax.annotation.Resource;

import rice.Continuation;

import com.bt.pi.core.continuation.GenericContinuation;
import com.bt.pi.core.entity.EntityResponseCode;
import com.bt.pi.core.entity.PiEntity;
import com.bt.pi.core.message.ApplicationMessage;
import com.bt.pi.core.message.KoalaMessage;
import com.bt.pi.core.parser.KoalaPiEntityFactory;

public class KoalaMessageContinuation<T extends PiEntity> extends GenericContinuation<KoalaMessage> {

    private Continuation<T, Exception> continuation;
    private KoalaPiEntityFactory koalaPiEntityFactory;

    public KoalaMessageContinuation() {
        this(null, null);
    }

    public KoalaMessageContinuation(Continuation<T, Exception> piEntityContinuation, KoalaPiEntityFactory aKoalaPiEntityFactory) {
        super();
        continuation = piEntityContinuation;
        koalaPiEntityFactory = aKoalaPiEntityFactory;
    }

    @Resource
    public void setKoalaPiEntityFactory(KoalaPiEntityFactory aKoalaPiEntityFactory) {
        this.koalaPiEntityFactory = aKoalaPiEntityFactory;
    }

    @Override
    public void handleException(Exception arg0) {
        continuation.receiveException(arg0);
    }

    @SuppressWarnings("unchecked")
    @Override
    public void handleResult(KoalaMessage arg0) {
        if (arg0 instanceof ApplicationMessage) {
            ApplicationMessage appMessage = (ApplicationMessage) arg0;
            T payload = (T) koalaPiEntityFactory.getPiEntity(appMessage.getJson());

            if (EntityResponseCode.OK.equals(appMessage.getResponseCode()))
                continuation.receiveResult(payload);
            else
                continuation.receiveException(new KoalaMessageContinuationException(appMessage.getResponseCode(), payload));
        } else {
            continuation.receiveException(new RuntimeException("Unable to transform KoalaMessage into PiEntity" + arg0));
        }
    }

}
