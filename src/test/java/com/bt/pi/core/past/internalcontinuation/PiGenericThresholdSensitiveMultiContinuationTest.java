package com.bt.pi.core.past.internalcontinuation;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;

import java.util.Arrays;

import org.junit.Before;
import org.junit.Test;

import rice.Continuation;
import rice.p2p.past.PastException;

@SuppressWarnings("unchecked")
public class PiGenericThresholdSensitiveMultiContinuationTest {
    private PiGenericThresholdSensitiveMultiContinuation multiContinuation;

    @Before
    public void before() {
        Continuation<Object, Exception> parentContinuation = mock(Continuation.class);
        multiContinuation = new PiGenericThresholdSensitiveMultiContinuation(parentContinuation, 4, .75);
    }

    @Test
    public void testGetResult() throws Exception {

        // seetup
        multiContinuation.getSubContinuation(0).receiveResult(true);
        multiContinuation.getSubContinuation(1).receiveResult(false);
        multiContinuation.getSubContinuation(2).receiveResult(true);
        multiContinuation.getSubContinuation(3).receiveResult(false);

        // act & assert
        assertEquals("[true, false, true, false]", Arrays.toString((Boolean[]) multiContinuation.getResult()));
    }

    @Test
    public void testIsDone() throws Exception {

        // seetup
        multiContinuation.getSubContinuation(0).receiveResult(true);
        multiContinuation.getSubContinuation(1).receiveResult(true);
        multiContinuation.getSubContinuation(2).receiveResult(true);
        multiContinuation.getSubContinuation(3).receiveResult(true);

        // act & assert
        assertTrue(multiContinuation.isDone());
    }

    @Test
    public void testIsDone75Percent() throws Exception {

        // seetup
        multiContinuation.getSubContinuation(0).receiveResult(true);
        multiContinuation.getSubContinuation(1).receiveResult(true);
        multiContinuation.getSubContinuation(2).receiveResult(true);
        multiContinuation.getSubContinuation(3).receiveResult(false);

        // act & assert
        assertTrue(multiContinuation.isDone());
    }

    @Test(expected = PastException.class)
    public void testIsDoneBelowSuccessThreshold() throws Exception {

        // seetup
        multiContinuation.getSubContinuation(0).receiveResult(true);
        multiContinuation.getSubContinuation(1).receiveResult(true);
        multiContinuation.getSubContinuation(2).receiveResult(false);
        multiContinuation.getSubContinuation(3).receiveResult(false);

        // act
        multiContinuation.isDone();
    }

    @Test
    public void testIsDoneMoreResultsToCome() throws Exception {

        // seetup
        multiContinuation.getSubContinuation(0).receiveResult(true);
        multiContinuation.getSubContinuation(1).receiveResult(true);

        // act & assert
        assertFalse(multiContinuation.isDone());
    }

    @Test
    public void testSetResult() throws Exception {
        // seetup
        multiContinuation.setResult(0, true);
        multiContinuation.setResult(1, true);
        multiContinuation.setResult(2, true);
        multiContinuation.setResult(3, true);
        // act & assert
        assertTrue(multiContinuation.isDone());
    }

    @Test(expected = PastException.class)
    public void testSetResultWithBadValues() throws Exception {
        // seetup
        multiContinuation.setResult(0, false);
        multiContinuation.setResult(1, false);
        multiContinuation.setResult(2, false);
        multiContinuation.setResult(3, false);
        // act & assert
        assertFalse(multiContinuation.isDone());
    }

}
