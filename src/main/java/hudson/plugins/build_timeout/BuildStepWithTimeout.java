package hudson.plugins.build_timeout;

import hudson.Extension;
import hudson.Launcher;
import hudson.model.*;
import hudson.plugins.build_timeout.impl.AbsoluteTimeOutStrategy;
import hudson.plugins.build_timeout.impl.DeadlineTimeOutStrategy;
import hudson.plugins.build_timeout.operations.AbortOperation;
import hudson.tasks.BuildStep;
import hudson.tasks.BuildStepDescriptor;
import hudson.tasks.BuildStepMonitor;
import hudson.tasks.Builder;
import jenkins.model.Jenkins;
import net.sf.json.JSONObject;
import org.jenkinsci.plugins.tokenmacro.MacroEvaluationException;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.StaplerRequest;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;


public class BuildStepWithTimeout extends Builder implements BuildStep {
    private /* final */ BuildTimeOutStrategy strategy;
    private BuildStep buildStep;
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

    public void setBuildStep(BuildStep buildStep) {
        this.buildStep = buildStep;
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


    public boolean perform(final Build<?,?> build, final Launcher launcher, final BuildListener listener) throws InterruptedException, IOException {
        final Timer timer = new Timer("Timer-" + build.getDisplayName().replace(" ", "_"), true);
        final AtomicBoolean stopped = new AtomicBoolean(false);
        try {
            final long delay = getTimeout(build, listener);

            timer.schedule(new TimerTask() {
                @Override
                public void run() {
                    if (operationList.isEmpty()) {
                        new AbortOperation().perform(build, listener, delay);
                    }

                    for (BuildTimeOutOperation op : operationList) {
                        op.perform(build, listener, delay);
                    }
                    stopped.set(true);
                    timer.cancel();
                }
            }, delay);

            return buildStep.perform((AbstractBuild) build, launcher, listener);
        } catch(InterruptedException e) {
            if (!stopped.get()) {
                throw e;
            }
        }

        return false;
    }

    public BuildTimeOutStrategy getStrategy() {
        return strategy;
    }

    @Override
    public BuildStepMonitor getRequiredMonitorService() {
        return BuildStepMonitor.NONE;
    }

    @Extension
    public static class DescriptorImpl extends BuildStepDescriptor<Builder> {
        public DescriptorImpl() {
            load();
        }

        @Override
        public BuildStepWithTimeout newInstance(StaplerRequest req, JSONObject formData)
                throws hudson.model.Descriptor.FormException {
            BuildStep buildstep = BuildTimeOutUtility.bindJSONWithDescriptor(req, formData, "buildStep", BuildStep.class);
            BuildTimeOutStrategy strategy = BuildTimeOutUtility.bindJSONWithDescriptor(req, formData, "strategy", BuildTimeOutStrategy.class);

            if (!(strategy instanceof AbsoluteTimeOutStrategy || strategy instanceof DeadlineTimeOutStrategy)) {
                throw new IllegalArgumentException("Unsupported strategy");
            }
            List<BuildTimeOutOperation> operationList = newInstancesFromHeteroList(req, formData, "operationList", getOperations());
            return new BuildStepWithTimeout(buildstep, strategy, operationList);
        }

        @Override
        public String getDisplayName() {
            return "Run with timeout";
        }

        public boolean isApplicable(Class<? extends AbstractProject> jobType) {
            return true;
        }

        public List<Descriptor<?>> getBuildStepRunners() {
            List<Descriptor<?>> buildsteps = new ArrayList<Descriptor<?>>(Builder.all());
            buildsteps.remove(this);
            return buildsteps;
        }

        public List<BuildTimeOutStrategyDescriptor> getStrategies() {
            return Jenkins.getInstance().getDescriptorList(BuildTimeOutStrategy.class);
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
