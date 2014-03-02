package hudson.plugins.build_timeout;

import hudson.model.Descriptor;
import hudson.model.Run;

/**
 * Timeout strategy for testing purpose.
 */
public class QuickBuildTimeOutStrategy extends BuildTimeOutStrategy {
    private final long timeout;
    
    public QuickBuildTimeOutStrategy() {
        this(5000);
    }
    
    public QuickBuildTimeOutStrategy(long milliseconds) {
        this.timeout = milliseconds;
    }
    
    @Override public long getTimeOut(Run run) {
        return timeout;
    }
    @Override public Descriptor<BuildTimeOutStrategy> getDescriptor() {
        throw new UnsupportedOperationException();
    }
}