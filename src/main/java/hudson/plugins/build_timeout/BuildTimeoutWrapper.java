package hudson.plugins.build_timeout;

import hudson.tasks.BuildWrapper;
import hudson.model.Descriptor;
import hudson.model.Build;
import hudson.model.BuildListener;
import hudson.model.Executor;
import hudson.Launcher;
import hudson.triggers.Trigger;
import hudson.triggers.SafeTimerTask;
import org.kohsuke.stapler.StaplerRequest;

import java.io.IOException;
import java.util.TimerTask;

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


    public Environment setUp(final Build build, Launcher launcher, final BuildListener listener) throws IOException {
        class EnvironmentImpl extends Environment {
            private final TimerTask task;

            public EnvironmentImpl() {
                task = new SafeTimerTask() {
                    public void doRun() {
                        // timed out
                        listener.getLogger().println("Build timed out. Aborting");
                        Executor e = build.getExecutor();
                        if (e != null)
                            e.interrupt();
                    }
                };
                Trigger.timer.schedule(task, timeoutMinutes*60L*1000L );
            }

            public boolean tearDown(Build build, BuildListener listener) throws IOException {
                task.cancel();
                return true;
            }
        }

        return new EnvironmentImpl();
    }

    public Descriptor<BuildWrapper> getDescriptor() {
        return DESCRIPTOR;
    }

    public static final DescriptorImpl DESCRIPTOR = new DescriptorImpl();

    public static final class DescriptorImpl extends Descriptor<BuildWrapper> {
        DescriptorImpl() {
            super(BuildTimeoutWrapper.class);
        }

        public String getDisplayName() {
            return "Abort the build if it's stuck";
        }

        public BuildTimeoutWrapper newInstance(StaplerRequest req) throws FormException {
            BuildTimeoutWrapper w = new BuildTimeoutWrapper();
            req.bindParameters(w,"build-timeout.");
            return w;
        }
    }
}
