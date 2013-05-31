package com.bt.pi.core.application.activation;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;

import org.junit.Before;
import org.junit.Test;

public class ApplicationInfoTest {

    private ActivationAwareApplication app;
    private ApplicationInfo appInfo;

    @Before
    public void before() {
        app = mock(ActivationAwareApplication.class);
        appInfo = new ApplicationInfo(app);
    }

    @Test
    public void testConstructor() {
        // assert
        assertEquals(app, appInfo.getApplication());
        assertEquals(ApplicationStatus.NOT_INITIALIZED, appInfo.getApplicationStatus());
        assertNull(appInfo.getCachedApplicationRecord());
    }

    @Test
    public void testGettersAndSetters() {
        // setup
        ApplicationRecord appRecord = new GlobalScopedApplicationRecord("My idea of an agreeable person is a person who agrees with me. - Benjamin Disraeli");

        // act
        appInfo.setApplicationStatus(ApplicationStatus.ACTIVE);
        appInfo.setCachedApplicationRecord(appRecord);

        // assert
        assertEquals(ApplicationStatus.ACTIVE, appInfo.getApplicationStatus());
        assertEquals(appRecord, appInfo.getCachedApplicationRecord());
    }

    @Test
    public void testToString() {
        // setup
        appInfo.setApplicationStatus(ApplicationStatus.ACTIVE);

        // act
        String result = appInfo.toString();

        // assert
        assertTrue(result.contains(ApplicationStatus.ACTIVE.toString()));
    }
}
