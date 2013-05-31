package com.bt.pi.core.testing;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;

import java.util.concurrent.Executor;

import org.apache.commons.lang.ArrayUtils;
import org.junit.Before;
import org.junit.Test;

public class StubCommandExecutorTest {

    private StubCommandExecutor stubCommandExecutor;

    @Before
    public void before() {
        Executor executor = mock(Executor.class);
        stubCommandExecutor = new StubCommandExecutor(executor);
    }

    @Test
    public void test() throws Exception {
        // act
        stubCommandExecutor.executeScript(new String[] { "1" }, null);
        stubCommandExecutor.executeScript(new String[] { "2" }, null, false);
        stubCommandExecutor.executeScript(new String[] { "3" }, null, 0);
        stubCommandExecutor.executeScript(null, new String[] { "4" }, null, false);
        stubCommandExecutor.executeScript(new String[] { "5" }, null, false, 0);
        stubCommandExecutor.executeScript(new String[] { "6" }, null, 0, false);
        stubCommandExecutor.executeScript(null, new String[] { "7" }, null, false, 0);
        stubCommandExecutor.executeScript(new String[] { "8" }, null, false, 0, false);
        stubCommandExecutor.executeScript(null, new String[] { "9" }, null, false, 0, false);

        // verify
        for (int i = 0; i < stubCommandExecutor.getCommands().size(); i++) {
            assertEquals("{" + (i + 1) + "}", ArrayUtils.toString(stubCommandExecutor.getCommands().toArray()[i]));
        }
        assertEquals(9, stubCommandExecutor.getCommands().size());
    }

    @Test
    public void testAssertCommandArrayTrue() throws Exception {
        // setup
        stubCommandExecutor.executeScript(new String[] { "1", "2", "3" }, null);
        stubCommandExecutor.executeScript(new String[] { "4", "5", "6" }, null);
        stubCommandExecutor.executeScript(new String[] { "7", "8", "9" }, null);
        stubCommandExecutor.executeScript(new String[] { "A", "B", "C" }, null);
        stubCommandExecutor.executeScript(new String[] { "D", "E", "F" }, null);
        String[] arrayTarget = "A B C".split(" ");

        // act
        boolean result = stubCommandExecutor.assertCommand(arrayTarget);

        // assert
        assertTrue(result);
    }

    @Test
    public void testAssertCommandArrayFalse() throws Exception {
        // setup
        stubCommandExecutor.executeScript(new String[] { "1", "2", "3" }, null);
        stubCommandExecutor.executeScript(new String[] { "4", "5", "6" }, null);
        stubCommandExecutor.executeScript(new String[] { "7", "8", "9" }, null);
        stubCommandExecutor.executeScript(new String[] { "A", "B", "C" }, null);
        stubCommandExecutor.executeScript(new String[] { "D", "E", "F" }, null);
        String[] arrayTarget = "A Z V".split(" ");

        // act
        boolean result = stubCommandExecutor.assertCommand(arrayTarget);

        // assert
        assertFalse(result);
    }

    @Test
    public void testAssertCommandMissingFalse() throws Exception {
        // setup
        stubCommandExecutor.executeScript(new String[] { "1", "2", "3" }, null);
        stubCommandExecutor.executeScript(new String[] { "4", "5", "6" }, null);
        stubCommandExecutor.executeScript(new String[] { "7", "8", "9" }, null);
        stubCommandExecutor.executeScript(new String[] { "A", "B", "C" }, null);
        stubCommandExecutor.executeScript(new String[] { "D", "E", "F" }, null);
        String[] arrayTarget = "A B C".split(" ");

        // act
        boolean result = stubCommandExecutor.assertCommandMissing(arrayTarget);

        // assert
        assertFalse(result);
    }

    @Test
    public void testAssertCommandMissingTrue() throws Exception {
        // setup
        stubCommandExecutor.executeScript(new String[] { "1", "2", "3" }, null);
        stubCommandExecutor.executeScript(new String[] { "4", "5", "6" }, null);
        stubCommandExecutor.executeScript(new String[] { "7", "8", "9" }, null);
        stubCommandExecutor.executeScript(new String[] { "A", "B", "C" }, null);
        stubCommandExecutor.executeScript(new String[] { "D", "E", "F" }, null);
        String[] arrayTarget = "A X R".split(" ");

        // act
        boolean result = stubCommandExecutor.assertCommandMissing(arrayTarget);

        // assert
        assertTrue(result);
    }

    @Test
    public void testAssertThatEmbedKeyCommandIsRunTrue() throws Exception {
        // setup
        String instanceImagePath = "instanceImagePath";
        stubCommandExecutor.executeScript(new String[] { "1", "2", "3" }, null);
        stubCommandExecutor.executeScript(new String[] { "4", "5", "6" }, null);
        stubCommandExecutor.executeScript(new String[] { "/opt/pi/current/bin/add_key.sh", instanceImagePath, System.getProperty("java.io.tmpdir") + "/sckey.tmp" }, null);

        // act
        boolean result = stubCommandExecutor.assertThatEmbedKeyCommandIsRun(instanceImagePath);

        // assert
        assertTrue(result);
    }

    @Test
    public void testAssertThatEmbedKeyCommandIsRunFalse() throws Exception {
        // setup
        String instanceImagePath = "instanceImagePath";
        stubCommandExecutor.executeScript(new String[] { "1", "2", "3" }, null);
        stubCommandExecutor.executeScript(new String[] { "4", "5", "6" }, null);
        stubCommandExecutor.executeScript(new String[] { "/opt/pi/current/bin/add_key.sh", instanceImagePath, System.getProperty("java.io.tmpdir") + "/sckey.tmp1" }, null);

        // act
        boolean result = stubCommandExecutor.assertThatEmbedKeyCommandIsRun(instanceImagePath);

        // assert
        assertFalse(result);
    }
}
