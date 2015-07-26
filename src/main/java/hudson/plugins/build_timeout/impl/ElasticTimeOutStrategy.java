package hudson.plugins.build_timeout.impl;

import static hudson.plugins.build_timeout.BuildTimeoutWrapper.MINIMUM_TIMEOUT_MILLISECONDS;

import hudson.Extension;
import hudson.model.AbstractBuild;
import hudson.model.BuildListener;
import hudson.model.Descriptor;
import hudson.model.Result;
import hudson.model.Run;
import hudson.plugins.build_timeout.BuildTimeOutStrategy;
import hudson.plugins.build_timeout.BuildTimeOutStrategyDescriptor;
import hudson.util.ListBoxModel;
import org.jenkinsci.plugins.tokenmacro.MacroEvaluationException;
import org.kohsuke.stapler.DataBoundConstructor;

import java.io.IOException;

public class ElasticTimeOutStrategy extends BuildTimeOutStrategy {

    private final String timeoutPercentage;

    private final String numberOfBuilds;

    /**
     * The timeout to use if there are no valid builds in the build
     * history (ie, no successful or unstable builds)
     */
    private final String timeoutMinutesElasticDefault;

    /**
     * @return how long percentage of the average duration to timeout.
     */
    public String getTimeoutPercentage() {
        return timeoutPercentage;
    }

    /**
     * @return the number of last builds to use to calculate average duration.
     */
    public String getNumberOfBuilds() {
        return numberOfBuilds;
    }

    /**
     * @return the default minutes to timeout used when failed to calculate average duration
     */
    public String getTimeoutMinutesElasticDefault() {
        return timeoutMinutesElasticDefault;
    }

    @Deprecated
    public ElasticTimeOutStrategy(int timeoutPercentage, int timeoutMinutesElasticDefault, int numberOfBuilds) {
        this.timeoutPercentage = Integer.toString(timeoutPercentage);
        this.timeoutMinutesElasticDefault = Integer.toString(timeoutMinutesElasticDefault);
        this.numberOfBuilds = Integer.toString(numberOfBuilds);
    }

    @DataBoundConstructor
    public ElasticTimeOutStrategy(String timeoutPercentage, String timeoutMinutesElasticDefault, String numberOfBuilds) {
        this.timeoutPercentage = timeoutPercentage;
        this.timeoutMinutesElasticDefault = timeoutMinutesElasticDefault;
        this.numberOfBuilds = numberOfBuilds;
    }

    @Override
    public long getTimeOut(AbstractBuild<?, ?> build, BuildListener listener)
            throws InterruptedException, MacroEvaluationException, IOException {
        double elasticTimeout = getElasticTimeout(Integer.parseInt(expandAll(build,listener,getTimeoutPercentage())), build, listener);
        if (elasticTimeout == 0) {
            return Math.max(MINIMUM_TIMEOUT_MILLISECONDS, Integer.parseInt(expandAll(build, listener, getTimeoutMinutesElasticDefault())) * MINUTES);
        } else {
            return (long) Math.max(MINIMUM_TIMEOUT_MILLISECONDS, elasticTimeout);
        }
    }

    private double getElasticTimeout(int timeoutPercentage, AbstractBuild<?, ?> build, BuildListener listener)
            throws InterruptedException, MacroEvaluationException, IOException {
        return timeoutPercentage * .01D * (timeoutPercentage > 0 ? averageDuration(build,listener) : 0);
    }

    private double averageDuration(AbstractBuild<?, ?> build, BuildListener listener)
            throws InterruptedException, MacroEvaluationException, IOException {
        int nonFailingBuilds = 0;
        int durationSum = 0;
        int numberOfBuilds = Integer.parseInt(expandAll(build, listener, getNumberOfBuilds()));

        while(build.getPreviousBuild() != null && nonFailingBuilds < numberOfBuilds) {
            build = build.getPreviousBuild();
            if (build.getResult() != null &&
                    build.getResult().isBetterOrEqualTo(Result.UNSTABLE)) {
                durationSum += build.getDuration();
                nonFailingBuilds++;
            }
        }


        return nonFailingBuilds > 0 ? ((double)durationSum) / nonFailingBuilds : 0;
    }

    public Descriptor<BuildTimeOutStrategy> getDescriptor() {
        return DESCRIPTOR;
    }

    @Extension
    public static final DescriptorImpl DESCRIPTOR = new DescriptorImpl();

    public static class DescriptorImpl extends BuildTimeOutStrategyDescriptor {

        @Override
        public String getDisplayName() {
            return Messages.ElasticTimeOutStrategy_DisplayName();
        }

        public int[] getPercentages() {
            return new int[] {150,200,250,300,350,400};
        }

        public ListBoxModel doFillTimeoutPercentageItems() {
            ListBoxModel m = new ListBoxModel();
            for (int option : getPercentages()) {
                String s = String.valueOf(option);
                m.add(s + "%", s);
            }
            return m;
        }

    }
}
