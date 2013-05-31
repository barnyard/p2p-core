package com.bt.pi.core.id;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.anyObject;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.Random;
import java.util.SortedMap;

import org.junit.Before;
import org.junit.Test;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

import rice.environment.Environment;
import rice.environment.random.RandomSource;
import rice.p2p.commonapi.Id;
import rice.p2p.commonapi.IdSet;
import rice.p2p.commonapi.NodeHandleSet;
import rice.pastry.commonapi.PastryIdFactory;

import com.bt.pi.core.entity.Locatable;
import com.bt.pi.core.entity.PiLocation;
import com.bt.pi.core.parser.KoalaPiEntityFactory;

public class KoalaIdFactoryTest {
    private static final String EXPECTED_TEST_KEY_ID_255_255 = "FFFF0DBF2FA10163AF49ABFF79D682C5D4070000";
    private static final String EXPECTED_TEST_KEY_ID_255_0 = "FF0DBF2FA10163AF49ABFF79D682C5D407000000";
    private static final String TEST_KEY = "somereallylongunhelpfulkey";
    private static final int TWO_HUNDRED_FIFTY_FIVE = 255;

    private byte[] byteMaterial;
    private PastryIdFactory pastryIdFactory;
    private KoalaIdFactory idFactory;
    private PiLocation scopedEntity;

    @Before
    public void before() {
        byteMaterial = new byte[] { 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10 };
        pastryIdFactory = new PastryIdFactory(new Environment());
        idFactory = new KoalaIdFactory();
        idFactory.setKoalaPiEntityFactory(new KoalaPiEntityFactory());
        scopedEntity = mock(PiLocation.class);
        when(scopedEntity.getUrl()).thenReturn("scopedEntity");
    }

    // No idea what this test is for!?
    @Test
    public void backwardsCompatibilityTest() {
        PId result = idFactory.buildPId("josh").forRegion(0x02);
        assertEquals("02F94ADCC3DDDA04A8F34928D862F404B4000020", result.getIdAsHex());
    }

    @Test
    public void shouldSetBackupableUsingScheme() {
        // setup
        KoalaPiEntityFactory koalaPiEntityFactory = mock(KoalaPiEntityFactory.class);
        idFactory.setKoalaPiEntityFactory(koalaPiEntityFactory);
        String scheme = "abc";
        when(koalaPiEntityFactory.isBackupable(scheme)).thenReturn(true);

        // act
        PId result = idFactory.buildPId(scheme + ":data");

        // assert
        assertTrue(result.getIdAsHex().endsWith("0002"));
    }

    @Test
    public void shouldSetBackupableUsingSchemeForLocateable() {
        // setup
        KoalaPiEntityFactory koalaPiEntityFactory = mock(KoalaPiEntityFactory.class);
        idFactory.setKoalaPiEntityFactory(koalaPiEntityFactory);
        String scheme = "abc";
        when(koalaPiEntityFactory.isBackupable(scheme)).thenReturn(true);
        Locatable locatable = mock(Locatable.class);
        when(locatable.getUrl()).thenReturn(scheme + ":data");

        // act
        PId result = idFactory.buildPId(locatable);

        // assert
        assertTrue(result.getIdAsHex().endsWith("0002"));
    }

    @Test
    public void shouldBuildIDFromRawId() {
        // act
        Id id = rice.pastry.Id.build("hello");

        // assert
        assertEquals(id, idFactory.buildIdFromToString(id.toStringFull()));
    }

    @Test
    public void shouldReturnSomething() {
        // act
        PId id = idFactory.buildPId(TEST_KEY);

        // assert
        assertEquals("0DBF0DBF2FA10163AF49ABFF79D682C5D4070000", id.getIdAsHex());
    }

    @Test
    public void shouldBuildIdFromByteMaterial() {
        // act & assert
        assertEquals(pastryIdFactory.buildId(byteMaterial), idFactory.buildId(byteMaterial));
    }

    @Test
    public void shouldBuildIdFromIntMaterial() {
        // setup
        int[] material = new int[] { 1, 2, 3, 4, 5, 6, 7, 8, 9, 0 };

        // act&assert
        assertEquals(pastryIdFactory.buildId(material), idFactory.buildId(material));
    }

    @Test
    public void shouldBuildIdByDistance() {
        // act&assert
        assertEquals(pastryIdFactory.buildIdDistance(byteMaterial), idFactory.buildIdDistance(byteMaterial));
    }

    @Test
    public void shouldBuildIdFromToString() {
        // act & assert
        assertEquals(pastryIdFactory.buildIdFromToString(EXPECTED_TEST_KEY_ID_255_255), idFactory.buildIdFromToString(EXPECTED_TEST_KEY_ID_255_255));
    }

    @Test
    public void shouldBuildIdFromToStringWithCharArray() {
        // act and assert
        assertEquals(pastryIdFactory.buildIdFromToString(EXPECTED_TEST_KEY_ID_255_255.toCharArray(), 0, 40), idFactory.buildIdFromToString(EXPECTED_TEST_KEY_ID_255_255.toCharArray(), 0, 40));
    }

    @Test
    public void shouldBuildIdRange() {
        // setup
        String idStr1 = "cool";
        String idStr2 = "cooler";

        // act&assert
        assertEquals(pastryIdFactory.buildIdRange(rice.pastry.Id.build(idStr1), rice.pastry.Id.build(idStr2)), idFactory.buildIdRange(rice.pastry.Id.build(idStr1), rice.pastry.Id.build(idStr2)));
        assertEquals(pastryIdFactory.buildIdRange(rice.pastry.Id.build(idStr1), rice.pastry.Id.build(idStr2)), idFactory.buildIdRange(rice.pastry.Id.build(idStr1), rice.pastry.Id.build(idStr2)));
    }

