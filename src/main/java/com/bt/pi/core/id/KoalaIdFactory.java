package com.bt.pi.core.id;

import java.util.Random;
import java.util.SortedMap;

import javax.annotation.Resource;

import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.lang.RandomStringUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.stereotype.Component;

import rice.environment.random.RandomSource;
import rice.p2p.commonapi.Id;
import rice.p2p.commonapi.Id.Distance;
import rice.p2p.commonapi.IdFactory;
import rice.p2p.commonapi.IdRange;
import rice.p2p.commonapi.IdSet;
import rice.p2p.commonapi.NodeHandleSet;
import rice.pastry.NodeIdFactory;

import com.bt.pi.core.conf.Property;
import com.bt.pi.core.entity.Locatable;
import com.bt.pi.core.parser.KoalaPiEntityFactory;

@Component
public class KoalaIdFactory implements IdFactory, NodeIdFactory, LocaleDescriptor {
    public static final int AVAILABILITY_ZONE_CODE_WITHIN_REGION_SIZE_BITS = 8;
    public static final int UNSET = -1;
    private static final String INDEX_SCHEME = "idx";
    private static final String SCHEME_DELIMITER = ":";
    private static final String UNSET_STRING = "-1";
    private static final int BITS_PER_BYTE = 4;
    private static final Log LOG = LogFactory.getLog(KoalaIdFactory.class);
    private KoalaPiEntityFactory koalaPiEntityFactory;

    private int region;
    private int availabilityZone;
    private String nodeId;

    public KoalaIdFactory() {
        this(UNSET, UNSET);
    }

    public KoalaIdFactory(int aRegion, int anAvailabilityZone) {
        LOG.debug(String.format("KoalaIdFactory(%d, %d)", aRegion, anAvailabilityZone));
        region = aRegion;
        availabilityZone = anAvailabilityZone;
        if (region != UNSET || availabilityZone != UNSET)
            LOG.info(String.format("Set region to %s and availability zone to %s in constructor", aRegion, anAvailabilityZone));
        koalaPiEntityFactory = null;
    }

    public PId buildPIdFromHexString(String hex) {
        return new PiId(hex, getGlobalAvailabilityZoneCode());
    }

    private String extractScheme(String url) {
        String scheme = "";
        // Index special case -
        // this is needed to avoid a reseed. :(
        if (url.startsWith(INDEX_SCHEME)) {
            scheme = url;
        } else {
            int schemeEnd = url.indexOf(SCHEME_DELIMITER);
            if (schemeEnd > -1) {
                scheme = url.substring(0, schemeEnd);
            }
        }
        return scheme;
    }

    public PId buildPId(Locatable entity) {
        return buildPId(entity.getUrl());
    }

    public PId buildPId(String url) {
        LOG.debug(String.format("buildPId(%s)", url));
        String hex = DigestUtils.md5Hex(url);
        String baseIdStr = hex.substring(0, PId.HEX_DIGITS_RESERVED_FOR_REGION_AND_AVZ) + hex;
        PiId result = new PiId(StringUtils.rightPad(baseIdStr, getIdToStringLength(), "0"), getGlobalAvailabilityZoneCode());
        String scheme = extractScheme(url);
        if (koalaPiEntityFactory.isBackupable(scheme))
            return result.asBackupable();
        return result;
    }

    public PId convertToPId(Id id) {
        return new PiId(id, this.getGlobalAvailabilityZoneCode());
    }

    @Override
    public Id buildId(byte[] material) {
        return rice.pastry.Id.build(material);
    }

    public Id buildId(PId pid) {
        return buildId(pid.getIdAsHex());
    }

    @Override
    public Id buildId(int[] material) {
        return rice.pastry.Id.build(material);
    }

    @Override
    public Id buildId(String string) {
        return rice.pastry.Id.build(string);
    }

    @Override
    public Distance buildIdDistance(byte[] material) {
        return new rice.pastry.Id.Distance(material);
    }

    @Override
    public Id buildIdFromToString(char[] chars, int offset, int length) {
        return rice.pastry.Id.build(chars, offset, length);
    }

    @Override
    public IdRange buildIdRange(Id cw, Id ccw) {
        if (cw instanceof rice.pastry.Id && ccw instanceof rice.pastry.Id) {
            return new rice.pastry.IdRange((rice.pastry.Id) cw, (rice.pastry.Id) ccw);
        } else
            return null;
    }

