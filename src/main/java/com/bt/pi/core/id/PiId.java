package com.bt.pi.core.id;

import java.util.Locale;

import org.apache.commons.lang.builder.EqualsBuilder;
import org.apache.commons.lang.builder.HashCodeBuilder;
import org.apache.commons.lang.builder.ToStringBuilder;

import rice.p2p.commonapi.Id;

import com.bt.pi.core.scope.NodeScope;

/*

 ID structure is:
 
 |2 hex digits for region|2 hex digits for zone|32 hex digits for id hash|2 free hex digits ('00')|1 hex digit to indicate scope *|1 hex digit for backup info **|
 
 * scope is '0' for global 
            '2' for region 
            '1' for zone 
           
 ** backup info is '3' for backupable and a backup
                   '2' for backupable but not a backup
                   '1' for not backupable and a backup
                   '0' for not backupable and not a backup 

 if the id is only region scoped, then the second group of 2 hex digits are omitted and there will be an extra '00' after the 32 digit id instead
 
 */
public class PiId extends PId {
    private static final String TWO_BYTE_HEX = "%02X";
    private static final int TWO = 2;
    private static final int SIXTEEN = 16;
    private static final int THIRTY_TWO = 32;
    private static final int FOUR = 4;
    private static final int MIN_LEN = 10;
    private String regionAvzHex;
    private String hashHex;
    private int localGlobalAvailabilityZoneCode;
    private boolean isRegionShifted;
    private NodeScope scope;
    private boolean backupable;
    private boolean backup;

    protected PiId(Id id, int currentLocalGlobalAvailabilityZoneCode) {
        this(id.toStringFull(), currentLocalGlobalAvailabilityZoneCode);
    }

    // TODO: should we add some validation to the incoming hex ?
    public PiId(String fullHexRepresentation, int currentLocalGlobalAvailabilityZoneCode) {
        this(fullHexRepresentation, currentLocalGlobalAvailabilityZoneCode, false);
    }

    private PiId(PiId copy) {
        regionAvzHex = copy.regionAvzHex;
        hashHex = copy.hashHex;
        localGlobalAvailabilityZoneCode = copy.localGlobalAvailabilityZoneCode;
        isRegionShifted = copy.isRegionShifted;
        scope = copy.scope;
        backupable = copy.backupable;
        backup = copy.backup;
    }

    private PiId(String fullHexRepresentation, int currentLocalGlobalAvailabilityZoneCode, boolean regionShifted) {
        validate(fullHexRepresentation);
        String idStr = fullHexRepresentation.toUpperCase(Locale.US);
        regionAvzHex = idStr.substring(0, PId.HEX_DIGITS_RESERVED_FOR_REGION_AND_AVZ);
        hashHex = idStr.substring(PId.HEX_DIGITS_RESERVED_FOR_REGION_AND_AVZ, idStr.length() - FOUR);
        String last4 = idStr.substring(idStr.length() - FOUR);
        int last = Integer.parseInt(last4.substring(2), HEX_RADIX);
        if ((last & THIRTY_TWO) == THIRTY_TWO)
            scope = NodeScope.REGION;
        else if ((last & SIXTEEN) == SIXTEEN)
            scope = NodeScope.AVAILABILITY_ZONE;
        else
            scope = NodeScope.GLOBAL;
        backupable = (last & TWO) == TWO;
        backup = (last & 1) == 1;
        localGlobalAvailabilityZoneCode = currentLocalGlobalAvailabilityZoneCode;
        isRegionShifted = regionShifted;
    }

    private void validate(String fullHexRepresentation) {
        if (null == fullHexRepresentation)
            throw new IllegalArgumentException("id cannot be null");
        int len = fullHexRepresentation.length();
        if (len < MIN_LEN)
            throw new IllegalArgumentException(String.format("id must be at least %d hex digits long", MIN_LEN));
        if ((len % 2) != 0)
            throw new IllegalArgumentException("id must be an even number of hex digits");
        if (!fullHexRepresentation.matches("[0-9ABCDEFabcdef]*"))
            throw new IllegalArgumentException("id must be a hex string");
    }

