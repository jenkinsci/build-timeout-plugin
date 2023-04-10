package hudson.plugins.build_timeout.impl;

import hudson.Extension;
import hudson.model.AbstractBuild;
import hudson.model.BuildListener;
import hudson.model.Descriptor;
import hudson.plugins.build_timeout.BuildTimeOutStrategy;
import hudson.plugins.build_timeout.BuildTimeOutStrategyDescriptor;
import hudson.plugins.build_timeout.BuildTimeoutWrapper;
import org.jenkinsci.plugins.tokenmacro.MacroEvaluationException;
import org.kohsuke.stapler.DataBoundConstructor;

import edu.umd.cs.findbugs.annotations.NonNull;
import java.io.IOException;
import java.util.StringJoiner;

/**
 * If the build took longer than {@code timeoutMinutes} amount of minutes, it will be terminated.
 */
public class AbsoluteTimeOutStrategy extends BuildTimeOutStrategy {

    private final String timeoutMinutes;

    /**
     * @return minutes to timeout.
     */
    public String getTimeoutMinutes() {
        return timeoutMinutes;
    }

    @Deprecated
    public AbsoluteTimeOutStrategy(int timeoutMinutes) {
        this.timeoutMinutes = Integer.toString(Math.max((int) (BuildTimeoutWrapper.MINIMUM_TIMEOUT_MILLISECONDS / MINUTES), timeoutMinutes));
    }

    @DataBoundConstructor
    public AbsoluteTimeOutStrategy(String timeoutMinutes) {
        this.timeoutMinutes = timeoutMinutes;
    }

    @Override
    public long getTimeOut(@NonNull AbstractBuild<?,?> build, @NonNull BuildListener listener)
            throws InterruptedException, MacroEvaluationException, IOException {
        return MINUTES * Math.max((int) (BuildTimeoutWrapper.MINIMUM_TIMEOUT_MILLISECONDS / MINUTES), Integer.parseInt(
                expandAll(build, listener, getTimeoutMinutes())));
    }

    @Override
    public Descriptor<BuildTimeOutStrategy> getDescriptor() {
        return DESCRIPTOR;
    }

    @Override
    public String toString() {
        return new StringJoiner(", ", AbsoluteTimeOutStrategy.class.getSimpleName() + "[", "]")
                .add("timeoutMinutes='" + timeoutMinutes + "'")
                .toString();
    }

    @Extension(ordinal=100) // This is displayed at the top as the default
    public static final DescriptorImpl DESCRIPTOR = new DescriptorImpl();

    public static class DescriptorImpl extends BuildTimeOutStrategyDescriptor {

        @Override
        public String getDisplayName() {
            return Messages.AbsoluteTimeOutStrategy_DisplayName();
        }

        @Override
        public boolean isApplicableAsBuildStep() {
            return true;
        }
    }
}
