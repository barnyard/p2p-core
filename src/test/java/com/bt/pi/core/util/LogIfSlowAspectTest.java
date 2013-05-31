package com.bt.pi.core.util;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertFalse;
import static junit.framework.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.io.IOException;

import javax.annotation.Resource;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import com.bt.pi.core.util.InstrumentedLogIfSlowAspect.TestLogIfSlowAspectListener;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration
public class LogIfSlowAspectTest {

    @Resource
    private InstrumentedLogIfSlowAspect aspect;

    @Resource
    private ExampleLogIfSlowForTest exampleLogIfSlow;

    private TestListener listener;

    @Before
    public void setup() {
        listener = new TestListener();
        aspect.setListener(listener);

    }

    @Test
    public void shouldLogIfSlowNoArgs() throws IOException {
        exampleLogIfSlow.testMethodNoArguments();
        assertTrue(listener.called);

        assertEquals(listener.heading, ExampleLogIfSlowForTest.class.getCanonicalName() + ".testMethodNoArguments");
        assertTrue(listener.time > 100);

    }

    @Test
    public void shouldLogIfSlowWithArgs() throws IOException, InterruptedException {
        Thread.sleep(500);
        exampleLogIfSlow.testMethodOneArgument("This is one argument");
        assertTrue(listener.called);

        assertEquals(listener.heading, ExampleLogIfSlowForTest.class.getCanonicalName() + ".testMethodOneArgument(This is one argument)");
        assertTrue(listener.time > 100);

    }

    @Test
    public void shouldOnlyPrintOneLine() throws InterruptedException, IOException {
        // Sleep to make sure that there are no previous delays.
        Thread.sleep(5000);
        String shouldBeLastLine = ExampleLogIfSlowForTest.class.getCanonicalName() + ".testMethodNoArguments";
        System.err.println("Should be last line: " + shouldBeLastLine);
        exampleLogIfSlow.testMethodNoArguments();
        Thread.sleep(100);
        assertTrue(listener.called);
        System.err.println("listener.heading: " + listener.heading);
        assertEquals(listener.heading, shouldBeLastLine);
        assertTrue(listener.time > 100);
        setup();

        Thread.sleep(300);

        // Now we print log because we exceeded delay time.
        exampleLogIfSlow.testMethodNoArguments();
        assertFalse(listener.called);

        assertNull(listener.heading);

    }

    private class TestListener implements TestLogIfSlowAspectListener {

        private long time;
        private String heading;
        private boolean called = false;

        @Override
        public void call(String heading, long time) {
            this.time = time;
            this.heading = heading;
            this.called = true;

        }

    }
}
