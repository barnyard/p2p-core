package com.bt.pi.core.application.activation;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

import java.util.Iterator;
import java.util.Set;
import java.util.TreeSet;

import org.junit.Before;
import org.junit.Test;

public class SuperNodeApplicationCheckPointsTest {
    private StringBuilder trailingZeroesOnIdWithAvailabilityZoneScope;

    private SuperNodeApplicationCheckPoints superNodeApplicationCheckPoints;

    @Before
    public void setup() {
        trailingZeroesOnIdWithAvailabilityZoneScope = new StringBuilder();
        for (int i = 0; i < 34; i++)
            trailingZeroesOnIdWithAvailabilityZoneScope.append("0");

        superNodeApplicationCheckPoints = new SuperNodeApplicationCheckPoints();
    }

    @Test
    public void testUrl() throws Exception {
        // act
        String result = superNodeApplicationCheckPoints.getUrl();

        // assert
        assertThat(result, equalTo(SuperNodeApplicationCheckPoints.URL));
    }

    @Test
    public void testType() throws Exception {
        // act
        String result = superNodeApplicationCheckPoints.getType();

        // assert
        assertThat(result, equalTo(SuperNodeApplicationCheckPoints.class.getSimpleName()));
    }

    @Test(expected = IllegalArgumentException.class)
    public void numberOfSuperNodesShouldBePowerOfTwo() throws Exception {
        // act
        superNodeApplicationCheckPoints.setSuperNodeCheckPointsForApplication("test", 6, 5);
    }

    @Test(expected = IllegalArgumentException.class)
    public void numberOfSuperNodesShouldFitInOneByte() throws Exception {
        // act
        superNodeApplicationCheckPoints.setSuperNodeCheckPointsForApplication("test", 512, 5);
    }

    @Test
    public void shouldReturnEmptyMapIfApplicationNotPresentInMap() throws Exception {
        // act
        Set<String> superNodeCheckPoints = superNodeApplicationCheckPoints.getSuperNodeCheckPoints("test", 3, 46);

        // assert
        assertThat(superNodeCheckPoints.isEmpty(), is(true));
    }

    @Test
    public void testSuperNodeCheckPointsWithNonZeroOffset() throws Exception {
        // setup
        superNodeApplicationCheckPoints.setSuperNodeCheckPointsForApplication("test", 32, 5);
        Set<Integer> expectedCheckPoints = new TreeSet<Integer>();
        for (int i = 0; i < 32; i++)
            expectedCheckPoints.add(i * 8 + 5);

        // act
        Set<String> superNodeCheckPoints = superNodeApplicationCheckPoints.getSuperNodeCheckPoints("test", 3, 46);

        // assert
        assertThat(superNodeCheckPoints.size(), equalTo(32));
        Iterator<String> iterator = superNodeCheckPoints.iterator();
        while (iterator.hasNext()) {
            String superNodeCheckPoint = iterator.next();
            assertThat(superNodeCheckPoint.length(), equalTo(40));
            assertThat(superNodeCheckPoint, superNodeCheckPoint.startsWith("032E"), is(true));
            assertThat(superNodeCheckPoint, expectedCheckPoints.contains(Integer.parseInt(superNodeCheckPoint.substring(4, 6), 16)), is(true));
            assertThat(superNodeCheckPoint.substring(6), equalTo(trailingZeroesOnIdWithAvailabilityZoneScope.toString()));
        }
    }

    @Test
    public void testSuperNodeCheckPointsWithZeroOffset() throws Exception {
        // setup
        superNodeApplicationCheckPoints.setSuperNodeCheckPointsForApplication("test", 32, 0);
        Set<Integer> expectedCheckPoints = new TreeSet<Integer>();
        for (int i = 0; i < 32; i++)
            expectedCheckPoints.add(i * 8);

        // act
        Set<String> superNodeCheckPoints = superNodeApplicationCheckPoints.getSuperNodeCheckPoints("test", 3, 46);

        // assert
        assertThat(superNodeCheckPoints.size(), equalTo(32));
        Iterator<String> iterator = superNodeCheckPoints.iterator();
        while (iterator.hasNext()) {
            String superNodeCheckPoint = iterator.next();
            assertThat(superNodeCheckPoint.length(), equalTo(40));
            assertThat(superNodeCheckPoint, superNodeCheckPoint.startsWith("032E"), is(true));
            assertThat(superNodeCheckPoint, expectedCheckPoints.contains(Integer.parseInt(superNodeCheckPoint.substring(4, 6), 16)), is(true));
            assertThat(superNodeCheckPoint.substring(6), equalTo(trailingZeroesOnIdWithAvailabilityZoneScope.toString()));
        }
    }

