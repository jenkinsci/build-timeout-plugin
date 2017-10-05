package hudson.plugins.build_timeout;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import hudson.Extension;
import hudson.Launcher;
import hudson.model.AbstractBuild;
import hudson.model.AbstractProject;
import hudson.model.Build;
import hudson.model.BuildListener;
import hudson.model.Descriptor;
import hudson.model.Run;
import hudson.model.TaskListener;
import hudson.plugins.build_timeout.operations.AbortOperation;
import hudson.tasks.BuildStep;
import hudson.tasks.BuildStepDescriptor;
import hudson.tasks.BuildStepMonitor;
import hudson.tasks.Builder;
import hudson.triggers.SafeTimerTask;
import hudson.triggers.Trigger;
import jenkins.model.Jenkins;
import net.sf.json.JSONObject;
import org.jenkinsci.plugins.tokenmacro.MacroEvaluationException;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.StaplerRequest;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;


public class BuildStepWithTimeout extends Builder implements BuildStep {
    private final BuildTimeOutStrategy strategy;
    private final BuildStep buildStep;
    private final List<BuildTimeOutOperation> operationList;

    @DataBoundConstructor
    public BuildStepWithTimeout(BuildStep buildStep, BuildTimeOutStrategy strategy, List<BuildTimeOutOperation> operationList) {
        this.strategy = strategy;
        this.buildStep = buildStep;
        if (operationList != null && !operationList.isEmpty()) {
            this.operationList = operationList;
        }
        else {
            this.operationList = Collections.emptyList();
        }

    }

    public List<BuildTimeOutOperation> getOperationList() {
        return operationList;
    }


    public BuildStep getBuildStep() {
        return buildStep;
    }

    private long getTimeout(Run run, TaskListener listener) throws IOException, InterruptedException {
        try {
            return strategy.getTimeOut((AbstractBuild<?, ?>) run, (BuildListener) listener);
        } catch (MacroEvaluationException e) {
            e.printStackTrace(listener.getLogger());
            listener.error("Can't evaluate timeout - timeout would be disabled");
            return Long.MAX_VALUE;
        }
    }

    @Override
    public boolean perform(AbstractBuild<?,?> build, Launcher launcher, BuildListener listener) throws InterruptedException, IOException {
        return perform((Build)build, launcher, listener);
    }

    @Override
    @SuppressFBWarnings(value = "NP_NULL_ON_SOME_PATH", justification = "No adequate replacement for Trigger.timer found")
    public boolean perform(final Build<?,?> build, final Launcher launcher, final BuildListener listener) throws InterruptedException, IOException {
        final Timer timer = Trigger.timer; // FIXME TODO replace with Timer
        final long delay = getTimeout(build, listener);

        final TimerTask task = new SafeTimerTask() {
            @Override
            public void doRun() {
                if (operationList.isEmpty()) {
                    new AbortOperation().perform(build, listener, delay);
                }

                for (BuildTimeOutOperation op : operationList) {
                    op.perform(build, listener, delay);
                }
            }
        };

        try {
            timer.schedule(task, delay);
            return buildStep.perform(build, launcher, listener);
        } finally {
            task.cancel();
        }
    }

    public BuildTimeOutStrategy getStrategy() {
        return strategy;
    }

    @Override
    public BuildStepMonitor getRequiredMonitorService() {
        return buildStep.getRequiredMonitorService();
    }

    @Extension
    public static class DescriptorImpl extends BuildStepDescriptor<Builder> {
        @Override
        public BuildStepWithTimeout newInstance(StaplerRequest req, JSONObject formData)
                throws hudson.model.Descriptor.FormException {
            BuildStep buildstep = BuildTimeOutUtility.bindJSONWithDescriptor(req, formData, "buildStep", BuildStep.class);
            BuildTimeOutStrategy strategy = BuildTimeOutUtility.bindJSONWithDescriptor(req, formData, "strategy", BuildTimeOutStrategy.class);
            List<BuildTimeOutOperation> operationList = newInstancesFromHeteroList(req, formData, "operationList", getOperations());
            return new BuildStepWithTimeout(buildstep, strategy, operationList);
        }

        @Override
        public String getDisplayName() {
            return Messages.BuildStepWithTimeout_DisplayName();
        }

        @Override
        public boolean isApplicable(Class<? extends AbstractProject> jobType) {
            return true;
        }

        public List<Descriptor<?>> getBuildStepWithTimeoutRunners() {
            List<Descriptor<?>> buildsteps = new ArrayList<Descriptor<?>>(Builder.all());
            buildsteps.remove(this);
            return buildsteps;
        }

        public List<BuildTimeOutStrategyDescriptor> getStrategies() {
            List<BuildTimeOutStrategyDescriptor> descriptors = Jenkins.getActiveInstance().getDescriptorList(BuildTimeOutStrategy.class);
            List<BuildTimeOutStrategyDescriptor> supportedStrategies = new ArrayList<BuildTimeOutStrategyDescriptor>(descriptors.size());

            for(BuildTimeOutStrategyDescriptor descriptor : descriptors) {
                if (descriptor.isApplicableAsBuildStep()) {
                    supportedStrategies.add(descriptor);
                }
            }

            return supportedStrategies;
        }

        @SuppressWarnings("unchecked")
        public List<BuildTimeOutOperationDescriptor> getOperations(AbstractProject<?,?> project) {
            return BuildTimeOutOperationDescriptor.all((Class<? extends AbstractProject<?, ?>>)project.getClass());
        }

        public List<BuildTimeOutOperationDescriptor> getOperations() {
            return BuildTimeOutOperationDescriptor.all();
        }
    }
}
