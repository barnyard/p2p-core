package com.bt.pi.core.id;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Before;
import org.junit.Test;

import com.bt.pi.core.scope.NodeScope;

public class PiIdTest {
    private PiId id;
    private static final int NUMBER_OF_HEX_DIGITS_FREE_FOR_APPS = 4;

    @Before
    public void before() {
        id = new PiId("1111567890123456789012345678901234560000", 0x0203);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testInvalidHexConstructorNull() {
        new PiId((String) null, 0);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testInvalidHexConstructorNotHex() {
        try {
            new PiId("fredbloggs", 0);
        } catch (IllegalArgumentException e) {
            assertFalse(e instanceof NumberFormatException);
            throw e;
        }
    }

    @Test(expected = IllegalArgumentException.class)
    public void testInvalidHexConstructorOddNumber() {
        new PiId("123456789123456789123456789", 0);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testInvalidHexConstructorTooShort() {
        new PiId("123456", 0);
    }

    @Test
    public void testAsBackupId() {
        assertTrue(id.asBackupId().getIdAsHex().endsWith("1"));
    }

    @Test
    public void testIsBackupable() {
        assertFalse(id.isBackupable());

        id = new PiId("1111567890123456789012345678901234560002", 0x0203);
        assertTrue(id.isBackupable());

        id = new PiId("1111567890123456789012345678901234560003", 0x0203);
        assertTrue(id.isBackupable());

        id = new PiId("1111567890123456789012345678901234560004", 0x0203);
        assertFalse(id.isBackupable());

        id = new PiId("1111567890123456789012345678901234560006", 0x0203);
        assertTrue(id.isBackupable());
    }

    @Test
    public void testAsBackable() {
        PiId result = id.asBackupable();
        assertTrue(result.getIdAsHex().endsWith("0002"));
        assertTrue(result.isBackupable());
        assertEquals(result.getIdAsHex().length(), id.getIdAsHex().length());
    }

    @Test
    public void testAsBackableAndBackup() {
        PiId result = id.asBackupable().asBackupId();
        assertTrue(result.isBackupable());
        assertEquals(result.getIdAsHex().length(), id.getIdAsHex().length());
        assertTrue(result.getIdAsHex().endsWith("0003"));
    }

    @Test
    public void testAsBackableAndBackupForRegion() {
        PiId result = id.asBackupable().asBackupId().forRegion(0x23);
        assertTrue(result.isBackupable());
        assertEquals(result.getIdAsHex().length(), id.getIdAsHex().length());
        assertTrue(result.getIdAsHex().endsWith("000023"));
    }

    @Test
    public void testConvertIdToBackupIdDoesntBreakWhenAlreadyABackup() {
        // setup
        PiId backupId = new PiId("1234567890123456789012345678901234567891", 0x1234);

        // act
        PId result = backupId.asBackupId();

        // assert
        assertEquals(backupId, result);
    }

    @Test
    public void testForRegion() {
        // act & assert
        PId result = id.forRegion(0x23);

        assertTrue(result.getIdAsHex().startsWith("23"));
        assertTrue(result.getIdAsHex().endsWith("000020"));

        assertEquals(id.forRegion(0x19).getIdAsHex().substring(2), result.getIdAsHex().substring(2));
        assertEquals(id.getIdAsHex().length(), result.getIdAsHex().length());

        assertEquals(NodeScope.REGION, result.getScope());
    }

    @Test
    public void testForAvailabilityZone() {
        // act
        PId result = id.forGlobalAvailablityZoneCode(0x8888);

        // assert
        int coreStart = NUMBER_OF_HEX_DIGITS_FREE_FOR_APPS;
        int coreEnd = result.getIdAsHex().length() - 4;
        assertEquals(id.getIdAsHex().substring(coreStart, coreEnd), result.getIdAsHex().substring(coreStart, coreEnd));
        assertTrue(result.getIdAsHex().startsWith("8888"));
        assertTrue(result.getIdAsHex().endsWith("0010"));

        assertEquals(NodeScope.AVAILABILITY_ZONE, result.getScope());
    }

    @Test
    public void testForLocal() {
        // act
        PId result = id.forLocalAvailabilityZone();

        // assert
        int coreStart = NUMBER_OF_HEX_DIGITS_FREE_FOR_APPS;
        int coreEnd = result.getIdAsHex().length() - 4;
        assertEquals(id.getIdAsHex().substring(coreStart, coreEnd), result.getIdAsHex().substring(coreStart, coreEnd));
        assertTrue(result.getIdAsHex().startsWith("0203"));
        assertTrue(result.getIdAsHex().endsWith("0010"));

        assertEquals(NodeScope.AVAILABILITY_ZONE, result.getScope());
    }

    @Test
    public void testForLocalRegion() {
        // setup
        int endOfHashAfterforRegion = PiId.NUMBER_OF_HEX_DIGITS_FREE_FOR_APPLICATIONS + 2;

        // act
        PId result = id.forLocalRegion();

        // assert
        assertTrue(result.getIdAsHex().startsWith("02"));
        assertTrue(result.getIdAsHex().endsWith("0020"));

        assertEquals(id.forRegion(0x19).getIdAsHex().substring(2), result.getIdAsHex().substring(2));

        assertEquals(result.getIdAsHex().substring(2, result.getIdAsHex().length() - endOfHashAfterforRegion), id.getIdAsHex().substring(4, result.getIdAsHex().length() - PiId.NUMBER_OF_HEX_DIGITS_FREE_FOR_APPLICATIONS));

        assertEquals(NodeScope.REGION, result.getScope());
    }

    @Test
    public void testForLocalScope() {
        // act
        PId global = id.forLocalScope(NodeScope.GLOBAL);
        PId regional = id.forLocalScope(NodeScope.REGION);
        PId local = id.forLocalScope(NodeScope.AVAILABILITY_ZONE);

        // assert
        assertEquals(id.forLocalAvailabilityZone(), local);
        assertEquals(id.forLocalRegion(), regional);
        assertEquals(id, global);
        assertTrue(global.getIdAsHex().endsWith("0000"));
        assertTrue(regional.getIdAsHex().endsWith("0020"));
        assertTrue(local.getIdAsHex().endsWith("0010"));

        assertEquals(NodeScope.REGION, regional.getScope());
        assertEquals(NodeScope.GLOBAL, global.getScope());
        assertEquals(NodeScope.AVAILABILITY_ZONE, local.getScope());
    }

    @Test
    public void testForScope() {
        // setup
        int avzCode = 0x0203;

        // act
        PId global = id.forScope(NodeScope.GLOBAL, avzCode);
        PId regional = id.forScope(NodeScope.REGION, avzCode);
        PId local = id.forScope(NodeScope.AVAILABILITY_ZONE, avzCode);

        // assert
        assertEquals(id.forLocalAvailabilityZone(), local);
        assertEquals(id.forLocalRegion(), regional);
        assertEquals(id, global);
        assertTrue(global.getIdAsHex().endsWith("0000"));
        assertTrue(regional.getIdAsHex().endsWith("0020"));
        assertTrue(local.getIdAsHex().endsWith("0010"));

        assertEquals(NodeScope.REGION, regional.getScope());
        assertEquals(NodeScope.GLOBAL, global.getScope());
        assertEquals(NodeScope.AVAILABILITY_ZONE, local.getScope());
    }

    @Test
    public void testForDhtBackup() {
        // act
        PId result = id.asBackupId().asBackupable().forLocalRegion().forDht();

        // assert
        assertTrue(result.getIdAsHex().endsWith("0001"));
    }

    @Test
    public void testForDhtNotBackup() {
        // act
        PId result = id.asBackupable().forLocalRegion().forDht();

        // assert
        assertTrue(result.getIdAsHex().endsWith("0000"));
    }

    @Test
    public void testConversions() {
        PId twistedId = id.forLocalAvailabilityZone();

        PId result = twistedId.forRegion(4).forLocalAvailabilityZone().forScope(NodeScope.AVAILABILITY_ZONE, 0x0203).forScope(NodeScope.REGION, 0x4444).forLocalRegion().forLocalAvailabilityZone();

        System.err.println(id);
        System.err.println(twistedId.forRegion(4).forLocalAvailabilityZone().forScope(NodeScope.AVAILABILITY_ZONE, 0x0203).forScope(NodeScope.REGION, 0x4444));
        System.err.println(result);
        assertEquals(twistedId, result);
    }
}