    @Test
    public void testSuperNodeCheckPointsWithVeryHighOffsetWrapsAroundCorrectly() throws Exception {
        // setup
        superNodeApplicationCheckPoints.setSuperNodeCheckPointsForApplication("test", 32, 69);
        Set<Integer> expectedCheckPoints = new TreeSet<Integer>();
        for (int i = 0; i < 32; i++)
            expectedCheckPoints.add(i * 8 + 5);

        // act
        Set<String> superNodeCheckPoints = superNodeApplicationCheckPoints.getSuperNodeCheckPoints("test", 3, 46);

        // assert
        assertThat(superNodeCheckPoints.size(), equalTo(32));
        Iterator<String> iterator = superNodeCheckPoints.iterator();
        while (iterator.hasNext()) {
            String superNodeCheckPoint = iterator.next();
            assertThat(superNodeCheckPoint.length(), equalTo(40));
            assertThat(superNodeCheckPoint, superNodeCheckPoint.startsWith("032E"), is(true));
            assertThat(superNodeCheckPoint, expectedCheckPoints.contains(Integer.parseInt(superNodeCheckPoint.substring(4, 6), 16)), is(true));
            assertThat(superNodeCheckPoint.substring(6), equalTo(trailingZeroesOnIdWithAvailabilityZoneScope.toString()));
        }
    }

    @Test
    public void testSuperNodeCheckPointsWithNumberOfSuperNodesFittingInOneHexDigit() throws Exception {
        // setup
        superNodeApplicationCheckPoints.setSuperNodeCheckPointsForApplication("test", 4, 1);
        Set<Integer> expectedCheckPoints = new TreeSet<Integer>();
        for (int i = 0; i < 4; i++)
            expectedCheckPoints.add(i * 4 + 1);

        // act
        Set<String> superNodeCheckPoints = superNodeApplicationCheckPoints.getSuperNodeCheckPoints("test", 3, 46);

        // assert
        assertThat(superNodeCheckPoints.size(), equalTo(4));
        Iterator<String> iterator = superNodeCheckPoints.iterator();
        while (iterator.hasNext()) {
            String superNodeCheckPoint = iterator.next();
            assertThat(superNodeCheckPoint.length(), equalTo(40));
            assertThat(superNodeCheckPoint, superNodeCheckPoint.startsWith("032E"), is(true));
            assertThat(superNodeCheckPoint, expectedCheckPoints.contains(Integer.parseInt(superNodeCheckPoint.substring(4, 5), 16)), is(true));
            assertThat(superNodeCheckPoint.substring(6), equalTo(trailingZeroesOnIdWithAvailabilityZoneScope.toString()));
        }
    }

    @Test
    public void testRandomSuperNodeCheckPoint() throws Exception {
        // setup
        superNodeApplicationCheckPoints.setSuperNodeCheckPointsForApplication("test", 32, 5);
        Set<Integer> expectedCheckPoints = new TreeSet<Integer>();
        for (int i = 0; i < 32; i++)
            expectedCheckPoints.add(i * 8 + 5);

        // act
        String superNodeCheckPoint = superNodeApplicationCheckPoints.getRandomSuperNodeCheckPoint("test", 3, 46);

        // assert
        assertThat(superNodeCheckPoint.length(), equalTo(40));
        assertThat(superNodeCheckPoint, superNodeCheckPoint.startsWith("032E"), is(true));
        assertThat(superNodeCheckPoint, expectedCheckPoints.contains(Integer.parseInt(superNodeCheckPoint.substring(4, 6), 16)), is(true));
        assertThat(superNodeCheckPoint.substring(6), equalTo(trailingZeroesOnIdWithAvailabilityZoneScope.toString()));
    }
}
