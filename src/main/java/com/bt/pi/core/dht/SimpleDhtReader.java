package com.bt.pi.core.dht;

import java.util.concurrent.Executor;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import rice.Continuation;

import com.bt.pi.core.continuation.GenericContinuation;
import com.bt.pi.core.entity.PiEntity;
import com.bt.pi.core.id.PId;
import com.bt.pi.core.past.KoalaDHTStorage;
import com.bt.pi.core.util.Blocking;

@Component
@Scope("prototype")
public class SimpleDhtReader extends DhtClientBase<PiEntity> implements DhtReader, BlockingDhtReader {
    private static final Log LOG = LogFactory.getLog(SimpleDhtReader.class);

    public SimpleDhtReader() {
        super(null, null);
    }

    protected SimpleDhtReader(Executor executor, KoalaDHTStorage aKoalaDhtStorage) {
        super(executor, aKoalaDhtStorage);
    }

    @Blocking
    public PiEntity get(final PId id) {
        getAsync(id, (Continuation<PiEntity, Exception>) null);
        return blockUntilComplete();
    }

    @Blocking
    public PiEntity getAny(final PId id) {
        getAnyAsync(id, (Continuation<PiEntity, Exception>) null);
        return blockUntilComplete();
    }

    public <T extends PiEntity> void getAsync(final PId id, Continuation<T, Exception> continuation) {
        checkIfAlreadyUsed();
        doRead(id, continuation);
    }

    public <T extends PiEntity> void getAnyAsync(final PId id, Continuation<T, Exception> continuation) {
        checkIfAlreadyUsed();
        doQuickRead(id, continuation);
    }

    protected <T extends PiEntity> void doQuickRead(final PId id, final Continuation<T, Exception> continuation) {
        LOG.debug(String.format("Going to do a quick read of id %s", id.toStringFull()));
        getStorage().getAny(id, new DhtReaderContinuation<T>(this, continuation));
    }

    protected <T extends PiEntity> void doRead(final PId id, final Continuation<T, Exception> continuation) {
        LOG.debug(String.format("Going to read id %s", id.toStringFull()));
        getStorage().get(id, new DhtReaderContinuation<T>(this, continuation));
    }
}

class DhtReaderContinuation<T extends PiEntity> extends GenericContinuation<T> {
    private static final Log CONTINUATION_LOG = LogFactory.getLog(DhtReaderContinuation.class);
    private SimpleDhtReader simpleDhtReader;
    private Continuation<T, Exception> continuation;

    public DhtReaderContinuation(SimpleDhtReader dhtReader, Continuation<T, Exception> callingContinuation) {
        simpleDhtReader = dhtReader;
        continuation = callingContinuation;
    }

    @Override
    public void handleException(Exception e) {
        if (CONTINUATION_LOG.isDebugEnabled())
            CONTINUATION_LOG.debug(String.format("Received exception on read: %s", e));

        simpleDhtReader.receiveException(e);
        if (continuation != null)
            continuation.receiveException(e);
    }

    @Override
    public void handleResult(T result) {
        if (CONTINUATION_LOG.isDebugEnabled())
            CONTINUATION_LOG.debug(String.format("Received read result: %s", result));

        simpleDhtReader.receiveResult(result);
        if (continuation != null)
            continuation.receiveResult(result);
    }

}
