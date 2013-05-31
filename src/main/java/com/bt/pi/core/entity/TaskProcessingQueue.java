package com.bt.pi.core.entity;

import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Queue;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.codehaus.jackson.annotate.JsonIgnore;
import org.codehaus.jackson.annotate.JsonProperty;

/*
 * PiEntity wrapper around a Queue of Tasks
 */

public class TaskProcessingQueue extends PiEntityBase {
    private static final Log LOG = LogFactory.getLog(TaskProcessingQueue.class);
    private static final String RIGHT_SQUARE_BRACKET = "]";
    @JsonProperty
    private Queue<TaskProcessingQueueItem> tasks;
    @JsonProperty
    private String url;

    public TaskProcessingQueue() {
        this.tasks = new LinkedList<TaskProcessingQueueItem>();
    }

    public TaskProcessingQueue(String aUrl) {
        this.url = aUrl;
        this.tasks = new LinkedList<TaskProcessingQueueItem>();
    }

    @Override
    public String getType() {
        return getClass().getSimpleName();
    }

    @Override
    public String getUrl() {
        return this.url;
    }

    public boolean add(String itemUrl) {
        return add(itemUrl, null);
    }

    public boolean add(TaskProcessingQueueItem item) {
        if (tasks.contains(item)) {
            LOG.debug(String.format("item url already on queue and will not be re-added: %s", item.getUrl()));
            return false;
        }
        if (!tasks.offer(item))
            throw new RuntimeException("Queue refused offer");
        return true;
    }

    public boolean add(String itemUrl, int remainingRetries) {
        return add(new TaskProcessingQueueItem(itemUrl, remainingRetries));
    }

    public boolean add(String itemUrl, String nodeId) {
        return add(new TaskProcessingQueueItem(itemUrl, nodeId));
    }

    public int size() {
        return tasks.size();
    }

    protected TaskProcessingQueueItem peek() {
        return tasks.peek();
    }

    public void removeOwnerFromAllTasks(String anOwnerNodeId) {
        if (StringUtils.isNotBlank(anOwnerNodeId)) {
            // find the items
            for (TaskProcessingQueueItem item : this.tasks) {
                if (anOwnerNodeId.equalsIgnoreCase(item.getOwnerNodeId())) {
                    item.setOwnerNodeId(null);
                }
            }
        }
    }

    @JsonIgnore
    public void setOwnerNodeIdForUrl(String aUrl, String anOwnerNodeId) {
        if (this.tasks.contains(new TaskProcessingQueueItem(aUrl))) {
            for (TaskProcessingQueueItem item : this.tasks) {
                if (aUrl.equals(item.getUrl())) {
                    item.setOwnerNodeId(anOwnerNodeId);
                }
            }
        }
    }

    @JsonIgnore
    public String getNodeIdForUrl(String aUrl) {
        if (this.tasks.contains(new TaskProcessingQueueItem(aUrl))) {
            for (TaskProcessingQueueItem item : this.tasks) {
                if (aUrl.equals(item.getUrl())) {
                    return item.getOwnerNodeId();
                }
            }
        }
        return null;
    }

    public Collection<TaskProcessingQueueItem> getStale(long olderThan) {
        Collection<TaskProcessingQueueItem> staleItems = new LinkedList<TaskProcessingQueueItem>();
        Iterator<TaskProcessingQueueItem> tasksIterator = tasks.iterator();
        while (tasksIterator.hasNext()) {
            TaskProcessingQueueItem next = tasksIterator.next();
            if ((next.getLastUpdatedMillis() + olderThan) < System.currentTimeMillis())
                staleItems.add(next);
            else
                break;
        }
        return staleItems;
    }

    public TaskProcessingQueueItem get(String itemUrl) {
        Iterator<TaskProcessingQueueItem> tasksIterator = tasks.iterator();
        while (tasksIterator.hasNext()) {
            TaskProcessingQueueItem next = tasksIterator.next();
            if (next.getUrl().equals(itemUrl))
                return next;
        }

        return null;
    }

    public boolean remove(String itemUrl) {
        return this.tasks.remove(new TaskProcessingQueueItem(itemUrl));
    }

    @Override
    public String toString() {
        return "TaskProcessingQueue [url=" + url + ", tasks=" + tasks + RIGHT_SQUARE_BRACKET;
    }

    @Override
    public String getUriScheme() {
        // TODO: somehow use app ResourceSchemes enum
        return "queue";
    }

    public void removeAllTasks() {
        this.tasks.clear();
    }
}
