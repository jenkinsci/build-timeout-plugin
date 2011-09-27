package hudson.plugins.build_timeout;

import hudson.Extension;
import hudson.Launcher;
import hudson.model.*;
import hudson.tasks.BuildWrapper;
import hudson.tasks.BuildWrapperDescriptor;
import hudson.triggers.SafeTimerTask;
import hudson.triggers.Trigger;
import org.kohsuke.stapler.DataBoundConstructor;

import java.io.IOException;

/**
 * {@link BuildWrapper}
 * that terminates a build if it's taking too long, in comparisson to previous builds
 *
 * @author gold.dudu@gmail.com [modified Kohsuke Kawaguchi's, build-timeout]
 */
public class BuildTimeoutWrapper extends BuildWrapper {

    private int delaySecondsForTest = 0;

    /**
     * If the build took longer than this % over the average calculated: timeout
     */
    public int thresholdPercentage = 30;

    private int timeoutSecondsCalculated;

    /**
     * Fail the build rather than aborting it
     */
    public boolean failBuild;

    /**
     * number of builds to calculate average on
     */
    public int buildsToCalculateAverage = 3;

    /**
     * hard coded timeout in seconds to override average timeout
     */
    public int timeoutSeconds = -1;

    @DataBoundConstructor
    public BuildTimeoutWrapper(int thresholdPercentage, int buildsToCalculateAverage, boolean failBuild, int timeoutSeconds) {
        /**
         * thresholdPercentage must be bigger than 10%
         */
        this.thresholdPercentage = Math.max(10, thresholdPercentage);
        /**
         * buildsToCalculateAverage must be bigger than 3
         */
        this.buildsToCalculateAverage = Math.max(3, buildsToCalculateAverage);

        this.failBuild = failBuild;

        this.timeoutSeconds = timeoutSeconds;
    }

    @Override
    public Environment setUp(final AbstractBuild build, Launcher launcher, final BuildListener listener) throws IOException, InterruptedException {
        class EnvironmentImpl extends Environment {
            final class TimeoutTimerTask extends SafeTimerTask {

                private final AbstractBuild build;
                private final BuildListener listener;
                //Did the task timeout?
                public boolean timeout = false;


                private TimeoutTimerTask(AbstractBuild build, BuildListener listener) {
                    this.build = build;
                    this.listener = listener;
                    int average = getPreviousBuildsTimeAverage(build);
                    int threshold = thresholdPercentage;
//                    System.out.println("Threshold % input = " + threshold);
//                    System.out.println("average = " + average);
                    if (average > 0){
                        listener.getLogger().println("Average builds time [secs] = " + average);
                        listener.getLogger().println("Threshold set = " + threshold + "% ==>" + (threshold*average / 100) + " seconds");
                    }
                    timeoutSecondsCalculated = average + (threshold*average / 100);
//                    System.out.println("timeout set to = " + timeoutSecondsCalculated);
                }

                public void doRun() {

                    String msg = "Build time exceeded ( " + timeoutSecondsCalculated + " seconds)," +
                            " that is the average of previous passed builds." +
                            " Marking the build as ";

                    // timed out
                    listener.getLogger().println(msg + (failBuild ? "failed." : "unstable."));

                    timeout = true;
                    Executor e = build.getExecutor();
                    if (e != null) {
                        if (failBuild) {
                            listener.getLogger().println("Stopping the build");
                            e.interrupt(Result.FAILURE);
                        } else {
                            listener.getLogger().println("Letting the build finish");
                            build.setResult(Result.UNSTABLE);
                        }
                    }
                }

                /**
                 * gets builds time [if not found return -1]
                 * @return time int
                 */
                private int getLastSuccessfulBuildTime(Run run) {

                    long timeMiliSecs = run.getDuration();
                    long timeSecs = (timeMiliSecs / 1000);
                    int buildTime = (int) timeSecs;
//                    System.out.println("run time [sec] = " + buildTime);
                    return buildTime;
                }

                /**
                 * gets 3 previous success builds time average
                 * @return time int
                 */
                public int getPreviousBuildsTimeAverage(AbstractBuild build) {
                    System.out.println("getting previous " + buildsToCalculateAverage + " builds time");
                    int lastThreeRunsTime = 0;
                    Run run = build;
                    for (int i = 0; i < buildsToCalculateAverage; i++) {
                        try {
                            run = run.getPreviousSuccessfulBuild();
                            run.getNumber();
//                            System.out.println("run number = " + run.getNumber());
                        } catch (Exception e) {
//                            System.out.println("Did not find successful build");
                            return -1;
                        }
                        lastThreeRunsTime += getLastSuccessfulBuildTime(run);
                    }
//                    System.out.println("total " + buildsToCalculateAverage + " builds = " + lastThreeRunsTime);
                    return lastThreeRunsTime > 0 ? lastThreeRunsTime / buildsToCalculateAverage : -1;
                }
            }

            private final TimeoutTimerTask task;

            public EnvironmentImpl() {
                task = new TimeoutTimerTask(build, listener);

                if (timeoutSecondsCalculated > 0 || timeoutSeconds > 0) {

                    int timeOutToUse = timeoutSecondsCalculated;

                    /**
                     * get the minimum valid timeout to enforce
                     */
                    if (timeoutSecondsCalculated > 0 && timeoutSeconds > 0)
                        timeOutToUse = Math.min(timeoutSeconds, timeoutSecondsCalculated);
                    else if (timeoutSeconds > 0)
                        timeOutToUse = timeoutSeconds;
                    listener.getLogger().println("hardcoded timeout [secs] = " + timeoutSeconds + "; Average build time + threshold [secs] = " + timeoutSecondsCalculated);
                    listener.getLogger().println("Setting timeout for " + timeOutToUse + " seconds");
                    Trigger.timer.schedule(task, timeOutToUse * 1000L);
                } else {
                    listener.getLogger().println("Was not able to find previous " + buildsToCalculateAverage + " success builds, and no hardcoded timeout given, so no timeout is enforced");
                }
                /**
                 * Test only
                 */
                if (delaySecondsForTest > 0) {
                    try {
                        System.out.println("sleep for " + delaySecondsForTest + " Secs ");
                        Thread.sleep(delaySecondsForTest * 1000);
                    } catch (InterruptedException e) {
                        //nothing to do
                    }
                }
            }

            @Override
            public boolean tearDown(AbstractBuild build, BuildListener listener) throws IOException, InterruptedException {
                task.cancel();
                return (!task.timeout || !failBuild);
            }
        }

        return new EnvironmentImpl();
    }


    @Override
    public Descriptor<BuildWrapper> getDescriptor() {
        return DESCRIPTOR;
    }

    @Extension
    public static final DescriptorImpl DESCRIPTOR = new DescriptorImpl();

    public static final class DescriptorImpl extends BuildWrapperDescriptor {
        DescriptorImpl() {
            super(BuildTimeoutWrapper.class);
        }

        public String getDisplayName() {
            return Messages.Descriptor_DisplayName();
        }

        public boolean isApplicable(AbstractProject<?, ?> item) {
            return true;
        }

    }

    public void setDelaySecondsForTest(int delaySecondsForTest) {
        this.delaySecondsForTest = delaySecondsForTest;
    }
}
