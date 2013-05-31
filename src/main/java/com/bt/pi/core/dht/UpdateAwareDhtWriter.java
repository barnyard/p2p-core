package com.bt.pi.core.dht;

import java.util.Random;
import java.util.concurrent.Executor;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import rice.Continuation;

import com.bt.pi.core.continuation.GenericContinuation;
import com.bt.pi.core.continuation.UpdateResolver;
import com.bt.pi.core.continuation.UpdateResolvingContinuation;
import com.bt.pi.core.continuation.UpdateResolvingPiContinuation;
import com.bt.pi.core.entity.Deletable;
import com.bt.pi.core.entity.PiEntity;
import com.bt.pi.core.exception.KoalaContentVersionMismatchException;
import com.bt.pi.core.id.PId;
import com.bt.pi.core.past.KoalaDHTStorage;
import com.bt.pi.core.util.Blocking;

@Component
@Scope("prototype")
public class UpdateAwareDhtWriter extends UnconditionalDhtWriter implements DhtWriter, BlockingDhtWriter {
    protected static final int WAIT_FACTOR_MILLIS = 100;
    private static final Log LOG = LogFactory.getLog(UpdateAwareDhtWriter.class);
    private static final int DEFAULT_MAX_NUM_VERSION_MISMATCH_RETRIES = 10;
    private int maxNumVersionMismatchRetries;
    private volatile int versionMismatchRetriesLeft;
    private Random rand;
    private UpdateStateObject<? extends PiEntity> updateStateObject;

    public UpdateAwareDhtWriter() {
        super(null, null);
        maxNumVersionMismatchRetries = DEFAULT_MAX_NUM_VERSION_MISMATCH_RETRIES;
        versionMismatchRetriesLeft = maxNumVersionMismatchRetries;
        this.rand = new Random();
        updateStateObject = null;
    }

    protected UpdateAwareDhtWriter(Executor anExecutor, KoalaDHTStorage aKoalaDhtStorage) {
        super(anExecutor, aKoalaDhtStorage);
        maxNumVersionMismatchRetries = DEFAULT_MAX_NUM_VERSION_MISMATCH_RETRIES;
        versionMismatchRetriesLeft = maxNumVersionMismatchRetries;
        this.rand = new Random();
        updateStateObject = null;
    }

    @Override
    @Blocking
    public <T extends PiEntity> void put(PId id, T entity) {
        super.put(id, entity);
    }

    public void setMaxNumVersionMismatchRetries(int aMaxNumVersionMismatchRetries) {
        int prev = this.maxNumVersionMismatchRetries;
        this.maxNumVersionMismatchRetries = aMaxNumVersionMismatchRetries;
        this.versionMismatchRetriesLeft = versionMismatchRetriesLeft + (aMaxNumVersionMismatchRetries - prev);
        LOG.debug(String.format("Version mismatch retries left was %d but is now %d", prev, versionMismatchRetriesLeft));
    }

    protected void setVersionMismatchRetriesLeft(int aVersionMismatchRetriesLeft) {
        this.versionMismatchRetriesLeft = aVersionMismatchRetriesLeft;
    }

    @Blocking
    public <T extends PiEntity> void update(final PId aId, final T aEntity, final UpdateResolver<T> aUpdateResolver) {
        if (LOG.isDebugEnabled())
            LOG.debug(String.format("update() Blocking - Going to update entity %s (%s) - %d retries left", aId.toStringFull(), aEntity, versionMismatchRetriesLeft));

        this.update(aId, aEntity, new UpdateResolvingPiContinuation<T>() {
            @Override
            public T update(T existingEntity, T requestedEntity) {
                LOG.debug(String.format("Going to resolve update for write to %s", aId.toStringFull()));
                T resultEntity = aUpdateResolver.update(existingEntity, requestedEntity);
                if (LOG.isDebugEnabled())
                    LOG.debug(String.format("UpdateResolver %s returned result %s for id %s", aUpdateResolver, resultEntity, aId));
                return resultEntity;
            }

            @Override
            public void handleResult(PiEntity result) {
            }
        });
        blockUntilComplete();
    }

    @Blocking
    public boolean writeIfAbsent(final PId aId, final PiEntity piEntity) {
        if (LOG.isDebugEnabled())
            LOG.debug(String.format("writeIfAbsent(%s, %s)", aId.toStringFull(), piEntity));
        update(aId, piEntity, new UpdateResolver<PiEntity>() {
            @Override
            public PiEntity update(PiEntity existingEntity, PiEntity requestedEntity) {
                if (LOG.isDebugEnabled())
                    LOG.debug(String.format("update(%s, %s)", existingEntity, requestedEntity));
                if (existingEntity != null) {
                    if (isDeletableAndDeleted(aId, existingEntity)) {
                        requestedEntity.setVersion(existingEntity.getVersion());
                        return requestedEntity;
                    }
                    LOG.warn(String.format("Tried to write entity of type %s, but found that an entry for that key already exists: %s", requestedEntity.getClass().getName(), existingEntity));
                    return null;
                }
                return requestedEntity;
            }
        });
        boolean written = getValueWritten() != null;
        if (written)
            if (LOG.isDebugEnabled())
                LOG.debug(String.format("Wrote entity %s under key %s", piEntity, aId));
        return written;
    }

