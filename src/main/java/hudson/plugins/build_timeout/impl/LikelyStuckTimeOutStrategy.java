package hudson.plugins.build_timeout.impl;

import hudson.Extension;
import hudson.model.Descriptor;
import hudson.model.Executor;
import hudson.model.Queue;
import hudson.model.Run;
import hudson.model.queue.Executables;
import hudson.plugins.build_timeout.BuildTimeOutStrategy;
import hudson.plugins.build_timeout.BuildTimeOutStrategyDescriptor;
import hudson.util.TimeUnit2;
import org.kohsuke.stapler.DataBoundConstructor;

/**
 * Get the time considered it stuck.
 *
 * @return 10 times as much as eta if eta is available, else 24 hours.
 * @see Executor#isLikelyStuck()
 */
public class LikelyStuckTimeOutStrategy extends BuildTimeOutStrategy {


    @DataBoundConstructor
    public LikelyStuckTimeOutStrategy() {
    }

    @Override
    public long getTimeOut(Run run) {
        Executor executor = run.getExecutor();
        if (executor == null) {
            return TimeUnit2.HOURS.toMillis(24);
        }

        Queue.Executable executable = executor.getCurrentExecutable();
        if (executable == null) {
            return TimeUnit2.HOURS.toMillis(24);
        }

        long eta = Executables.getEstimatedDurationFor(executable);
        if (eta >= 0) {
            return eta * 10;
        } else {
            return TimeUnit2.HOURS.toMillis(24);
        }
    }


    public Descriptor<BuildTimeOutStrategy> getDescriptor() {
        return DESCRIPTOR;
    }

    @Extension
    public static final DescriptorImpl DESCRIPTOR = new DescriptorImpl();

    public static class DescriptorImpl extends BuildTimeOutStrategyDescriptor {

        @Override
        public String getDisplayName() {
            return "Likely stuck";
        }
    }
}
