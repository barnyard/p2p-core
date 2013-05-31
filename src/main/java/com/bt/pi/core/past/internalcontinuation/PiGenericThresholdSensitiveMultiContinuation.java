package com.bt.pi.core.past.internalcontinuation;

import java.util.Arrays;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import rice.Continuation;
import rice.Continuation.MultiContinuation;

import com.bt.pi.core.exception.PiInsufficientResultsException;

public class PiGenericThresholdSensitiveMultiContinuation extends MultiContinuation {
    protected static final Log LOG = LogFactory.getLog(PiGenericThresholdSensitiveMultiContinuation.class);
    private double successThreshhold;
    private Boolean booleanTrue;

    /**
     * Constructor which takes a parent continuation as well as the number of results which to expect.
     * 
     * @param callBack
     *            The parent continuation
     * @param num
     *            The number of results expected to come in
     * @param successThresholdPercentage
     *            The percentage of inserts that must pass for the insert to be considered a success
     */
    @SuppressWarnings("unchecked")
    public PiGenericThresholdSensitiveMultiContinuation(Continuation callBack, int num, double successThresholdPercentage) {
        super(callBack, num);
        successThreshhold = successThresholdPercentage;
        booleanTrue = Boolean.valueOf(true);
    }

    @Override
    public boolean isDone() throws Exception {
        int numSuccess = 0;
        for (int i = 0; i < haveResult.length; i++)
            if ((haveResult[i]) && !(result[i] instanceof Exception) && !(Boolean.FALSE.equals(result[i])))
                numSuccess++;

        if (numSuccess >= (successThreshhold * haveResult.length))
            return true;

        if (!super.isDone())
            return false;

        for (int i = 0; i < result.length; i++)
            if (result[i] instanceof Exception)
                if (LOG.isDebugEnabled())
                    LOG.debug("result[" + i + "]:" + ((Exception) result[i]).getMessage());

        throw new PiInsufficientResultsException(String.format("Had only %s successful continuation results %s. Results returned: %s. - aborting.", numSuccess, result.length, Arrays.toString(result)));
    }

    public void setResult(int anIndex, Object o) {
        this.receive(anIndex, o);
    }

    public Object getResult() {
        Boolean[] b = new Boolean[result.length];
        for (int i = 0; i < b.length; i++)
            b[i] = Boolean.valueOf((result[i] == null) || booleanTrue.equals(result[i]));

        return b;
    }
}