    private boolean isDeletableAndDeleted(final PId id, final PiEntity piEntity) {
        if (piEntity instanceof Deletable) {
            Deletable deletable = (Deletable) piEntity;
            LOG.debug(String.format("%s: %s is deletable and %s deleted", piEntity, id.toStringFull(), deletable.isDeleted() ? "IS" : "is NOT"));
            if (deletable.isDeleted())
                return true;
        }
        return false;
    }

    public <T extends PiEntity> void update(final PId aId, final UpdateResolvingContinuation<T, Exception> aUpdateResolvingContinuation) {
        update(aId, null, aUpdateResolvingContinuation);
    }

    public <T extends PiEntity> void update(final PId aId, final T aEntity, final UpdateResolvingContinuation<T, Exception> aUpdateResolvingContinuation) {
        LOG.debug(String.format("update() Async - Going to update entity %s (%s) - %d retries left", aId.toStringFull(), aEntity, versionMismatchRetriesLeft));
        checkIfAlreadyUsed();
        updateStateObject = new UpdateStateObject<T>(aId, aEntity, aUpdateResolvingContinuation);
        doUpdate(updateStateObject);
    }

    protected <T extends PiEntity> void doUpdate(final UpdateStateObject<T> aUpdateStateObject) {
        getStorage().get(aUpdateStateObject.getId(), new GenericContinuation<T>() {
            @Override
            public void handleException(Exception e) {
                LOG.debug(String.format("Received exception on read: %s", e));
                UpdateAwareDhtWriter.this.receiveException(e);
            }

            @Override
            public void handleResult(T result) {
                LOG.debug(String.format("Received read result: %s", result));

                // call update resolver and try to get what we want to write.
                T entityToWrite = aUpdateStateObject.getUpdateResolvingContinuation().update(result, aUpdateStateObject.getEntity());
                if (entityToWrite == null) {
                    LOG.debug(String.format("Null entity will not be written"));
                    UpdateAwareDhtWriter.this.handleResultOnWrite(null, aUpdateStateObject.getUpdateResolvingContinuation());
                    return;
                }

                LOG.debug(String.format("Entity to be written: %s", entityToWrite));
                doWrite(aUpdateStateObject.getId(), entityToWrite, aUpdateStateObject.getUpdateResolvingContinuation());
            }
        });
    }

    @Override
    protected void handleExceptionOnWrite(Exception e, Continuation<?, Exception> continuation) {
        LOG.debug(String.format("handleExceptionOnWrite(Exception - %s, Continuation - %s)", e, continuation));
        if (!(e instanceof KoalaContentVersionMismatchException)) {
            LOG.debug(String.format("Processing version mismatch and passing it on to the parent."));
            super.handleExceptionOnWrite(e, continuation);
            return;
        }

        versionMismatchRetriesLeft--;
        if (versionMismatchRetriesLeft < 1) {
            String idInfoStr = null;
            if (updateStateObject != null && updateStateObject.getId() != null) {
                idInfoStr = updateStateObject.getId().toStringFull();
            }
            LOG.debug(String.format("No more retries left for write of %s", idInfoStr));
            super.handleExceptionOnWrite(new DhtOperationMaximumRetriesExceededException(String.format("Write of %s exceeded %d retries", idInfoStr, maxNumVersionMismatchRetries)), continuation);
            return;
        }

        try {
            Thread.sleep(getWaitTimeMillis());
        } catch (InterruptedException ie) {
            LOG.error(ie.getMessage(), ie);
        }
        doUpdate(updateStateObject);
    }

    protected long getWaitTimeMillis() {
        return (maxNumVersionMismatchRetries - versionMismatchRetriesLeft + 1) * (WAIT_FACTOR_MILLIS + rand.nextInt((int) WAIT_FACTOR_MILLIS));
    }

}

class UpdateStateObject<T extends PiEntity> {
    private PId id;
    private T entity;
    private UpdateResolvingContinuation<T, Exception> updateResolvingContinuation;

    public UpdateStateObject(PId aId, T aEntity, UpdateResolvingContinuation<T, Exception> aUpdateResolvingContinuation) {
        this.id = aId;
        this.entity = aEntity;
        this.updateResolvingContinuation = aUpdateResolvingContinuation;
    }

    public PId getId() {
        return id;
    }

    public T getEntity() {
        return entity;
    }

    public UpdateResolvingContinuation<T, Exception> getUpdateResolvingContinuation() {
        return updateResolvingContinuation;
    }
}