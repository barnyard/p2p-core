package com.bt.pi.core.past;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.when;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import rice.p2p.commonapi.Id;
import rice.p2p.commonapi.IdFactory;
import rice.p2p.commonapi.IdSet;
import rice.p2p.past.gc.GCId;
import rice.p2p.past.gc.GCIdFactory;
import rice.p2p.past.gc.GCPastMetadata;
import rice.persistence.StorageManager;

import com.bt.pi.core.id.KoalaIdFactory;
import com.bt.pi.core.id.PId;
import com.bt.pi.core.parser.KoalaPiEntityFactory;

@RunWith(MockitoJUnitRunner.class)
public class KoalaReplicationPolicyTest {
    @Mock
    private StorageManager storage;
    @InjectMocks
    private KoalaReplicationPolicy koalaReplicationPolicy = new KoalaReplicationPolicy(storage);
    private KoalaIdFactory koalaIdFactory;
    private IdFactory idFactory;
    private IdSet local;
    private IdSet remote;

    @Before
    public void before() {
        KoalaPiEntityFactory koalaPiEntityFactory = new KoalaPiEntityFactory();

        koalaIdFactory = new KoalaIdFactory();
        koalaIdFactory.setKoalaPiEntityFactory(koalaPiEntityFactory);

        idFactory = new GCIdFactory(koalaIdFactory);

        local = idFactory.buildIdSet();
        remote = idFactory.buildIdSet();
    }

    @Test
    public void shouldReturnAllOfRemoteWithEmptyLocal() {
        // setup
        Id id1 = new GCId(koalaIdFactory.buildId("123"), 1);
        Id id2 = new GCId(koalaIdFactory.buildId("456"), 1);
        remote.addId(id1);
        remote.addId(id2);

        // act
        IdSet result = koalaReplicationPolicy.difference(local, remote, idFactory);

        // assert
        assertEquals(2, result.numElements());
        assertTrue(result.isMemberId(id1));
        assertTrue(result.isMemberId(id2));
    }

    @Test
    public void shouldNotReturnRemoteIfLocalIsSameVersion() {
        // setup
        Id gcId1 = new GCId(koalaIdFactory.buildId("123"), 1);
        Id id2 = koalaIdFactory.buildId("456");
        Id gcId2 = new GCId(id2, 1);
        Id gcId3 = new GCId(id2, 1);
        remote.addId(gcId1);
        remote.addId(gcId2);
        local.addId(gcId3);
        GCPastMetadata metadata = new GCPastMetadata(1);
        when(storage.getMetadata(id2)).thenReturn(metadata);

        // act
        IdSet result = koalaReplicationPolicy.difference(local, remote, idFactory);

        // assert
        assertEquals(1, result.numElements());
        assertTrue(result.isMemberId(gcId1));
    }

    @Test
    public void shouldReturnRemoteIfLocalIsOlderVersion() {
        // setup
        Id id1 = new GCId(koalaIdFactory.buildId("123"), 1);
        Id id2 = new GCId(koalaIdFactory.buildId("456"), 2);
        Id id3 = new GCId(koalaIdFactory.buildId("456"), 1);
        remote.addId(id1);
        remote.addId(id2);
        local.addId(id3);
        GCPastMetadata metadata = new GCPastMetadata(1);
        when(storage.getMetadata(id2)).thenReturn(metadata);

        // act
        IdSet result = koalaReplicationPolicy.difference(local, remote, idFactory);

        // assert
        assertEquals(2, result.numElements());
        assertTrue(result.isMemberId(id1));
        assertTrue(result.isMemberId(id2));
    }

    @Test
    public void shouldNotReturnBackupId() {
        // setup
        Id id1 = new GCId(koalaIdFactory.buildId("123"), 1);
        PId pid = koalaIdFactory.buildPId("456");
        PId pid2 = pid.asBackupId();
        Id backupId = koalaIdFactory.buildIdFromToString(pid2.getIdAsHex());
        Id id2 = new GCId(backupId, 2);
        remote.addId(id1);
        remote.addId(id2);

        // act
        IdSet result = koalaReplicationPolicy.difference(local, remote, idFactory);

        // assert
        assertEquals(1, result.numElements());
        assertTrue(result.isMemberId(id1));
    }
}

