package hudson.plugins.build_timeout.impl;

import hudson.Extension;
import hudson.model.AbstractBuild;
import hudson.model.BuildListener;
import hudson.model.Descriptor;
import hudson.model.Executor;
import hudson.model.Queue;
import hudson.plugins.build_timeout.BuildTimeOutStrategy;
import hudson.plugins.build_timeout.BuildTimeOutStrategyDescriptor;
import org.jenkinsci.plugins.tokenmacro.MacroEvaluationException;
import org.kohsuke.stapler.DataBoundConstructor;

import edu.umd.cs.findbugs.annotations.NonNull;
import java.io.IOException;
import java.util.StringJoiner;
import java.util.concurrent.TimeUnit;

/**
 * Get the time considered it stuck.
 *
 * Return 10 times as much as eta if eta is available, else 24 hours.
 * @see Executor#isLikelyStuck()
 */
public class LikelyStuckTimeOutStrategy extends BuildTimeOutStrategy {


    @DataBoundConstructor
    public LikelyStuckTimeOutStrategy() {
    }

    @Override
    public long getTimeOut(@NonNull AbstractBuild<?, ?> run, @NonNull BuildListener listener)
            throws InterruptedException, MacroEvaluationException, IOException {
        Executor executor = run.getExecutor();
        if (executor == null) {
            return TimeUnit.HOURS.toMillis(24);
        }

        Queue.Executable executable = executor.getCurrentExecutable();
        if (executable == null) {
            return TimeUnit.HOURS.toMillis(24);
        }

        long eta = executable.getEstimatedDuration();
        if (eta >= 0) {
            return eta * 10;
        } else {
            return TimeUnit.HOURS.toMillis(24);
        }
    }

    @Override
    public String toString() {
        return new StringJoiner(", ", LikelyStuckTimeOutStrategy.class.getSimpleName() + "[", "]")
                .add("preferred='10 x estimated duration'")
                .add("fallback='24 hours'")
                .toString();
    }

    public Descriptor<BuildTimeOutStrategy> getDescriptor() {
        return DESCRIPTOR;
    }

    @Extension
    public static final DescriptorImpl DESCRIPTOR = new DescriptorImpl();

    public static class DescriptorImpl extends BuildTimeOutStrategyDescriptor {

        @Override
        public String getDisplayName() {
            return Messages.LikelyStuckTimeOutStrategy_DisplayName();
        }
    }
}
