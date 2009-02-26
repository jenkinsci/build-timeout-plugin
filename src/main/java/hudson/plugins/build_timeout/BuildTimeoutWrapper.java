package hudson.plugins.build_timeout;

import hudson.Launcher;
import hudson.model.AbstractBuild;
import hudson.model.AbstractProject;
import hudson.model.BuildListener;
import hudson.model.Descriptor;
import hudson.model.Executor;
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
    public BuildTimeoutWrapper (int timeoutMinutes, boolean failBuild) {
        System.out.println ("Constructor called: "+timeoutMinutes+" "+failBuild);
        this.timeoutMinutes = Math.max(3,timeoutMinutes);
        this.failBuild = failBuild;
    }
    
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
                    listener.getLogger().println("Build timed out. Aborting");
                    timeout=true;
                    Executor e = build.getExecutor();
                    if (e != null)
                        e.interrupt();
                }
            }

            private final TimeoutTimerTask task;
            
            public EnvironmentImpl() {
                task = new TimeoutTimerTask(build, listener);
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

    
    public Descriptor<BuildWrapper> getDescriptor() {
        return DESCRIPTOR;
    }

    public static final DescriptorImpl DESCRIPTOR = new DescriptorImpl();

    public static final class DescriptorImpl extends BuildWrapperDescriptor {
        DescriptorImpl() {
            super(BuildTimeoutWrapper.class);
        }

        public String getDisplayName() {
            return "Abort the build if it's stuck";
        }

        public boolean isApplicable(AbstractProject<?, ?> item) {
            return true;
        }

    }
}
