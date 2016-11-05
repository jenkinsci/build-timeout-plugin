package hudson.plugins.build_timeout;

import hudson.Extension;
import hudson.Launcher;
import hudson.model.AbstractBuild;
import hudson.model.AbstractProject;
import hudson.model.BuildListener;
import hudson.tasks.BuildStep;
import hudson.tasks.BuildStepDescriptor;
import hudson.tasks.Builder;

import java.io.IOException;

public class FakeBuildStep extends Builder implements BuildStep {
    private long delay = 0;
    
    public FakeBuildStep(long delay) {
        this.delay = delay;
    }

    @Override
    public boolean perform(AbstractBuild<?, ?> build, Launcher launcher, BuildListener listener) throws InterruptedException, IOException {
        Thread.sleep(delay);

        listener.getLogger().print("Test");

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
