package hudson.plugins.build_timeout;

import hudson.Extension;
import hudson.Launcher;
import hudson.model.AbstractBuild;
import hudson.model.AbstractProject;
import hudson.model.BuildListener;
import hudson.tasks.BuildStep;
import hudson.tasks.BuildStepDescriptor;
import hudson.tasks.Builder;

public class FakeBuildStep extends Builder implements BuildStep {
    static final String FAKE_BUILD_STEP_OUTPUT = "fake-build-step-output";
    private long delay = 0;

    FakeBuildStep(long delay) {
        this.delay = delay;
    }

    @Override
    public boolean perform(AbstractBuild<?, ?> build, Launcher launcher, BuildListener listener) throws InterruptedException {
        Thread.sleep(delay);

        listener.getLogger().print(FAKE_BUILD_STEP_OUTPUT);

        return true;
    }

    @Extension
    public static class DescriptorImpl extends BuildStepDescriptor<Builder> {

        public boolean isApplicable(Class<? extends AbstractProject> jobType) {
            return false;
        }

        public String getDisplayName() {
            return null;
        }
    }
}
