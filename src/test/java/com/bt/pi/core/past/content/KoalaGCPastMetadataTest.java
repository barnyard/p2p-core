package com.bt.pi.core.past.content;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

import org.junit.Test;

import com.bt.pi.core.past.content.KoalaGCPastMetadata;

public class KoalaGCPastMetadataTest {
    @Test
    public void shouldGetVersionAndDeletedDeletableFlag() throws Exception {
        // act
        KoalaGCPastMetadata koalaGCPastMetadata = new KoalaGCPastMetadata(10, true, "test");

        // assert
        assertThat(koalaGCPastMetadata.getExpiration(), equalTo(10L));
        assertThat(koalaGCPastMetadata.isDeletedAndDeletable(), is(true));
        assertThat(koalaGCPastMetadata.getEntityType(), equalTo("test"));
    }
}
