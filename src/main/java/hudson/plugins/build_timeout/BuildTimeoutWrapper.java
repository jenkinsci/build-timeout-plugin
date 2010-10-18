package hudson.plugins.build_timeout;

import static hudson.util.TimeUnit2.MILLISECONDS;
import static hudson.util.TimeUnit2.MINUTES;
import hudson.Extension;
import hudson.Launcher;
import hudson.model.AbstractBuild;
import hudson.model.AbstractProject;
import hudson.model.BuildListener;
import hudson.model.Descriptor;
import hudson.model.Executor;
import hudson.model.Result;
import hudson.model.Run;
import hudson.tasks.BuildWrapper;
import hudson.tasks.BuildWrapperDescriptor;
import hudson.triggers.SafeTimerTask;
import hudson.triggers.Trigger;
import hudson.util.TimeUnit2;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import net.sf.json.JSONObject;

import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.StaplerRequest;

/**
 * {@link BuildWrapper} that terminates a build if it's taking too long.
 *
 * @author Kohsuke Kawaguchi
 */
public class BuildTimeoutWrapper extends BuildWrapper {
    
    protected static final int NUMBER_OF_BUILDS_TO_AVERAGE = 3;
    public static long MINIMUM_TIMEOUT_MILLISECONDS = Long.getLong(BuildTimeoutWrapper.class.getName()+ ".MINIMUM_TIMEOUT_MILLISECONDS", 3 * 60 * 1000);

    
    public static final String ABSOLUTE = "absolute";
    public static final String ELASTIC = "elastic";
    /**
     * If the build took longer than this amount of minutes,
     * it will be terminated.
     */
    public int timeoutMinutes;

    /**
     * Fail the build rather than aborting it
     */
    public boolean failBuild;

    /**
     * The percentage of the mean of the duration of the last n successful builds
     * to wait before killing the build.
     * 
     * IE, if the last n successful builds averaged a 10 minute duration,
     * then 200% of that would be 20 minutes.
     */
    public int timeoutPercentage;
    
    /**
     * Values can be "elastic" or "absolute"
     */
    public String timeoutType;
    
    /**
     * The timeout to use if there are no valid builds in the build 
     * history (ie, no successful or unstable builds)
     */
    public int timeoutMinutesElasticDefault;
    
    @DataBoundConstructor
    public BuildTimeoutWrapper(int timeoutMinutes, boolean failBuild, int timeoutPercentage, int timeoutMinutesElasticDefault, String timeoutType) {
        this.timeoutPercentage = timeoutPercentage;
        this.timeoutMinutes = Math.max(3,timeoutMinutes);
        this.timeoutMinutesElasticDefault = Math.max(3, timeoutMinutesElasticDefault);
        this.failBuild = failBuild;
        this.timeoutType = timeoutType;
    }
    
    @Override
    public Environment setUp(final AbstractBuild build, Launcher launcher, final BuildListener listener) {
        class EnvironmentImpl extends Environment {
            final class TimeoutTimerTask extends SafeTimerTask {
                private final AbstractBuild build;
                private final BuildListener listener;
                //Did the task timeout?
                public boolean timeout= false;

                private TimeoutTimerTask(AbstractBuild build, BuildListener listener) {
                    this.build = build;
                    this.listener = listener;
                }

                public void doRun() {
                    // timed out
                    long effectiveTimeoutMinutes = MINUTES.convert(effectiveTimeout,MILLISECONDS);
                    if (failBuild) {
                        listener.getLogger().println("Build timed out (after " + effectiveTimeoutMinutes + " minutes). Marking the build as failed.");
                    } else
                        listener.getLogger().println("Build timed out (after " + effectiveTimeoutMinutes + " minutes). Marking the build as aborted.");
                    timeout=true;
                    Executor e = build.getExecutor();
                    if (e != null)
                        e.interrupt();
                }
            }

            private final TimeoutTimerTask task;
            
            private final long effectiveTimeout;
            
            public EnvironmentImpl() {
                task = new TimeoutTimerTask(build, listener);
                this.effectiveTimeout = getEffectiveTimeout(timeoutMinutes * 60*1000, timeoutPercentage, timeoutMinutesElasticDefault, timeoutType, build.getProject().getBuilds()); 
                Trigger.timer.schedule(task, effectiveTimeout);
            }

            @Override
            public boolean tearDown(AbstractBuild build, BuildListener listener) throws IOException, InterruptedException {
                task.cancel();
                return (!task.timeout ||!failBuild);
            }
        }

        return new EnvironmentImpl();
    }

    public static long getEffectiveTimeout(int timeoutMilliseconds, int timeoutPercentage, int timeoutMillsecondsElasticDefault,
            String timeoutType, List<Run> builds) {
        
        if (ELASTIC.equals(timeoutType)) {
            double elasticTimeout = getElasticTimeout(timeoutPercentage, builds);
            if (elasticTimeout == 0) {
                return Math.max(MINIMUM_TIMEOUT_MILLISECONDS, timeoutMillsecondsElasticDefault);
            } else {
                return (long) Math.max(MINIMUM_TIMEOUT_MILLISECONDS, elasticTimeout);    
            }
        } else {
            return (long) Math.max(MINIMUM_TIMEOUT_MILLISECONDS, timeoutMilliseconds);    
        }
    }
    
    private static double getElasticTimeout(int timeoutPercentage, List<Run> builds) {
        return timeoutPercentage * .01D * (timeoutPercentage > 0 ? averageDuration(builds) : 0);
    }

    private static double averageDuration(List <Run> builds) {
        int nonFailingBuilds = 0;
        int durationSum= 0;
        
        for (int i = builds.size() - 1; i >= 0 && nonFailingBuilds < NUMBER_OF_BUILDS_TO_AVERAGE; i--) {
            Run run = builds.get(i);
            if (run.getResult() != null && 
                    run.getResult().isBetterOrEqualTo(Result.UNSTABLE)) {
                durationSum += run.getDuration();
                nonFailingBuilds++;
            }
        }
        
        return nonFailingBuilds > 0 ? durationSum / nonFailingBuilds : 0;
    }

    protected Object readResolve() {
        if (timeoutType == null)  {
            timeoutType = ABSOLUTE;
        }
        return this;
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

        public int[] getPercentages() {
            return new int[] {150,200,250,300,350,400};
        }
        
        @Override
        public BuildWrapper newInstance(StaplerRequest req, JSONObject formData)
                throws hudson.model.Descriptor.FormException {
            formData.put("timeoutType", formData.getJSONObject("timeoutType").getString("value"));
            return super.newInstance(req, formData);
        }
        
    }
}
