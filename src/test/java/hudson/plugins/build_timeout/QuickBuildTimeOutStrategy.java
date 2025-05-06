package hudson.plugins.build_timeout;

import hudson.init.InitMilestone;
import hudson.init.Initializer;
import hudson.model.Items;
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

    @Initializer(after=InitMilestone.PLUGINS_STARTED)
    public static void registerAlias() {
        // This is extracted from inner of BuildTimeoutWrapperIntegrationTest
        Items.XSTREAM2.addCompatibilityAlias(
                "hudson.plugins.build_timeout.BuildTimeoutWrapperIntegrationTest$QuickBuildTimeOutStrategy",
                QuickBuildTimeOutStrategy.class
        );
    }

    public Object readResolve()
    {
        if (timeout == 0) {
            return new QuickBuildTimeOutStrategy(5000);
        }
        return this;
    }

    @Override public long getTimeOut(Run run) {
        return timeout;
    }
    @Override public Descriptor<BuildTimeOutStrategy> getDescriptor() {
        throw new UnsupportedOperationException();
    }
}