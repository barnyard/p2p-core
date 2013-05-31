package com.bt.pi.core.application;

import static junit.framework.Assert.assertNotNull;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;

import java.util.concurrent.TimeUnit;

import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import rice.p2p.scribe.ScribeMultiClient;

import com.bt.pi.core.application.activation.ApplicationActivator;
import com.bt.pi.core.entity.PiLocation;
import com.bt.pi.core.id.KoalaIdFactory;
import com.bt.pi.core.id.PId;
import com.bt.pi.core.scope.NodeScope;
import com.bt.pi.core.scribe.KoalaScribeImpl;

@RunWith(MockitoJUnitRunner.class)
public class SuperNodeApplicationBaseTest {
    private PiLocation topic = new PiLocation("topic:test", NodeScope.REGION);

    @Mock
    private ApplicationActivator applicationActivator;

    @SuppressWarnings("unused")
    @Mock
    private KoalaScribeImpl koalaScribeImpl;

    @SuppressWarnings("unused")
    @Mock
    private KoalaIdFactory koalaIdFactory;

    @InjectMocks
    private SuperNodeApplicationBase superNodeApplication = spy(new SuperNodeApplicationBase() {
        @Override
        public String getApplicationName() {
            return null;
        }

        @Override
        protected NodeScope getSuperNodeTopicScope() {
            return NodeScope.REGION;
        }

        @Override
        protected String getSuperNodeTopicUrl() {
            return "topic:test";
        }
    });

    @Test
    public void shouldGetApplicationActivator() throws Exception {
        // act
        ApplicationActivator result = superNodeApplication.getApplicationActivator();

        // assert
        assertThat(result, equalTo(applicationActivator));
    }

    @Test
    public void shouldGetActivationCheckPeriodSecs() throws Exception {
        // setup
        superNodeApplication.setActivationCheckPeriodSecs(100);

        // act
        int result = superNodeApplication.getActivationCheckPeriodSecs();

        // assert
        assertThat(result, equalTo(100));
    }

    @Test
    public void shouldGetStartTimeOut() throws Exception {
        // setup
        superNodeApplication.setStartTimeoutMillis(1000);

        // act & assert
        assertThat(superNodeApplication.getStartTimeout(), equalTo(1000L));
        assertThat(superNodeApplication.getStartTimeoutUnit(), equalTo(TimeUnit.MILLISECONDS));
    }

    @Test
    public void shouldSubscribeToTopicWhenGoingActive() throws Exception {
        // setup
        doNothing().when(superNodeApplication).subscribe(topic, superNodeApplication);

        // act
        boolean isActive = superNodeApplication.becomeActive();

        // assert
        verify(superNodeApplication).subscribe(topic, superNodeApplication);
        assertThat(isActive, is(true));
    }

    @Test
    public void shouldUnsubscribeFromTopicWhenGoingPassive() throws Exception {
        // setup
        doNothing().when(superNodeApplication).unsubscribe(topic, superNodeApplication);

        // act
        superNodeApplication.becomePassive();

        // assert
        verify(superNodeApplication).unsubscribe(topic, superNodeApplication);
    }

    @Test(expected = UnsupportedOperationException.class)
    public void anycastShouldThrowUnsupportedException() {
        // act
        superNodeApplication.handleAnycast(null, null, null);
    }

    @Test(expected = UnsupportedOperationException.class)
    public void deliverPubSubShouldThrowUnsupportedException() {
        // act
        superNodeApplication.deliver((PubSubMessageContext) null, null, null);
    }

    @Test(expected = UnsupportedOperationException.class)
    public void deliverDirectedShouldThrowUnsupportedException() {
        // act
        superNodeApplication.deliver((PId) null, (ReceivedMessageContext) null);
    }

    @Test
    @Ignore
    public void handleNodeDepartureShouldInvokeApplicationActivator() {
        // act
        superNodeApplication.handleNodeDeparture("anode");

        // assert
        // verify that the application activator is called for an active/passive check
        fail();
    }

    @Test
    public void shouldGetScribe() {

        // assert
        assertNotNull(superNodeApplication.getScribe());
    }

    @Test
    public void shouldHandlSuccess() {
        superNodeApplication.subscribe(mock(PId.class), mock(ScribeMultiClient.class));
    }

}