    @Test
    public void shouldBuildIdRangeFromPrefix() {
        // act&assert
        assertEquals(pastryIdFactory.buildIdRangeFromPrefix(EXPECTED_TEST_KEY_ID_255_0), idFactory.buildIdRangeFromPrefix(EXPECTED_TEST_KEY_ID_255_0));
    }

    @Test
    public void shouldBuildIdSet() {
        // act & assert
        assertTrue(idFactory.buildIdSet() instanceof IdSet);
    }

    @Test
    public void shouldBuildIdSetFromMap() {
        // act & assert
        assertTrue(idFactory.buildIdSet(mock(SortedMap.class)) instanceof IdSet);
    }

    @Test
    public void shouldBuildIdNodeHandleSet() {
        // act & assert
        assertTrue(idFactory.buildNodeHandleSet() instanceof NodeHandleSet);
    }

    @Test
    public void shouldBuildRandomId() {
        // setup
        Random rnd = mock(Random.class);
        doAnswer(new Answer<Object>() {

            @Override
            public Object answer(InvocationOnMock invocation) throws Throwable {
                byte[] bytes = (byte[]) invocation.getArguments()[0];
                for (int i = 0; i < bytes.length; i++) {
                    bytes[i] = byteMaterial[i];
                }
                return null;
            }

        }).when(rnd).nextBytes((byte[]) anyObject());

        // act & assert
        assertEquals(pastryIdFactory.buildRandomId(rnd), idFactory.buildRandomId(rnd));
    }

    @Test
    public void shouldBuildRandomIdFromRandomSource() {
        // setup
        RandomSource rnd = mock(RandomSource.class);
        doAnswer(new Answer<Object>() {

            @Override
            public Object answer(InvocationOnMock invocation) throws Throwable {
                byte[] bytes = (byte[]) invocation.getArguments()[0];
                for (int i = 0; i < bytes.length; i++) {
                    bytes[i] = byteMaterial[i];
                }
                return null;
            }

        }).when(rnd).nextBytes((byte[]) anyObject());

        // act & assert
        assertEquals(pastryIdFactory.buildRandomId(rnd), idFactory.buildRandomId(rnd));
    }

    @Test
    public void shouldReturnAvailabilityZone() {
        // setup
        idFactory.setAvailabilityZone(TWO_HUNDRED_FIFTY_FIVE);

        // act & assert
        assertEquals(TWO_HUNDRED_FIFTY_FIVE, idFactory.getAvailabilityZoneWithinRegion());
    }

    @Test
    public void shouldReturnRegion() {
        // setup
        idFactory.setRegion(TWO_HUNDRED_FIFTY_FIVE);

        // act & assert
        assertEquals(TWO_HUNDRED_FIFTY_FIVE, idFactory.getRegion());
    }

    @Test
    public void shouldGetGlobalAvzCodeForCurrentRegionAndAvz() {
        idFactory.setRegion(0x12);
        idFactory.setAvailabilityZone(0x34);

        // assert
        assertEquals(0x1234, idFactory.getGlobalAvailabilityZoneCode());
    }

    @Test
    public void shouldReturnIdToStringLegth() {
        // act & assert
        assertEquals(pastryIdFactory.getIdToStringLength(), idFactory.getIdToStringLength());
    }

    @Test
    public void shouldReturnNodeIdBasedOnRegionAndAvailabilityZone() throws Exception {
        // setup
        idFactory.setRegion(TWO_HUNDRED_FIFTY_FIVE);
        idFactory.setAvailabilityZone(TWO_HUNDRED_FIFTY_FIVE);

        // act
        rice.pastry.Id result = idFactory.generateNodeId();

        // assert
        System.out.println(result.toStringFull());
        assertTrue(result.toStringFull().startsWith("FFFF"));
        assertTrue(result.toStringFull().endsWith("0000"));
        assertEquals(40, result.toStringFull().length());

    }

    @Test
    public void shouldReturnNodeIdIfNoRegionAndAvailabilityZone() throws Exception {
        // act
        rice.pastry.Id nodeId = idFactory.generateNodeId();

        // assert
        assertTrue(nodeId.toStringFull().startsWith("0000"));
        assertTrue(nodeId.toStringFull().endsWith("0000"));
        assertEquals(40, nodeId.toStringFull().length());
    }

    @Test
    public void shouldReturnNodeIdIfSetAsExisting() throws Exception {
        // setup
        idFactory.setNodeId(EXPECTED_TEST_KEY_ID_255_0);

        // act
        rice.pastry.Id nodeId = idFactory.generateNodeId();

        // assert
        assertEquals(EXPECTED_TEST_KEY_ID_255_0, nodeId.toStringFull());
    }

    @Test
    public void setNodeIdShouldAlsoSetRegionAndZone() {
        // setup
        String[] existingIds = { EXPECTED_TEST_KEY_ID_255_255, "2134abcd", "22", "" };
        int[] expectedRegions = { 255, 33, 34, -1 };
        int[] expectedAvailabilityZones = { 255, 52, -1, -1 };

        // act/assert
        int i = 0;
        for (String existingId : existingIds) {
            idFactory = new KoalaIdFactory();
            idFactory.setNodeId(existingId);
            assertEquals(expectedAvailabilityZones[i], idFactory.getAvailabilityZoneWithinRegion());
            assertEquals(expectedRegions[i], idFactory.getRegion());
            i++;
        }
    }
}
