package com.bt.pi.core.application.activation;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertNull;
import static junit.framework.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.junit.Before;
import org.junit.Test;

public class InterApplicationDependenciesStoreTest {

    private static final List<String> MOCK_APP_1_EXCLUDED_APPS = Arrays.asList("mockApplication2");
    private static final List<String> MOCK_APP_2_EXCLUDED_APPS = Arrays.asList("mockApplication1");
    private static final List<String> MOCK_APP_3_EXCLUDED_APPS = Arrays.asList("mockApplication1", "mockApplication2");
    private ActivationAwareApplication mockApplication1;
    private ActivationAwareApplication mockApplication2;
    private ActivationAwareApplication mockApplication3;

    private InterApplicationDependenciesStore interApplicationDependenciesStore;

    @Before
    public void setup() {
        interApplicationDependenciesStore = new InterApplicationDependenciesStore();
        mockApplication1 = mock(ActivationAwareApplication.class);
        mockApplication2 = mock(ActivationAwareApplication.class);
        mockApplication3 = mock(ActivationAwareApplication.class);
        when(mockApplication1.getPreferablyExcludedApplications()).thenReturn(MOCK_APP_1_EXCLUDED_APPS);
        when(mockApplication2.getPreferablyExcludedApplications()).thenReturn(MOCK_APP_2_EXCLUDED_APPS);
        when(mockApplication3.getPreferablyExcludedApplications()).thenReturn(MOCK_APP_3_EXCLUDED_APPS);

        when(mockApplication1.getApplicationName()).thenReturn("mockApplication1");
        when(mockApplication2.getApplicationName()).thenReturn("mockApplication2");
        when(mockApplication3.getApplicationName()).thenReturn("mockApplication3");

    }

    @Test
    public void shouldReturnExcludedAppsForApp() {
        // setup
        interApplicationDependenciesStore.setPastryApplications(Arrays.asList(mockApplication1, mockApplication2));
        // act
        List<String> excludedAppsForApp1 = interApplicationDependenciesStore.getPreferablyExcludedApplications(mockApplication1.getApplicationName());
        List<String> excludedAppsForApp2 = interApplicationDependenciesStore.getPreferablyExcludedApplications(mockApplication2.getApplicationName());
        // assert
        assertEquals(MOCK_APP_1_EXCLUDED_APPS, excludedAppsForApp1);
        assertEquals(MOCK_APP_2_EXCLUDED_APPS, excludedAppsForApp2);

    }

    @Test
    public void shouldNotThrowNPEWhenCallingSetPreferablyExcludedApplicationsWithNullArg() {
        // act
        interApplicationDependenciesStore.setPastryApplications(null);

    }

    @Test
    public void shouldReturnNullWhenGettingAppNotInStore() {
        // setup
        interApplicationDependenciesStore.setPastryApplications(Arrays.asList(mockApplication1, mockApplication2));
        // act
        List<String> excludeAppsForMockApp = interApplicationDependenciesStore.getPreferablyExcludedApplications("notinstore");
        // assert
        assertNull(excludeAppsForMockApp);
    }

    @Test
    public void shouldMakeExclusionListsSymmetric() {
        // setup
        interApplicationDependenciesStore.setPastryApplications(Arrays.asList(mockApplication1, mockApplication2, mockApplication3));
        // act
        List<String> excludedAppsForApp1 = interApplicationDependenciesStore.getPreferablyExcludedApplications(mockApplication1.getApplicationName());
        List<String> excludedAppsForApp2 = interApplicationDependenciesStore.getPreferablyExcludedApplications(mockApplication2.getApplicationName());
        List<String> excludedAppsForApp3 = interApplicationDependenciesStore.getPreferablyExcludedApplications(mockApplication3.getApplicationName());

        // assert
        assertTrue("Mock App 1 exclusion list should contain Mock App 3", excludedAppsForApp1.contains(mockApplication3.getApplicationName()));
        assertTrue("Mock App 2 exclusion list should contain Mock App 3", excludedAppsForApp2.contains(mockApplication3.getApplicationName()));
        assertEquals(MOCK_APP_3_EXCLUDED_APPS, excludedAppsForApp3);

    }

    @Test(expected = IllegalArgumentException.class)
    public void shouldThrowExceptionIfApplicationNameIsNull() {

        // act
        interApplicationDependenciesStore.setPastryApplications(Arrays.asList(mock(ActivationAwareApplication.class)));
    }

    @Test
    public void shouldTreatAsEmptyListIfExclusionListIsNull() {
        // setup
        when(mockApplication1.getPreferablyExcludedApplications()).thenReturn(null);
        // act
        interApplicationDependenciesStore.setPastryApplications(Arrays.asList(mockApplication1));
        List<String> excludedAppsForApp1 = interApplicationDependenciesStore.getPreferablyExcludedApplications(mockApplication1.getApplicationName());
        // assert

        assertEquals(Collections.emptyList(), excludedAppsForApp1);
    }

}