    @Override
    public IdRange buildIdRangeFromPrefix(String string) {
        rice.pastry.Id start = rice.pastry.Id.build(string);

        rice.pastry.Id end = rice.pastry.Id.build(string + "ffffffffffffffffffffffffffffffffffffffff");

        end = end.getCW();

        return new rice.pastry.IdRange(start, end);
    }

    @Override
    public IdSet buildIdSet() {
        return new rice.pastry.IdSet();
    }

    @SuppressWarnings("rawtypes")
    @Override
    public IdSet buildIdSet(SortedMap map) {
        return new rice.pastry.IdSet(map);
    }

    @Override
    public NodeHandleSet buildNodeHandleSet() {
        return new rice.pastry.NodeSet();
    }

    @Override
    public Id buildRandomId(Random rng) {
        return rice.pastry.Id.makeRandomId(rng);
    }

    @Override
    public Id buildRandomId(RandomSource rng) {
        return rice.pastry.Id.makeRandomId(rng);
    }

    @Override
    public int getIdToStringLength() {
        return rice.pastry.Id.IdBitLength / BITS_PER_BYTE;
    }

    @Override
    public rice.pastry.Id generateNodeId() {
        LOG.debug(String.format("generateNodeId() - Existing node id is %s and the global avz code is %d", nodeId, getGlobalAvailabilityZoneCode()));
        PId generatedNodeId = buildPId(RandomStringUtils.random(getIdToStringLength())).forGlobalAvailablityZoneCode(0);
        if (nodeId != null) {
            generatedNodeId = new PiId(nodeId, this.getGlobalAvailabilityZoneCode());
        } else if (region >= 0 && availabilityZone >= 0) {
            generatedNodeId = generatedNodeId.forGlobalAvailablityZoneCode(getGlobalAvailabilityZoneCode());
        }
        rice.pastry.Id idToReturn = rice.pastry.Id.build(generatedNodeId.forDht().getIdAsHex());
        LOG.debug(String.format("Returning id: %s", idToReturn.toStringFull()));
        return idToReturn;
    }

    @Override
    public Id buildIdFromToString(String string) {
        return rice.pastry.Id.build(string);
    }

    public void setRegionFromNodeId(String aNodeId) {
        int reg = PId.getRegionFromId(aNodeId);
        if (reg != -1) {
            setRegion(reg);
            LOG.info(String.format("Set region to %s from node id %s", region, nodeId));
        } else {
            LOG.warn(String.format("Tried to set region to -1 from node id %s", aNodeId));
        }
    }

    public void setAvailabilityZoneFromNodeId(String aNodeId) {
        int zone = PId.getAvailabilityZoneFromId(aNodeId);
        if (zone != -1) {
            setAvailabilityZone(zone);
            LOG.info(String.format("Set availability zone to %s from node id %s", zone, nodeId));
        } else {
            LOG.warn(String.format("Tried to set availability zone to -1 from node id %s", aNodeId));
        }
    }

    public int getRegion() {
        return region;
    }

    @Property(key = "node.region", defaultValue = UNSET_STRING)
    public void setRegion(int aRegion) {
        if (aRegion != UNSET) {
            this.region = aRegion;
            LOG.info(String.format("Set region to %s", aRegion));
        } else
            LOG.warn(String.format("Attempt to set region to %d, not setting...", UNSET));
    }

    public int getAvailabilityZoneWithinRegion() {
        return availabilityZone;
    }

    public int getGlobalAvailabilityZoneCode() {
        return PId.getGlobalAvailabilityZoneCodeFromRegionAndLocalAvailabilityZone(region, availabilityZone);
    }

    @Property(key = "node.availability.zone", defaultValue = UNSET_STRING)
    public void setAvailabilityZone(int anAvailabilityZone) {
        if (anAvailabilityZone != UNSET) {
            this.availabilityZone = anAvailabilityZone;
            LOG.info(String.format("Set availability zone to %s", anAvailabilityZone));
        } else
            LOG.warn(String.format("Attempt to set availability zone to %d, not setting...", UNSET));
    }

    public void setNodeId(String existingId) {
        nodeId = existingId;
        setRegionFromNodeId(existingId);
        setAvailabilityZoneFromNodeId(existingId);
        LOG.debug(String.format("Set node id to %s", nodeId));
    }

    @Resource
    public void setKoalaPiEntityFactory(KoalaPiEntityFactory factory) {
        koalaPiEntityFactory = factory;
    }
}
