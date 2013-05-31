package com.bt.pi.core.dht;

import java.util.concurrent.Executor;

import javax.annotation.Resource;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import com.bt.pi.core.past.KoalaDHTStorage;

/**
 * DhtClientFactory should be injected via spring to handle the creation of classes used to read and write from the DHT.
 * 
 */
public abstract class DhtClientFactory {
    private static final Log LOG = LogFactory.getLog(DhtClientFactory.class);
    private KoalaDHTStorage koalaDhtStorage;
    private Executor executor;

    public DhtClientFactory() {
        this.koalaDhtStorage = null;
    }

    public void setKoalaDhtStorage(KoalaDHTStorage aKoalaDhtStorage) {
        LOG.debug(String.format("Injecting dht storage %s", aKoalaDhtStorage));
        this.koalaDhtStorage = aKoalaDhtStorage;
    }

    @Resource(type = ThreadPoolTaskExecutor.class)
    public void setExecutor(Executor anExecutor) {
        LOG.debug(String.format("Injecting executor storage %s", anExecutor));
        executor = anExecutor;
    }

    /**
     * Creates a standard async writer that can be used for a single write/update to the DHT.
     * 
     * @return DhtWriter
     */
    public DhtWriter createWriter() {

        return new UpdateAwareDhtWriter(executor, koalaDhtStorage);
    }

    /**
     * Creates a blocking writer that can be used for a single write/update to the DHT.
     * 
     * Note: Blocking writers should be used with care due to the fact that using one within the Selector thread can
     * impede Pi's performance.
     * 
     * @return DhtWriter
     */
    public BlockingDhtWriter createBlockingWriter() {
        BlockingDhtWriter writer = createEmptyBlockingWriter();
        writer.setExecutor(executor);
        writer.setStorage(koalaDhtStorage);
        return writer;
    }

    /**
     * Creates a standard async DhtReader which can be used for a single dht read.
     * 
     * @return DhtReader
     */
    public DhtReader createReader() {
        return new SimpleDhtReader(executor, koalaDhtStorage);
    }

    /**
     * 
     * Creates a BlockingDhtReader which can be used for a single dht read.
     * 
     * Note: Blocking readers should be used with care due to the fact that using one within the Selector thread can
     * impede Pi's performance.
     * 
     * @return BlockingDhtReader
     */
    public BlockingDhtReader createBlockingReader() {
        BlockingDhtReader reader = createEmptyBlockingReader();
        reader.setExecutor(executor);
        reader.setStorage(koalaDhtStorage);
        return reader;
    }

    protected abstract BlockingDhtWriter createEmptyBlockingWriter();

    protected abstract BlockingDhtReader createEmptyBlockingReader();

}
