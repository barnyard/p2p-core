package com.bt.pi.core.dht;

/**
 * 
 * Implementationn of DhtClientFactory for tests.
 * 
 */
public class SubDhtClientFactory extends DhtClientFactory {

    public SubDhtClientFactory() {

    }

    @Override
    protected UpdateAwareDhtWriter createEmptyBlockingWriter() {
        return new UpdateAwareDhtWriter();
    }

    @Override
    protected SimpleDhtReader createEmptyBlockingReader() {
        return new SimpleDhtReader();
    }

}