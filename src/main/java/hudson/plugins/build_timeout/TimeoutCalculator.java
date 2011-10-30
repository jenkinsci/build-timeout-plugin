package hudson.plugins.build_timeout;

import hudson.model.AbstractBuild;
import hudson.model.BuildListener;
import hudson.model.Run;

/**
 * Created by
 * User: dudug
 * Date: 28/09/11
 * Time: 10:01
 * Calculate the timeout for  previous successful builds and adds the given threshold %
 */
public class TimeoutCalculator {

    AbstractBuild currentBuild;
    BuildListener listener;
    int buildsToCalculate;

    int thresholdPercentage;

    int hardTimeoutSeconds;

    public TimeoutCalculator(AbstractBuild build, BuildListener listener, int buildsToCalculate, int thresholdPercentage, int hardTimeoutSeconds) {

        this.buildsToCalculate = buildsToCalculate;
        this.listener = listener;
        this.currentBuild = build;
        this.thresholdPercentage = thresholdPercentage;
        this.hardTimeoutSeconds = hardTimeoutSeconds;

    }


    /**
     * get the calculated timeout of the average and hardcoded given timeouts
     * MIN(average of #previous, hardcoded)
     * or returns -1 for no timeout calculation
     *
     * @return
     */
    public int getCalculatedTimeoutSeconds() {

        int averageBuildTime = getPreviousBuildsTimeAverage();

        int thresholdInSeconds = 0;
        int timeoutSecondsCalculated = -1;
        if (averageBuildTime > 0) {
            thresholdInSeconds = thresholdPercentage * averageBuildTime / 100;
            timeoutSecondsCalculated = averageBuildTime + thresholdInSeconds;
            listener.getLogger().println("calculated average timeout : " + timeoutSecondsCalculated + " seconds");
        } else {
            listener.getLogger().println("no average timeout calculated");
        }

        if (hardTimeoutSeconds < 0) {
            listener.getLogger().println("no hardcoded timeout input found");
        } else {
            listener.getLogger().println("hardcoded timeout input is : " + hardTimeoutSeconds + " seconds");
        }

        int timeOutToUse = -1;

        if (timeoutSecondsCalculated > 0 || hardTimeoutSeconds > 0) {
            /**
             * get the minimum valid timeout to enforce
             */
            if (timeoutSecondsCalculated > 0 && hardTimeoutSeconds > 0)
                timeOutToUse = Math.min(hardTimeoutSeconds, timeoutSecondsCalculated);
            else
                timeOutToUse = (hardTimeoutSeconds > 0) ? hardTimeoutSeconds : timeoutSecondsCalculated;
        }
        return timeOutToUse;
    }


    /**
     * gets 3 previous success builds time average
     *
     * @return time int
     */

    private int getPreviousBuildsTimeAverage() {
        System.out.println("getting previous " + buildsToCalculate + " builds time");
        int lastRunsTime = 0;
        Run run = currentBuild;
        for (int i = 0; i < buildsToCalculate; i++) {
            run = run.getPreviousSuccessfulBuild();
            if (run == null)
                return -1;
            lastRunsTime += getLastSuccessfulBuildTime(run);
        }
        return lastRunsTime > 0 ? lastRunsTime / buildsToCalculate : -1;
    }

    /**
     * gets builds time [if not found return -1]
     *
     * @return time int
     */
    private int getLastSuccessfulBuildTime(Run run) {
        long timeMiliSecs = run.getDuration();
        return (int) (timeMiliSecs / 1000);
    }


}
