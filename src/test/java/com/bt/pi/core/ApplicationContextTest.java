package com.bt.pi.core;

import org.junit.Ignore;
import org.junit.Test;
import org.springframework.context.support.AbstractApplicationContext;
import org.springframework.context.support.FileSystemXmlApplicationContext;

public class ApplicationContextTest {

    @Test
    @Ignore
    public void testLoadingApplicationContext() throws Exception {
        AbstractApplicationContext applicationContext = new FileSystemXmlApplicationContext("classpath:applicationContext-p2p-core.xml");
        applicationContext.destroy();
        applicationContext = null;
    }
}
