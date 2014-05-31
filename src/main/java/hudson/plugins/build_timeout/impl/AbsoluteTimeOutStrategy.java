package hudson.plugins.build_timeout.impl;

import static hudson.plugins.build_timeout.BuildTimeoutWrapper.MINIMUM_TIMEOUT_MILLISECONDS;

import hudson.Extension;
import hudson.model.Descriptor;
import hudson.model.Run;
import hudson.plugins.build_timeout.BuildTimeOutStrategy;
import hudson.plugins.build_timeout.BuildTimeOutStrategyDescriptor;
import org.kohsuke.stapler.DataBoundConstructor;

/**
 * If the build took longer than <tt>timeoutMinutes</tt> amount of minutes, it will be terminated.
 */
public class AbsoluteTimeOutStrategy extends BuildTimeOutStrategy {

    public final int timeoutMinutes;

    @DataBoundConstructor
    public AbsoluteTimeOutStrategy(int timeoutMinutes) {
        this.timeoutMinutes = Math.max((int) (MINIMUM_TIMEOUT_MILLISECONDS / MINUTES), timeoutMinutes);
    }

    @Override
    public long getTimeOut(Run run) {
        return MINUTES * timeoutMinutes;
    }

    public Descriptor<BuildTimeOutStrategy> getDescriptor() {
        return DESCRIPTOR;
    }

    @Extension(ordinal=100) // This is displayed at the top as the default
    public static final DescriptorImpl DESCRIPTOR = new DescriptorImpl();

    public static class DescriptorImpl extends BuildTimeOutStrategyDescriptor {

        @Override
        public String getDisplayName() {
            return "Absolute";
        }
    }
}