    public String getIdAsHex() {
        int last = 0;
        if (NodeScope.REGION.equals(scope))
            last += THIRTY_TWO;
        else if (NodeScope.AVAILABILITY_ZONE.equals(scope))
            last += SIXTEEN;
        if (backupable)
            last += TWO;
        if (backup)
            last += 1;
        if (isRegionShifted)
            return regionAvzHex.substring(0, TWO) + hashHex + "0000" + String.format(TWO_BYTE_HEX, last);
        return regionAvzHex + hashHex + "00" + String.format(TWO_BYTE_HEX, last);
    }

    public PiId forGlobalAvailablityZoneCode(int globalAvailabilityZoneCode) {
        if (globalAvailabilityZoneCode > Math.pow(HEX_RADIX, HEX_DIGITS_RESERVED_FOR_REGION_AND_AVZ)) {
            throw new IllegalArgumentException("");
        }
        PiId result = new PiId(this);
        result.regionAvzHex = String.format("%04X", globalAvailabilityZoneCode);
        result.scope = NodeScope.AVAILABILITY_ZONE;
        result.isRegionShifted = false;
        return result;
    }

    public PiId forLocalAvailabilityZone() {
        return this.forGlobalAvailablityZoneCode(localGlobalAvailabilityZoneCode);
    }

    public PiId forLocalRegion() {
        return this.forRegion(getRegionCodeFromGlobalAvailabilityZoneCode(localGlobalAvailabilityZoneCode));
    }

    public PiId forRegion(int regionCode) {
        PiId result = new PiId(this);
        result.scope = NodeScope.REGION;
        result.isRegionShifted = true;
        result.regionAvzHex = String.format("%02X%2s", regionCode, regionAvzHex.substring(2));
        return result;
    }

    public PiId forLocalScope(NodeScope aScope) {
        return this.forScope(aScope, localGlobalAvailabilityZoneCode);
    }

    public PiId forScope(NodeScope aScope, int globalAvailabilityZoneCode) {
        if (aScope.equals(NodeScope.AVAILABILITY_ZONE))
            return this.forGlobalAvailablityZoneCode(globalAvailabilityZoneCode);
        if (aScope.equals(NodeScope.REGION))
            return this.forRegion(getRegionCodeFromGlobalAvailabilityZoneCode(globalAvailabilityZoneCode));
        return this;
    }

    public PiId forDht() {
        PiId result = new PiId(this);
        result.backupable = false;
        result.scope = NodeScope.GLOBAL;
        return result;
    }

    public PiId asBackupId() {
        PiId copy = new PiId(this);
        copy.backup = true;
        return copy;
    }

    public PiId asBackupable() {
        PiId copy = new PiId(this);
        copy.backupable = true;
        return copy;
    }

    @Override
    public boolean isBackupable() {
        return backupable;
    }

    @Override
    public NodeScope getScope() {
        return scope;
    }

    @Override
    public int getAvailabilityZone() {
        return getAvailabilityZoneFromId(regionAvzHex);
    }

    @Override
    public int getRegion() {
        return getRegionFromId(regionAvzHex);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (!(obj instanceof PiId))
            return false;

        PiId other = (PiId) obj;

        return new EqualsBuilder().append(backup, other.backup).append(backupable, other.backupable).append(scope, other.scope).append(hashHex, other.hashHex).append(regionAvzHex, other.regionAvzHex).append(isRegionShifted, other.isRegionShifted)
                .append(localGlobalAvailabilityZoneCode, other.localGlobalAvailabilityZoneCode).isEquals();
    }

    @Override
    public String toStringFull() {
        return getIdAsHex();
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder().append(backup).append(backupable).append(scope).append(hashHex).append(isRegionShifted).append(localGlobalAvailabilityZoneCode).append(regionAvzHex).toHashCode();
    }

    @Override
    public String toString() {
        return new ToStringBuilder(this).append("backup", backup).append("backupable", backupable).append("scope", scope).append("isRegionShifted", isRegionShifted).append("regionAvzHex", regionAvzHex).append("hashHex", hashHex)
                .append("localGlobalAvailabilityZoneCode", localGlobalAvailabilityZoneCode).toString();
    }
}
