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
import hudson.plugins.build_timeout.impl.AbsoluteTimeOutStrategy;
import hudson.plugins.build_timeout.impl.ElasticTimeOutStrategy;
import hudson.plugins.build_timeout.impl.LikelyStuckTimeOutStrategy;
import hudson.tasks.BuildWrapper;
import hudson.tasks.BuildWrapperDescriptor;
import hudson.triggers.SafeTimerTask;
import hudson.triggers.Trigger;
import hudson.util.ListBoxModel;
import hudson.util.TimeUnit2;
import static hudson.util.TimeUnit2.MILLISECONDS;
import static hudson.util.TimeUnit2.MINUTES;
import java.io.IOException;
import java.util.List;
import java.util.Set;

import jenkins.model.Jenkins;
import net.sf.json.JSONObject;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.StaplerRequest;

/**
 * {@link BuildWrapper} that terminates a build if it's taking too long.
 *
 * @author Kohsuke Kawaguchi
 */
public class BuildTimeoutWrapper extends BuildWrapper {
    
    public static long MINIMUM_TIMEOUT_MILLISECONDS = Long.getLong(BuildTimeoutWrapper.class.getName()+ ".MINIMUM_TIMEOUT_MILLISECONDS", 3 * 60 * 1000);


    private /* final */ BuildTimeOutStrategy strategy;

    /**
     * Fail the build rather than aborting it
     */
    public boolean failBuild;

    /**
     * Writing the build description when timeout occurred.
     */
    public boolean writingDescription;

    
    @DataBoundConstructor
    public BuildTimeoutWrapper(BuildTimeOutStrategy strategy, boolean failBuild, boolean writingDescription) {
        this.strategy = strategy;
        this.failBuild = failBuild;
        this.writingDescription = writingDescription;
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
            
            private final long effectiveTimeout = strategy.getTimeOut(build);
            
            public EnvironmentImpl() {
                task = new TimeoutTimerTask(build, listener);
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

    protected Object readResolve() {
        if ("elastic".equalsIgnoreCase(timeoutType)) {
            strategy = new ElasticTimeOutStrategy(timeoutPercentage,
                    timeoutMinutesElasticDefault != null ? timeoutMinutesElasticDefault.intValue() : 60,
                    3);
        } else if ("likelyStuck".equalsIgnoreCase(timeoutType)) {
            strategy = new LikelyStuckTimeOutStrategy();
        } else if (strategy == null) {
            strategy = new AbsoluteTimeOutStrategy(timeoutMinutes);
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

        public List<BuildTimeOutStrategyDescriptor> getStrategies() {
            return Jenkins.getInstance().getDescriptorList(BuildTimeOutStrategy.class);
        }
    }

    public BuildTimeOutStrategy getStrategy() {
        return strategy;
    }

    // --- legacy attributes, kept for backward compatibility

    public transient int timeoutMinutes;

    public transient int timeoutPercentage;

    public transient String timeoutType;

    public transient Integer timeoutMinutesElasticDefault;
}
