package com.bt.pi.core.application.activation;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.Set;

import org.junit.Before;
import org.junit.Test;

public class ApplicationRegistryTest {
    private static final String APP_NAME = "app-name";
    private ApplicationRegistry appManager;
    private ApplicationRecord applicationRecord;
    private ActivationAwareApplication app;

    @Before
    public void before() {
        app = mock(ActivationAwareApplication.class);
        when(app.getApplicationName()).thenReturn(APP_NAME);

        applicationRecord = mock(ApplicationRecord.class);
        appManager = new ApplicationRegistry();
    }

    @Test
    public void testGetApplicationStatus() {
        // setup
        appManager.registerApplication(app);
        appManager.setApplicationStatus(APP_NAME, ApplicationStatus.CHECKING);

        // act
        ApplicationStatus result = appManager.getApplicationStatus(APP_NAME);

        // assert
        assertEquals(ApplicationStatus.CHECKING, result);
    }

    @Test
    public void testRegisterApplication() {
        // act
        appManager.registerApplication(app);

        // assert
        assertEquals(ApplicationStatus.NOT_INITIALIZED, appManager.getApplicationStatus(APP_NAME));
    }

    @Test(expected = ApplicationAlreadyExistsException.class)
    public void testRegisterDuplicateApplicationThrows() {
        // setup
        appManager.registerApplication(app);

        // act
        appManager.registerApplication(app);
    }

    @Test
    public void testSetAppState() {
        // setup
        appManager.registerApplication(app);

        // act
        appManager.setApplicationStatus(APP_NAME, ApplicationStatus.ACTIVE);

        // assert
        assertEquals(ApplicationStatus.ACTIVE, appManager.getApplicationStatus(APP_NAME));
    }

    @Test(expected = UnknownApplicationException.class)
    public void testSetAppStateFailsWhenNotRegistered() {
        // act
        appManager.setApplicationStatus(APP_NAME, ApplicationStatus.ACTIVE);
    }

    @Test(expected = UnknownApplicationException.class)
    public void shouldThrowIfAppUnknown() {
        // act
        appManager.getApplicationStatus("some-other-app");
    }

    @Test
    public void testSetAppRecord() {
        // setup
        appManager.registerApplication(app);

        // act
        appManager.setCachedApplicationRecord(APP_NAME, applicationRecord);

        // assert
        assertEquals(applicationRecord, appManager.getCachedApplicationRecord(APP_NAME));
    }

    @Test
    public void testGetKeys() {
        // setup
        appManager.registerApplication(app);
        appManager.setCachedApplicationRecord(APP_NAME, applicationRecord);

        // act
        Set<String> apps = appManager.getApplicationNames();

        // assert
        assertEquals(1, apps.size());
        assertEquals(APP_NAME, (String) apps.toArray()[0]);
    }

    @Test
    public void testGetApplicationInfo() {
        // setup
        appManager.registerApplication(app);
        appManager.setCachedApplicationRecord(APP_NAME, applicationRecord);

        // act
        ApplicationInfo appInfo = appManager.getApplicationInfor(APP_NAME);

        // assert
        assertEquals(app, appInfo.getApplication());
        assertEquals(applicationRecord, appInfo.getCachedApplicationRecord());
    }

    @Test(expected = UnknownApplicationException.class)
    public void testSetAppRecordFailsWhenNotRegistered() {
        // act
        appManager.setCachedApplicationRecord(APP_NAME, applicationRecord);
    }
}
