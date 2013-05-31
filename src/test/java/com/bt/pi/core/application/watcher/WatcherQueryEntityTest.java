package com.bt.pi.core.application.watcher;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.junit.Assert.assertThat;

import org.junit.Test;

import com.bt.pi.core.application.watcher.WatcherQueryEntity;

public class WatcherQueryEntityTest {
    private String queue = "id";
    private String entity = "id2";
    private WatcherQueryEntity watcherQueryEntity = new WatcherQueryEntity();

    @Test
    public void testGetUrl() throws Exception {
        // setup
        watcherQueryEntity.setQueueUrl(queue);
        watcherQueryEntity.setEntityUrl(entity);

        // act
        String result = watcherQueryEntity.getUrl();

        // assert
        assertThat(result, equalTo("wqe:id:id2"));
    }

    @Test
    public void testGetUriScheme() throws Exception {
        // act
        String result = watcherQueryEntity.getUriScheme();

        // assert
        assertThat(result, equalTo("wqe"));
    }

    @Test
    public void testGetType() throws Exception {
        // act
        String result = watcherQueryEntity.getType();

        // assert
        assertThat(result, equalTo("WatcherQueryEntity"));
    }

    @Test
    public void testGetQueue() throws Exception {
        // setup
        watcherQueryEntity.setQueueUrl(queue);

        // act
        String result = watcherQueryEntity.getQueueUrl();

        // assert
        assertThat(result, equalTo(queue));
    }

    @Test
    public void testGetEntity() throws Exception {
        // setup
        watcherQueryEntity.setEntityUrl(entity);

        // act
        String result = watcherQueryEntity.getEntityUrl();

        // assert
        assertThat(result, equalTo(entity));
    }

    @Test
    public void testGetQueueWithConstructorArgument() throws Exception {
        // setup
        watcherQueryEntity = new WatcherQueryEntity(queue, entity);

        // act
        String queueResult = watcherQueryEntity.getQueueUrl();
        String entityResult = watcherQueryEntity.getEntityUrl();

        // assert
        assertThat(queueResult, equalTo(queue));
        assertThat(entityResult, equalTo(entity));
    }
}
