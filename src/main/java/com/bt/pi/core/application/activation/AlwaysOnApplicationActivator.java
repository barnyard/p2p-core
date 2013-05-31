package com.bt.pi.core.application.activation;

import java.util.TimerTask;

import org.apache.commons.lang.NotImplementedException;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.stereotype.Component;

@Component
public class AlwaysOnApplicationActivator extends ApplicationActivatorBase {
    private static final Log LOG = LogFactory.getLog(AlwaysOnApplicationActivator.class);

    public AlwaysOnApplicationActivator() {
    }

    @Override
    protected void checkAndActivate(ActivationAwareApplication application, TimerTask timerTask) {
        LOG.debug(String.format("checkAndActivate(%s)", application.getApplicationName()));
        executeApplicationActivation(application);
        if (timerTask != null) {
            LOG.debug(String.format("Canceling long startup rollback task. Successfully: %s", timerTask.cancel()));
        }
    }

    @Override
    protected ApplicationActivationCheckStatus checkLocalActivationPreconditions(ActivationAwareApplication application) {
        LOG.debug(String.format("checkLocalActivationPreconditions(%s)", application.getApplicationName()));
        return ApplicationActivationCheckStatus.ACTIVATE;
    }

    @Override
    public void deActivateNode(String id, ActivationAwareApplication anActivationAwareApplication) {
        throw new NotImplementedException();
    }

    @Override
    protected void checkActiveApplicationStillActiveAndHeartbeat(ActivationAwareApplication application) {
    }
}
