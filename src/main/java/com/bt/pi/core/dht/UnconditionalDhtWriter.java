package com.bt.pi.core.dht;

import java.util.Arrays;
import java.util.concurrent.Executor;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import rice.Continuation;

import com.bt.pi.core.continuation.GenericContinuation;
import com.bt.pi.core.entity.PiEntity;
import com.bt.pi.core.id.PId;
import com.bt.pi.core.past.KoalaDHTStorage;
import com.bt.pi.core.past.content.VersionedContent;

public class UnconditionalDhtWriter extends DhtClientBase<PiEntity> {
    private static final Log LOG = LogFactory.getLog(UnconditionalDhtWriter.class);
    private PiEntity valueWritten;

    protected UnconditionalDhtWriter(Executor anExecutor, KoalaDHTStorage aKoalaDhtStorage) {
        super(anExecutor, aKoalaDhtStorage);
        valueWritten = null;
    }

    public <T extends PiEntity> void put(final PId id, final T entity) {
        put(id, entity, (Continuation<PiEntity, Exception>) null);
        blockUntilComplete();
    }

    public <T extends PiEntity> void put(final PId id, final T entity, Continuation<T, Exception> continuation) {
        checkIfAlreadyUsed();
        doWrite(id, entity, continuation);
    }

    protected <T extends PiEntity> void doWrite(final PId id, final T entityToWrite, final Continuation<T, Exception> continuation) {
        entityToWrite.incrementVersion();

        if (LOG.isDebugEnabled()) {
            LOG.debug(String.format("Incremented version number for %s to %d", id.toStringFull(), ((VersionedContent) entityToWrite).getVersion()));
            LOG.debug(String.format("Going to write entity %s", entityToWrite));
        }

        getStorage().put(id, entityToWrite, new GenericContinuation<Boolean[]>() {
            @Override
            public void handleException(Exception e) {
                if (LOG.isDebugEnabled())
                    LOG.debug(String.format("receiveException(%s)", e));

                handleExceptionOnWrite(e, continuation);
            }

            @Override
            public void handleResult(Boolean[] result) {
                if (LOG.isDebugEnabled())
                    LOG.debug(String.format("receiveResult(%s)", Arrays.toString(result)));

                valueWritten = entityToWrite;
                handleResultOnWrite(entityToWrite, continuation);
            }
        });
    }

    protected void handleExceptionOnWrite(Exception e, Continuation<?, Exception> continuation) {
        LOG.debug(String.format("Received exception on write: %s", e));
        super.receiveException(e);
        if (continuation != null)
            continuation.receiveException(e);
    }

    protected <T extends PiEntity> void handleResultOnWrite(T result, Continuation<T, Exception> continuation) {
        LOG.debug(String.format("Received result: %s", result));
        super.receiveResult(result);
        if (continuation != null)
            continuation.receiveResult(result);
    }

    public PiEntity getValueWritten() {
        return valueWritten;
    }
}