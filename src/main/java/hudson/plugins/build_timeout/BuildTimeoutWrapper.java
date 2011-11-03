package hudson.plugins.build_timeout;

import hudson.Extension;
import hudson.Launcher;
import hudson.model.AbstractBuild;
import hudson.model.AbstractProject;
import hudson.model.BuildListener;
import hudson.model.Descriptor;
import hudson.model.Executor;
import hudson.model.Hudson;
import hudson.model.Result;
import hudson.tasks.BuildWrapper;
import hudson.tasks.BuildWrapperDescriptor;
import hudson.triggers.SafeTimerTask;
import hudson.triggers.Trigger;

import java.io.IOException;

import org.kohsuke.stapler.DataBoundConstructor;

/**
 * {@link BuildWrapper} that terminates a build if it's taking too long.
 *
 * @author Kohsuke Kawaguchi
 */
public class BuildTimeoutWrapper extends BuildWrapper {
    /**
     * If the build took longer than this amount of minutes,
     * it will be terminated.
     */
    public int timeoutMinutes;

    /**
     * Fail the build rather than aborting it
     */
    public boolean failBuild;
    
    @DataBoundConstructor
    public BuildTimeoutWrapper(int timeoutMinutes, boolean failBuild) {
        this.timeoutMinutes = Math.max(3,timeoutMinutes);
        this.failBuild = failBuild;
    }
    
    @Override
    public Environment setUp(final AbstractBuild build, final Launcher launcher, final BuildListener listener) throws IOException, InterruptedException {
        class EnvironmentImpl extends Environment {
            final class TimeoutTimerTask extends SafeTimerTask {
                private final AbstractBuild build;
                private final BuildListener listener;
                private final Launcher launcher;
                //Did the task timeout?
                public boolean timeout= false;

                private TimeoutTimerTask(AbstractBuild build, BuildListener listener, Launcher launcher) {
                    this.build = build;
                    this.listener = listener;
                    this.launcher = launcher;
                }

                public void doRun() {
                    // timed out
                    if (failBuild)
                        listener.getLogger().println("Build timed out (after " + timeoutMinutes + " minutes). Marking the build as failed.");
                    else
                        listener.getLogger().println("Build timed out (after " + timeoutMinutes + " minutes). Marking the build as aborted.");
                    timeout=true;
                    Executor ex = build.getExecutor();
                    if (ex != null) {
                        ex.interrupt(failBuild? Result.FAILURE : Result.ABORTED);

                        if (Hudson.getInstance().getPlugin("schedule-failed-builds") != null) {
                            com.progress.hudson.ScheduleFailedBuildsPublisher retryFailedBuild =
                                (com.progress.hudson.ScheduleFailedBuildsPublisher) build.getProject().getPublishersList().get(com.progress.hudson.ScheduleFailedBuildsPublisher.class);
                            try {
                                retryFailedBuild.perform(build, launcher, listener);
                            } catch (InterruptedException e) {
                                listener.getLogger().println("failed to re-scehdule failed build " + e.getMessage());
                            } catch (IOException e) {
                                listener.getLogger().println("failed to re-scehdule failed build " + e.getMessage());
                            }
                        }
                    }
                }
            }

            private final TimeoutTimerTask task;
            
            public EnvironmentImpl() {
                task = new TimeoutTimerTask(build, listener, launcher);
                Trigger.timer.schedule(task, timeoutMinutes*60L*1000L );
            }

            @Override
            public boolean tearDown(AbstractBuild build, BuildListener listener) throws IOException, InterruptedException {
                task.cancel();
                return (!task.timeout ||!failBuild);
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
}
