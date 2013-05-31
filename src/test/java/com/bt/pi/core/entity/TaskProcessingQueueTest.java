package com.bt.pi.core.entity;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;

import java.util.Collection;
import java.util.Iterator;

import org.junit.Before;
import org.junit.Test;

import com.bt.pi.core.parser.KoalaJsonParser;

public class TaskProcessingQueueTest {
    private static final String URL = "queue:test";
    private TaskProcessingQueue taskProcessingQueue;
    private String url1 = "url1";
    private String url2 = "url2";

    @Before
    public void setUp() throws Exception {
        this.taskProcessingQueue = new TaskProcessingQueue(URL);
    }

    @Test
    public void testRoundTrip() {
        // setup
        KoalaJsonParser koalaJsonParser = new KoalaJsonParser();

        this.taskProcessingQueue.add(url1);
        String anOwnerNodeId = "ownerId";
        this.taskProcessingQueue.setOwnerNodeIdForUrl(url1, anOwnerNodeId);
        this.taskProcessingQueue.add(url2);

        // act
        String json = koalaJsonParser.getJson(this.taskProcessingQueue);
        TaskProcessingQueue reverse = (TaskProcessingQueue) koalaJsonParser.getObject(json, TaskProcessingQueue.class);

        // assert
        assertEquals(2, reverse.size());
        assertEquals(URL, reverse.getUrl());
        assertEquals(anOwnerNodeId, reverse.peek().getOwnerNodeId());
        Collection<TaskProcessingQueueItem> collection = reverse.getStale(-1);
        assertEquals(2, collection.size());
        Iterator<TaskProcessingQueueItem> iterator = collection.iterator();
        assertEquals(url1, iterator.next().getUrl());
        assertEquals(url2, iterator.next().getUrl());
    }

    @Test
    public void shouldSetLastUpdatedOnEntryToQueue() {
        // act
        this.taskProcessingQueue.add(url1);

        // assert
        assertTrue(Math.abs(this.taskProcessingQueue.peek().getLastUpdatedMillis() - System.currentTimeMillis()) < 200);
    }

    @Test
    public void testGetStaleNoneOlder() {
        // setup
        this.taskProcessingQueue.add(url1);

        // act
        Collection<TaskProcessingQueueItem> result = this.taskProcessingQueue.getStale(10);

        // assert
        assertEquals(0, result.size());
        assertEquals(1, this.taskProcessingQueue.size());
    }

    @Test
    public void testGetStaleFound() throws Exception {
        // setup
        this.taskProcessingQueue.add(url1);

        // act
        Thread.sleep(30);
        Collection<TaskProcessingQueueItem> result = this.taskProcessingQueue.getStale(10);

        // assert
        assertEquals(1, result.size());
        assertEquals(url1, result.iterator().next().getUrl());
        assertEquals(1, this.taskProcessingQueue.size());
    }

    @Test
    public void testRemove() {
        // setup
        this.taskProcessingQueue.add(url1);
        this.taskProcessingQueue.add(url2);

        // act
        boolean result = this.taskProcessingQueue.remove(url1);

        // assert
        assertTrue(result);
        assertEquals(1, this.taskProcessingQueue.size());
        assertEquals(url2, this.taskProcessingQueue.getStale(-1).iterator().next().getUrl());
    }

    @Test
    public void testRemoveNotFound() {
        // setup

        // act
        boolean result = this.taskProcessingQueue.remove(url1);

        // assert
        assertFalse(result);
    }

    @Test
    public void testRemoveOwnerFromAllTasks() {
        // setup
        String owner = "theMan";
        this.taskProcessingQueue.add(url1, owner);
        this.taskProcessingQueue.add(url2, owner);

        // act
        this.taskProcessingQueue.removeOwnerFromAllTasks(owner);

        // assert
        assertNull(taskProcessingQueue.getNodeIdForUrl(url1));
        assertNull(taskProcessingQueue.getNodeIdForUrl(url2));
    }

    @Test
    public void testAdd() {
        // act
        boolean result = this.taskProcessingQueue.add(url1);

        // assert
        assertTrue(result);
    }

    @Test
    public void testAddAlreadyExists() {
        // setup
        this.taskProcessingQueue.add(url1);

        // act
        boolean result = this.taskProcessingQueue.add(url1);

        // assert
        assertFalse(result);
    }

    @Test
    public void shouldRemoveAllTasksFromQueue() {
        // setup
        this.taskProcessingQueue.add(url1);
        this.taskProcessingQueue.add(url2);

        assertThat(taskProcessingQueue.size(), is(2));

        // act
        this.taskProcessingQueue.removeAllTasks();

        // assert
        assertThat(taskProcessingQueue.size(), is(0));
    }

    @Test
    public void shouldGetUrlFromQueue() {
        // setup
        this.taskProcessingQueue.add(url1);
        this.taskProcessingQueue.add(url2);

        assertThat(taskProcessingQueue.size(), is(2));

        // act
        TaskProcessingQueueItem result = this.taskProcessingQueue.get(url1);

        // assert
        assertEquals(url1, result.getUrl());
        assertNull(taskProcessingQueue.get("dont exist"));
    }
}
