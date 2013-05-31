package com.bt.pi.core.id;

import com.bt.pi.core.scope.NodeScope;

public abstract class PId {
    // I would think about setting this stuff as a parameter but changing it would
    // require a reseed, so not much point.
    public static final int AVAILABILITY_ZONE_CODE_WITHIN_REGION_SIZE_BITS = 8;
    public static final int HEX_DIGITS_RESERVED_FOR_REGION_AND_AVZ = 4;
    public static final int HEX_DIGITS_RESERVED_FOR_REGION = 2;
    public static final int NUMBER_OF_HEX_DIGITS_FREE_FOR_APPLICATIONS = 4;
    public static final int HEX_RADIX = 16;
    protected static final int THREE = 3;
    private static final int TWO = 2;

    public abstract int getRegion();

    public abstract int getAvailabilityZone();

    public abstract String getIdAsHex();

    public abstract PId forGlobalAvailablityZoneCode(int globalAvailabilityZoneCode);

    // we should probably make this more concise.
    public abstract PId forScope(NodeScope scope, int globalAvailabilityZoneCode);

    public abstract PId forLocalScope(NodeScope scope);

    public abstract PId forRegion(int region);

    public abstract PId forLocalAvailabilityZone();

    public abstract PId forLocalRegion();

    // remove region, scope and backupable flags from last bytes, but retain the backup flag
    public abstract PId forDht();

    public abstract PId asBackupId();

    public abstract PId asBackupable();

    public abstract String toStringFull();

    public abstract boolean isBackupable();

    public abstract NodeScope getScope();

    public static int getGlobalAvailabilityZoneCodeFromRegionAndLocalAvailabilityZone(int reg, int avz) {
        return reg * (int) Math.pow(2, AVAILABILITY_ZONE_CODE_WITHIN_REGION_SIZE_BITS) + avz;
    }

    public static int getRegionCodeFromGlobalAvailabilityZoneCode(int globalAvailabilityZoneCode) {
        return globalAvailabilityZoneCode >> AVAILABILITY_ZONE_CODE_WITHIN_REGION_SIZE_BITS;
    }

    public static int getAvailabilityZoneCodeWithinRegionFromGlobalAvailabilityZoneCode(int globalAvailabilityZoneCode) {
        return globalAvailabilityZoneCode % (int) Math.pow(2, AVAILABILITY_ZONE_CODE_WITHIN_REGION_SIZE_BITS);
    }

    public static int getGlobalAvailabilityZoneCodeFromId(String id) {
        return getGlobalAvailabilityZoneCodeFromRegionAndLocalAvailabilityZone(getRegionFromId(id), getAvailabilityZoneFromId(id));
    }

    public static int getRegionFromId(String id) {
        if (id != null && id.length() > 1) {
            String hexRegion = id.substring(0, TWO);
            return Integer.parseInt(hexRegion, HEX_RADIX);
        }
        return -1;
    }

    public static int getAvailabilityZoneFromId(String id) {
        if (id != null && id.length() > THREE) {
            String hexAvailabilityZone = id.substring(TWO, HEX_DIGITS_RESERVED_FOR_REGION_AND_AVZ);
            return Integer.parseInt(hexAvailabilityZone, HEX_RADIX);
        }
        return -1;
    }
}
