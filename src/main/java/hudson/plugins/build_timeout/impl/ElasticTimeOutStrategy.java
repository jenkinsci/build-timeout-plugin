package hudson.plugins.build_timeout.impl;

import static hudson.plugins.build_timeout.BuildTimeoutWrapper.MINIMUM_TIMEOUT_MILLISECONDS;

import hudson.Extension;
import hudson.model.Descriptor;
import hudson.model.Result;
import hudson.model.Run;
import hudson.plugins.build_timeout.BuildTimeOutStrategy;
import hudson.plugins.build_timeout.BuildTimeOutStrategyDescriptor;
import hudson.util.ListBoxModel;
import org.kohsuke.stapler.DataBoundConstructor;

public class ElasticTimeOutStrategy extends BuildTimeOutStrategy {

    public final int timeoutPercentage;

    public final int numberOfBuilds;

    /**
     * The timeout to use if there are no valid builds in the build
     * history (ie, no successful or unstable builds)
     */
    public final int timeoutMinutesElasticDefault;


    @DataBoundConstructor
    public ElasticTimeOutStrategy(int timeoutPercentage, int timeoutMinutesElasticDefault, int numberOfBuilds) {
        this.timeoutPercentage = timeoutPercentage;
        this.timeoutMinutesElasticDefault = timeoutMinutesElasticDefault;
        this.numberOfBuilds = numberOfBuilds;
    }

    @Override
    public long getTimeOut(Run run) {
        double elasticTimeout = getElasticTimeout(timeoutPercentage, run);
        if (elasticTimeout == 0) {
            return Math.max(MINIMUM_TIMEOUT_MILLISECONDS, timeoutMinutesElasticDefault * MINUTES);
        } else {
            return (long) Math.max(MINIMUM_TIMEOUT_MILLISECONDS, elasticTimeout);
        }
    }

    private double getElasticTimeout(int timeoutPercentage, Run build) {
        return timeoutPercentage * .01D * (timeoutPercentage > 0 ? averageDuration(build) : 0);
    }

    private double averageDuration(Run run) {
        int nonFailingBuilds = 0;
        int durationSum = 0;

        while(run.getPreviousBuild() != null && nonFailingBuilds < numberOfBuilds) {
            run = run.getPreviousBuild();
            if (run.getResult() != null &&
                    run.getResult().isBetterOrEqualTo(Result.UNSTABLE)) {
                durationSum += run.getDuration();
                nonFailingBuilds++;
            }
        }


        return nonFailingBuilds > 0 ? durationSum / nonFailingBuilds : 0;
    }

    public Descriptor<BuildTimeOutStrategy> getDescriptor() {
        return DESCRIPTOR;
    }

    @Extension
    public static final DescriptorImpl DESCRIPTOR = new DescriptorImpl();

    public static class DescriptorImpl extends BuildTimeOutStrategyDescriptor {

        @Override
        public String getDisplayName() {
            return "Elastic";
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
