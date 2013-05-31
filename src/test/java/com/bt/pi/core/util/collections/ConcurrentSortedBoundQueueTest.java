package com.bt.pi.core.util.collections;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertTrue;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThat;

import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.junit.Before;
import org.junit.Test;
import org.springframework.test.util.ReflectionTestUtils;

public class ConcurrentSortedBoundQueueTest {
    private static class MyQueueElement implements ConcurrentSortedBoundQueueElement<MyQueueElement> {
        protected String value;

        public MyQueueElement(String value) {
            this.value = value;
        }

        @Override
        public Object[] getKeysForMap() {
            return new Object[] { value };
        }

        @Override
        public int getKeysForMapCount() {
            return 1;
        }

        @Override
        public int compareTo(MyQueueElement o) {
            return value.compareTo(o.value);
        }

        @Override
        public boolean equals(Object obj) {
            if (obj == null || (!(obj instanceof MyQueueElement)))
                return false;

            return value.equals(((MyQueueElement) obj).value);
        }
    }

    private class MyQueueElementUniqueKey extends MyQueueElement {

        public MyQueueElementUniqueKey(String value) {
            super(value);

        }

        @Override
        public Object[] getKeysForMap() {

            return new String[] { "KEY" };
        }

        @Override
        public String toString() {
            return "MyQueueElementUniqueKey:" + value;
        }
    }

    private int maxSize = 10;
    private ConcurrentSortedBoundQueue<MyQueueElement> concurrentSortedBoundQueue;

    @Before
    public void setup() {
        concurrentSortedBoundQueue = new ConcurrentSortedBoundQueue<MyQueueElement>(maxSize);
    }

    @Test
    public void addShouldRemoveOldestElementsIfQueueSizeExceedsMaxSizeAndAlsoRemovesCorrepondingElementsFromMaps() throws Exception {
        // setup
        CharSequence characters = "abcdwxyzefghstuvijklopqrmn";

        // act
        for (int i = 0; i < 26; i++)
            concurrentSortedBoundQueue.add(new MyQueueElement(String.format("%c", characters.charAt(i))));

        // assert
        assertThat(concurrentSortedBoundQueue.size(), equalTo(maxSize));
        Iterator<MyQueueElement> iterator = concurrentSortedBoundQueue.iterator();
        for (char c = 'q'; iterator.hasNext(); c++) {
            MyQueueElement next = iterator.next();
            assertThat(next, equalTo(new MyQueueElement(String.format("%c", c))));
        }

        // assert maps
        for (char c = 'a'; c <= 'z'; c++) {
            if (c >= 'q')
                assertThat(concurrentSortedBoundQueue.getCollectionByKey(0, String.format("%c", c)).size(), equalTo(1));
            else
                assertNull(concurrentSortedBoundQueue.getCollectionByKey(0, String.format("%c", c)));
        }
    }

    @Test
    public void setSizeToAGreaterSizeShouldIncreaseCapacity() {
        // setup
        CharSequence characters = "abcdwxyzefghstuvijklopqrmn";
        final int newMaxSize = 20;
        concurrentSortedBoundQueue.setSize(newMaxSize);

        // act
        for (int i = 0; i < 26; i++)
            concurrentSortedBoundQueue.add(new MyQueueElement(String.format("%c", characters.charAt(i))));

        // assert
        assertThat(concurrentSortedBoundQueue.size(), equalTo(newMaxSize));
    }

    @Test
    public void setSizeToASmallerSizeShouldDecreaseCapacityAndRemoveExtra() {
        // setup
        CharSequence characters = "abcdwxyzefghstuvijklopqrmn";
        for (int i = 0; i < 26; i++)
            concurrentSortedBoundQueue.add(new MyQueueElement(String.format("%c", characters.charAt(i))));
        final int newMaxSize = 5;
        assertThat(concurrentSortedBoundQueue.size(), equalTo(maxSize));

        // act
        concurrentSortedBoundQueue.setSize(newMaxSize);

        // assert
        assertThat(concurrentSortedBoundQueue.size(), equalTo(newMaxSize));
    }

    @Test(expected = IllegalArgumentException.class)
    public void shouldNodAddItemsIfKeyIsNull() {
        // setup
        MyQueueElement myQueueElement = new MyQueueElement(null);
        // act
        concurrentSortedBoundQueue.add(myQueueElement);
    }

    @Test(expected = IllegalArgumentException.class)
    public void shouldNotAddItemsIfKeyIsEmptyString() {
        // setup
        MyQueueElement myQueueElement = new MyQueueElement("");
        // act
        concurrentSortedBoundQueue.add(myQueueElement);
    }

    @Test
    public void shouldReturnHashCode() {
        assertTrue(concurrentSortedBoundQueue.hashCode() > 0);
    }

    @Test
    public void shouldBeEqualToAnotherQueue() {
        ConcurrentSortedBoundQueue<MyQueueElement> anotherQueue = new ConcurrentSortedBoundQueue<ConcurrentSortedBoundQueueTest.MyQueueElement>(maxSize);
        assertEquals(concurrentSortedBoundQueue, anotherQueue);
    }

    @Test
    public void shouldCleanupMapsWhenOverflowingQueue() throws InterruptedException {
        // setup
        CharSequence characters = "abcdefghijklmnopqrstuvwxyz";

        for (int i = 0; i < 20; i++) {
            MyQueueElementUniqueKey entity = new MyQueueElementUniqueKey(String.format("%c", characters.charAt(i)));

            concurrentSortedBoundQueue.add(entity);

        }
        assertThat(concurrentSortedBoundQueue.size(), equalTo(maxSize));
        @SuppressWarnings("unchecked")
        Map<Object, Collection<MyQueueElementUniqueKey>> map = (Map<Object, Collection<MyQueueElementUniqueKey>>) ((List<MyQueueElementUniqueKey>) ReflectionTestUtils.getField(concurrentSortedBoundQueue, "maps")).get(0);
        Collection<MyQueueElementUniqueKey> internalCollection = map.get("KEY");
        assertThat(internalCollection.size(), equalTo(maxSize));

    }

    @Test
    public void shouldBoundQueueIfMaxSizeIsSetToZero() {
        // setup
        int ZERO = 0;

        concurrentSortedBoundQueue = new ConcurrentSortedBoundQueue<MyQueueElement>(ZERO);

        // act
        CharSequence characters = "abcdwxyzefghstuvijklopqrmn";
        for (int i = 0; i < 26; i++)
            concurrentSortedBoundQueue.add(new MyQueueElement(String.format("%c", characters.charAt(i))));

        // assert
        assertThat(concurrentSortedBoundQueue.size(), equalTo(ZERO));
    }
}
