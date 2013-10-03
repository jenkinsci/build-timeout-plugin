package hudson.plugins.build_timeout;

import hudson.Extension;
import hudson.Launcher;
import hudson.model.AbstractBuild;
import hudson.model.AbstractProject;
import hudson.model.BuildListener;
import hudson.model.Descriptor;
import hudson.model.Executor;
import hudson.model.Queue;
import hudson.model.Result;
import hudson.model.Run;
import hudson.model.queue.Executables;
import hudson.tasks.BuildWrapper;
import hudson.tasks.BuildWrapperDescriptor;
import hudson.triggers.SafeTimerTask;
import hudson.triggers.Trigger;
import hudson.util.FormValidation;
import hudson.util.ListBoxModel;
import hudson.util.TimeUnit2;
import static hudson.util.TimeUnit2.MILLISECONDS;
import static hudson.util.TimeUnit2.MINUTES;
import java.io.IOException;
import java.util.List;
import java.util.Set;
import net.sf.json.JSONObject;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.QueryParameter;
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
    public static final String STUCK = "likelyStuck";
    public static final int ELASTIC_MIN_PERCENTAGE=150;
    public static final int ELASTIC_MAX_PERCENTAGE=800;
    
    
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
     * Writing the build description when timeout occurred.
     */
    public boolean writingDescription;

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
    public Integer timeoutMinutesElasticDefault;
    
    @DataBoundConstructor
    public BuildTimeoutWrapper(int timeoutMinutes, boolean failBuild, boolean writingDescription,
                               int timeoutPercentage, int timeoutMinutesElasticDefault, String timeoutType) {
        this.timeoutMinutes = Math.max(3,timeoutMinutes);
        this.failBuild = failBuild;
        this.writingDescription = writingDescription;
        this.timeoutPercentage = timeoutPercentage;
        this.timeoutMinutesElasticDefault = Math.max(3, timeoutMinutesElasticDefault);
        this.timeoutType = timeoutType;
    }
    
    @Override
    public Environment setUp(final AbstractBuild build, Launcher launcher, final BuildListener listener) throws IOException, InterruptedException {
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
                    String msg;
                    if (failBuild) {
                        msg = Messages.Timeout_Message(effectiveTimeoutMinutes, Messages.Timeout_Failed());
                    } else {
                        msg = Messages.Timeout_Message(effectiveTimeoutMinutes, Messages.Timeout_Aborted());
                    }

                    listener.getLogger().println(msg);
                    if (writingDescription) {
                        try {
                            build.setDescription(msg);
                        } catch (IOException e) {
                            listener.getLogger().println("failed to write to the build description!");
                        }
                    }

                    timeout=true;
                    Executor e = build.getExecutor();
                    if (e != null)
                        e.interrupt(failBuild? Result.FAILURE : Result.ABORTED);
                }
            }

            private final TimeoutTimerTask task;
            
            private final long effectiveTimeout;
            
            public EnvironmentImpl() {
                long timeout;
                if (ELASTIC.equals(timeoutType)) {
                    timeout = getEffectiveTimeout(timeoutMinutes * 60L * 1000L, timeoutPercentage,
                            timeoutMinutesElasticDefault * 60*1000, timeoutType, build.getProject().getBuilds());
                } else if (STUCK.equals(timeoutType)) {
                    timeout = getLikelyStuckTime();
                } else {
                    timeout = timeoutMinutes * 60L * 1000L;
                }

                this.effectiveTimeout = timeout;
                task = new TimeoutTimerTask(build, listener);
                Trigger.timer.schedule(task, timeout);
            }

            /**
             * Get the time considered it stuck.
             * 
             * @return 10 times as much as eta if eta is available, else 24 hours.
             * @see Executor#isLikelyStuck()
             */
            private long getLikelyStuckTime() {
                Executor executor = build.getExecutor();
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

            @Override
            public boolean tearDown(AbstractBuild build, BuildListener listener) throws IOException, InterruptedException {
                task.cancel();
                return (!task.timeout ||!failBuild);
            }
        }

        return new EnvironmentImpl();
    }

    public static long getEffectiveTimeout(long timeoutMilliseconds, int timeoutPercentage, int timeoutMillsecondsElasticDefault,
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
            JSONObject timeoutObject = formData.getJSONObject("timeoutType");
            
            // Validate timeout percentage
            if (timeoutObject.containsKey("timeoutPercentage")) {
                int percentage = timeoutObject.optInt("timeoutPercentage", -1);
                if (percentage == -1) {
                    throw new FormException("Cannot convert value to integer", "timeoutPercentage");
                }
                if (percentage<ELASTIC_MIN_PERCENTAGE || percentage>ELASTIC_MAX_PERCENTAGE) {
                    throw new FormException(Messages.Timeout_PercentageOutOfBounds(percentage, ELASTIC_MIN_PERCENTAGE, ELASTIC_MAX_PERCENTAGE), "timeoutPercentage");
                }
            }
            
            // we would ideally do this on the form itself (to show the default)
            //but there is a show/hide bug when using radioOptions inside an optionBlock
            if (timeoutObject.isNullObject() || timeoutObject.isEmpty()) {
                formData.put("timeoutType", ABSOLUTE);
            } else {
                // Jenkins 1.427
                // {"timeoutType": {
                //   "value": "elastic", "timeoutPercentage": "150", 
                //   "timeoutMinutesElasticDefault": "3333333"}}
                // Jenkins 1.420
                // {"timeoutMinutes": "3", 
                //  "timeoutType": {"value": "elastic"}, 
                //  "timeoutPercentage": "150", "timeoutMinutesElasticDefault": "3333333", 
                // "failBuild": false, "writingDescription": false}
                // => to keep comaptibility
                // "timeoutType": "elastic",  "timeoutPercentage": "150", 
                // "timeoutMinutesElasticDefault": "3333333"... 
                String timeoutType = timeoutObject.getString("value");
                timeoutObject.remove("value");
                for (String key : (Set<String>) timeoutObject.keySet()) {
                    formData.put(key, timeoutObject.get(key));
                }
                formData.put("timeoutType", timeoutType);
            }        

            return super.newInstance(req, formData);
        }

        public FormValidation doCheckTimeoutPercentage(@QueryParameter String value) {
            int percentage;
            try {
                percentage = Integer.parseInt(value);
            } catch (NumberFormatException ex) {
                return FormValidation.error(ex.getMessage());
            }
            if (percentage<ELASTIC_MIN_PERCENTAGE || percentage>ELASTIC_MAX_PERCENTAGE) {
                return FormValidation.error(Messages.Timeout_PercentageOutOfBounds(percentage, ELASTIC_MIN_PERCENTAGE, ELASTIC_MAX_PERCENTAGE));
            }
            return FormValidation.ok();
        }
    }
}
