package com.bt.pi.core.id;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.Map.Entry;

public class SuperNodeIdFactory {
    private static final int REGION_HEX_LENGTH = 2;
    private static final int AVAILABILITY_ZONE_HEX_LENGTH = 2;
    private static final int ID_BIT_LENGTH = 40;
    private static final int SIXTEEN = 16;

    protected SuperNodeIdFactory() {
    }

    /**
     * 
     * @param region
     * @param availabilityZone
     * @param numberOfSuperNodes
     * @param offset
     * @return a sorted set of Ids
     */
    public static Set<String> getSuperNodeCheckPoints(int region, int availabilityZone, int numberOfSuperNodes, int offset) {
        return getSuperNodeCheckPoints(region, availabilityZone, numberOfSuperNodes, offset, null);
    }

    /**
     * 
     * @param region
     * @param availabilityZone
     * @param numberOfSuperNodes
     * @param offset
     * @param id
     * @return a sorted set of Ids
     */
    public static SortedSet<String> getSuperNodeCheckPoints(int region, int availabilityZone, int numberOfSuperNodes, int offset, String id) {
        String regionString = Integer.toHexString(region).toUpperCase(Locale.ENGLISH);
        String availabilityZoneString = Integer.toHexString(availabilityZone).toUpperCase(Locale.ENGLISH);

        Map<Integer, String> rangesToReplace = new HashMap<Integer, String>();
        rangesToReplace.put(REGION_HEX_LENGTH, regionString);
        rangesToReplace.put(REGION_HEX_LENGTH + AVAILABILITY_ZONE_HEX_LENGTH, availabilityZoneString);

        return getSuperNodeCheckPoints(rangesToReplace, numberOfSuperNodes, offset, id);
    }

    /**
     * 
     * @param region
     * @param numberOfSuperNodes
     * @param offset
     * @return a sorted set of Ids
     */
    public static Set<String> getSuperNodeCheckPoints(int region, int numberOfSuperNodes, int offset) {
        return getSuperNodeCheckPoints(region, numberOfSuperNodes, offset, null);
    }

    /**
     * 
     * @param region
     * @param numberOfSuperNodes
     * @param offset
     * @param id
     * @return a sorted set of Ids
     */
    public static SortedSet<String> getSuperNodeCheckPoints(int region, int numberOfSuperNodes, int offset, String id) {
        String regionString = Integer.toHexString(region).toUpperCase(Locale.ENGLISH);

        Map<Integer, String> rangesToReplace = new HashMap<Integer, String>();
        rangesToReplace.put(REGION_HEX_LENGTH, regionString);

        return getSuperNodeCheckPoints(rangesToReplace, numberOfSuperNodes, offset, id);
    }

    /**
     * 
     * @param numberOfSuperNodes
     * @param offset
     * @return a sorted set of Ids
     */
    public static SortedSet<String> getSuperNodeCheckPoints(int numberOfSuperNodes, int offset) {
        return getSuperNodeCheckPoints(null, numberOfSuperNodes, offset, null);
    }

    /**
     * 
     * @param numberOfSuperNodes
     * @param offset
     * @param id
     * @return a sorted set of Ids
     */
    public static SortedSet<String> getSuperNodeCheckPoints(int numberOfSuperNodes, int offset, String id) {
        return getSuperNodeCheckPoints(null, numberOfSuperNodes, offset, id);
    }

    private static SortedSet<String> getSuperNodeCheckPoints(Map<Integer, String> rangesToReplace, int numberOfSuperNodes, int offset, String id) {
        if ((numberOfSuperNodes & (numberOfSuperNodes - 1)) != 0) {
            throw new IllegalArgumentException(String.format("The number of checkpoint ids must be divisible by 2. %s is not.", numberOfSuperNodes));
        }

        // if zero supernodes return an empty set
        if (numberOfSuperNodes == 0)
            return new TreeSet<String>();

        StringBuilder checkPointTemplate = createIdTemplate(id);
        int replacedRangesLength = 0;
        if (rangesToReplace != null) {
            for (Entry<Integer, String> rangeToReplace : rangesToReplace.entrySet()) {
                replaceRangeWithString(checkPointTemplate, rangeToReplace.getValue(), rangeToReplace.getKey());
                replacedRangesLength = Math.max(replacedRangesLength, rangeToReplace.getKey());
            }
        }

        SortedSet<String> superNodeCheckPoints = new TreeSet<String>();
        int numberOfHexCharacters = Integer.toHexString(numberOfSuperNodes - 1).length();
        int maxHexValueForNumberOfCharacters = (int) Math.pow(SIXTEEN, numberOfHexCharacters);
        int value = offset % maxHexValueForNumberOfCharacters;
        for (int i = 0; i < numberOfSuperNodes; value = (value + maxHexValueForNumberOfCharacters / numberOfSuperNodes) % maxHexValueForNumberOfCharacters, i++) {
            String hexString = Integer.toHexString(value).toUpperCase(Locale.ENGLISH);
            StringBuilder checkPoint = new StringBuilder(checkPointTemplate);
            replaceRangeWithString(checkPoint, hexString, replacedRangesLength + numberOfHexCharacters);
            superNodeCheckPoints.add(checkPoint.toString());
        }
        return superNodeCheckPoints;
    }

    private static void replaceRangeWithString(StringBuilder original, String replaceString, int end) {
        original.replace(end - replaceString.length(), end, replaceString);
    }

    private static StringBuilder createIdTemplate(String id) {

        StringBuilder idTemplate;
        if (id == null) {
            idTemplate = new StringBuilder();

            for (int i = 0; i < ID_BIT_LENGTH; i++)
                idTemplate.append("0");
        } else
            idTemplate = new StringBuilder(id);

        return idTemplate;
    }
}
