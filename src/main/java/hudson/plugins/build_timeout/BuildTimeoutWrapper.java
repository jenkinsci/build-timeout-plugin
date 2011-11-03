package hudson.plugins.build_timeout;

import hudson.Extension;
import hudson.Launcher;
import hudson.model.BuildListener;
import hudson.model.Result;
import hudson.model.AbstractBuild;
import hudson.model.AbstractProject;
import hudson.model.Descriptor;
import hudson.model.Executor;
import hudson.model.Queue.Executable;
import hudson.model.queue.Executables;
import hudson.tasks.BuildWrapper;
import hudson.tasks.BuildWrapperDescriptor;
import hudson.triggers.SafeTimerTask;
import hudson.triggers.Trigger;
import hudson.util.TimeUnit2;

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
                    if (failBuild)
                        listener.getLogger().println("Build timed out (after " + timeoutMinutes + " minutes). Marking the build as failed.");
                    else
                        listener.getLogger().println("Build timed out (after " + timeoutMinutes + " minutes). Marking the build as aborted.");
                    timeout=true;
                    Executor e = build.getExecutor();
                    if (e != null)
                        e.interrupt(failBuild? Result.FAILURE : Result.ABORTED);
                }
            }

            private final TimeoutTimerTask task;
            

            public EnvironmentImpl() {
				long timeout;
				if (true) {
					timeout = getLikelyStuckTime();
				} else {
					timeout = timeoutMinutes * 60L * 1000L;
				}

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

				Executable executable = executor.getCurrentExecutable();
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
