package com.bt.pi.core.application.activation;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.List;

import org.junit.Test;

public class GlobalScopedApplicationRecordTest {
    GlobalScopedApplicationRecord applicationRecord = new GlobalScopedApplicationRecord();

    @Test
    public void shouldReturnValidUrl() {
        // setup
        applicationRecord.setApplicationName("anApp");

        // act
        String url = applicationRecord.getUrl();

        // assert
        assertEquals("globalapp:anApp", url);
    }

    @Test
    public void shouldReturnValidType() {
        // setup

        // act
        String type = applicationRecord.getType();

        // assert
        assertEquals(GlobalScopedApplicationRecord.class.getSimpleName(), type);
    }

    @Test
    public void shouldConstructApplicationRecordWithApplicationName() {
        // act
        applicationRecord = new GlobalScopedApplicationRecord("appName");

        // assert
        assertEquals("appName", applicationRecord.getApplicationName());
        assertEquals("globalapp:appName", applicationRecord.getUrl());
    }

    @Test
    public void shouldConstructApplicationRecordWithApplicationNameAndVersion() {
        // act
        applicationRecord = new GlobalScopedApplicationRecord("anApp", 150);

        // assert
        assertEquals("anApp", applicationRecord.getApplicationName());
        assertEquals(150, applicationRecord.getVersion());
    }

    @Test
    public void shouldConstructApplicationRecordWithNumberOfRequiredActiveApps() {
        // act
        applicationRecord = new GlobalScopedApplicationRecord("anApp", 100, 2);

        // assert
        assertEquals(2, applicationRecord.getRequiredActive());
    }

    @Test
    public void shouldConstructApplicationRecordWithListOfResources() {
        // setup
        List<String> resources = new ArrayList<String>();
        resources.add("res1");
        resources.add("res2");

        // act
        applicationRecord = new GlobalScopedApplicationRecord("anApp", 101, resources);

        // assert
        assertEquals(2, applicationRecord.getRequiredActive());
        assertEquals(2, applicationRecord.getActiveNodeMap().size());
        assertTrue(applicationRecord.getActiveNodeMap().containsKey("res1"));
        assertTrue(applicationRecord.getActiveNodeMap().containsKey("res2"));
    }

    @Test
    public void shouldGetCorrectURIScheme() {
        // setup

        // act
        String uriScheme = applicationRecord.getUriScheme();

        // assert
        assertEquals("globalapp", uriScheme);
    }
}
