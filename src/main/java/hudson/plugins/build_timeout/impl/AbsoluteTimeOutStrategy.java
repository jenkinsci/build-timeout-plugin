package hudson.plugins.build_timeout.impl;

import static hudson.plugins.build_timeout.BuildTimeoutWrapper.MINIMUM_TIMEOUT_MILLISECONDS;

import hudson.Extension;
import hudson.model.AbstractBuild;
import hudson.model.BuildListener;
import hudson.model.Descriptor;
import hudson.plugins.build_timeout.BuildTimeOutStrategy;
import hudson.plugins.build_timeout.BuildTimeOutStrategyDescriptor;
import org.jenkinsci.plugins.tokenmacro.MacroEvaluationException;
import org.jenkinsci.plugins.tokenmacro.TokenMacro;
import org.kohsuke.stapler.DataBoundConstructor;

import java.io.IOException;

/**
 * If the build took longer than <tt>timeoutMinutes</tt> amount of minutes, it will be terminated.
 */
public class AbsoluteTimeOutStrategy extends BuildTimeOutStrategy {

    public final String timeoutMinutes;

    @Deprecated
    public AbsoluteTimeOutStrategy(int timeoutMinutes) {
        this.timeoutMinutes = Integer.toString(Math.max((int) (MINIMUM_TIMEOUT_MILLISECONDS / MINUTES), timeoutMinutes));
    }

    @DataBoundConstructor
    public AbsoluteTimeOutStrategy(String timeoutMinutes) {
        this.timeoutMinutes = timeoutMinutes;
    }

    @Override
    public long getTimeOut(AbstractBuild<?,?> build, BuildListener listener)
            throws InterruptedException, MacroEvaluationException, IOException {
        return MINUTES * Math.max((int) (MINIMUM_TIMEOUT_MILLISECONDS / MINUTES), Integer.parseInt(
                expandAll(build, listener, this.timeoutMinutes)));
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
