package com.bt.pi.core;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.File;

import org.junit.Before;
import org.junit.Test;

import com.bt.pi.core.ExcludeHiddenFilesFilter;

public class ExcludeHiddenFilesFilterTest {
    private ExcludeHiddenFilesFilter filter;
    private File pathname;

    @Before
    public void setup() {
        filter = new ExcludeHiddenFilesFilter();
        pathname = mock(File.class);
    }

    @Test
    public void testHiddenFile() throws Exception {
        // setup
        when(pathname.isHidden()).thenReturn(true);

        // act
        boolean result = filter.accept(pathname);

        // assert
        assertThat(result, equalTo(false));
    }

    @Test
    public void testNonHiddenFile() throws Exception {
        // setup
        when(pathname.isHidden()).thenReturn(false);

        // act
        boolean result = filter.accept(pathname);

        // assert
        assertThat(result, equalTo(true));
    }
}
