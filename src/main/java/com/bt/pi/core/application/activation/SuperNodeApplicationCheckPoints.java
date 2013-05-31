package com.bt.pi.core.application.activation;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import com.bt.pi.core.entity.PiEntityBase;
import com.bt.pi.core.id.SuperNodeIdFactory;

public class SuperNodeApplicationCheckPoints extends PiEntityBase {
    public static final String SCHEME = "snapp";
    public static final String URL = SCHEME + ":checkpoints";
    private static final int ONE_BYTE = 256;
    private static final int REGION_HEX_LENGTH = 2;
    private static final int AVAILABILITY_ZONE_HEX_LENGTH = 2;

    private Map<String, Integer> numberOfSuperNodesPerApplication;
    private Map<String, Integer> offsetPerApplication;

    public SuperNodeApplicationCheckPoints() {
        numberOfSuperNodesPerApplication = new HashMap<String, Integer>();
        offsetPerApplication = new HashMap<String, Integer>();
    }

    @Override
    public String getType() {
        return SuperNodeApplicationCheckPoints.class.getSimpleName();
    }

    @Override
    public String getUrl() {
        return URL;
    }

    public Map<String, Integer> getNumberOfSuperNodesPerApplication() {
        return numberOfSuperNodesPerApplication;
    }

    public Map<String, Integer> getOffsetPerApplication() {
        return offsetPerApplication;
    }

    public void setSuperNodeCheckPointsForApplication(String applicationName, int numberOfSuperNodes, int offset) {
        if (Integer.highestOneBit(numberOfSuperNodes) != numberOfSuperNodes)
            throw new IllegalArgumentException(String.format("Number of super nodes should be a power of 2: %d", numberOfSuperNodes));
        if (numberOfSuperNodes > ONE_BYTE)
            throw new IllegalArgumentException(String.format("Number of super nodes should not be more than 256: %d", numberOfSuperNodes));

        numberOfSuperNodesPerApplication.put(applicationName, numberOfSuperNodes);
        offsetPerApplication.put(applicationName, offset);
    }

    public Set<String> getSuperNodeCheckPoints(String applicationName, int region, int availabilityZone) {
        String regionString = Integer.toHexString(region).toUpperCase(Locale.ENGLISH);
        String availabilityZoneString = Integer.toHexString(availabilityZone).toUpperCase(Locale.ENGLISH);

        Map<Integer, String> rangesToReplace = new HashMap<Integer, String>();
        rangesToReplace.put(REGION_HEX_LENGTH, regionString);
        rangesToReplace.put(REGION_HEX_LENGTH + AVAILABILITY_ZONE_HEX_LENGTH, availabilityZoneString);

        if (numberOfSuperNodesPerApplication.containsKey(applicationName)) {
            int numberOfSuperNodes = numberOfSuperNodesPerApplication.get(applicationName);
            int offset = offsetPerApplication.get(applicationName);

            return SuperNodeIdFactory.getSuperNodeCheckPoints(region, availabilityZone, numberOfSuperNodes, offset);
        }

        return new TreeSet<String>();
    }

    public String getRandomSuperNodeCheckPoint(String applicationName, int region, int availabilityZone) {
        Set<String> superNodeCheckPoints = getSuperNodeCheckPoints(applicationName, region, availabilityZone);
        int random = (int) (Math.random() * superNodeCheckPoints.size());
        Iterator<String> iterator = superNodeCheckPoints.iterator();
        for (int count = 0; iterator.hasNext(); count++) {
            if (count == random)
                return iterator.next();
            iterator.next();
        }
        return null;
    }

    @Override
    public String getUriScheme() {
        return SCHEME;
    }

    @Override
    public String toString() {
        return "SuperNodeApplicationCheckPoints [numberOfSuperNodesPerApplication=" + numberOfSuperNodesPerApplication + ", offsetPerApplication=" + offsetPerApplication + "]";
    }
}
