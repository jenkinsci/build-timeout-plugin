package hudson.plugins.build_timeout;

import hudson.Extension;
import hudson.Launcher;
import hudson.Util;
import hudson.model.AbstractBuild;
import hudson.model.AbstractProject;
import hudson.model.BuildListener;
import hudson.model.Descriptor;
import hudson.model.Run.RunnerAbortedException;
import hudson.plugins.build_timeout.impl.AbsoluteTimeOutStrategy;
import hudson.plugins.build_timeout.impl.ElasticTimeOutStrategy;
import hudson.plugins.build_timeout.impl.LikelyStuckTimeOutStrategy;
import hudson.plugins.build_timeout.operations.AbortOperation;
import hudson.plugins.build_timeout.operations.FailOperation;
import hudson.plugins.build_timeout.operations.WriteDescriptionOperation;
import hudson.tasks.BuildWrapper;
import hudson.tasks.BuildWrapperDescriptor;
import hudson.triggers.SafeTimerTask;
import hudson.triggers.Trigger;

import java.io.IOException;
import java.io.OutputStream;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import hudson.util.IOException2;
import jenkins.model.Jenkins;
import net.sf.json.JSONObject;

import org.jenkinsci.plugins.tokenmacro.MacroEvaluationException;
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
    private final String timeoutEnvVar;

    /**
     * Fail the build rather than aborting it
     * @deprecated use {@link FailOperation} instead.
     */
    @Deprecated
    public transient boolean failBuild;

    /**
     * Writing the build description when timeout occurred.
     * @deprecated use {@link WriteDescriptionOperation} instead.
     */
    @Deprecated
    public transient boolean writingDescription;
    
    private final List<BuildTimeOutOperation> operationList;
    
    /**
     * @return operations to perform at timeout.
     */
    public List<BuildTimeOutOperation> getOperationList() {
        return operationList;
    }
    
    private static List<BuildTimeOutOperation> createCompatibleOperationList(
            boolean failBuild, boolean writingDescription
    ) {
        BuildTimeOutOperation lastOp = (failBuild)?new FailOperation():new AbortOperation();
        if (!writingDescription) {
            return Arrays.asList(lastOp);
        }
        
        String msg;
        if (failBuild) {
            msg = Messages.Timeout_Message("{0}", Messages.Timeout_Failed());
        } else {
            msg = Messages.Timeout_Message("{0}", Messages.Timeout_Aborted());
        }
        BuildTimeOutOperation firstOp = new WriteDescriptionOperation(msg);
        return Arrays.asList(firstOp, lastOp);
    }
    
    @Deprecated
    public BuildTimeoutWrapper(BuildTimeOutStrategy strategy, boolean failBuild, boolean writingDescription) {
        this.strategy = strategy;
        this.operationList = createCompatibleOperationList(failBuild, writingDescription);
        this.timeoutEnvVar = null;
    }
    
    @Deprecated
    public BuildTimeoutWrapper(BuildTimeOutStrategy strategy, List<BuildTimeOutOperation> operationList) {
        this.strategy = strategy;
        this.operationList = (operationList != null)?operationList:Collections.<BuildTimeOutOperation>emptyList();
        this.timeoutEnvVar = null;
    }

    /**
     * ctor.
     * 
     * Don't forget to update {@link DescriptorImpl#newInstance(StaplerRequest, JSONObject)}
     * when you add new arguments.
     * 
     * @param strategy
     * @param operationList
     * @param timeoutEnvVar
     */
    @DataBoundConstructor
    public BuildTimeoutWrapper(BuildTimeOutStrategy strategy, List<BuildTimeOutOperation> operationList, String timeoutEnvVar) {
        this.strategy = strategy;
        this.operationList = (operationList != null)?operationList:Collections.<BuildTimeOutOperation>emptyList();
        this.timeoutEnvVar = Util.fixEmptyAndTrim(timeoutEnvVar);
    }
    
    public class EnvironmentImpl extends Environment {
            private final AbstractBuild<?,?> build;
            private final BuildListener listener;
            
            //Did some opertion failed?
            protected boolean operationFailed = false;
            
            final class TimeoutTimerTask extends SafeTimerTask {
                public void doRun() {
                    synchronized(EnvironmentImpl.this) {
                        EnvironmentImpl.this.task = null;   // mark timer is not active.
                    }
                    List<BuildTimeOutOperation> opList = getOperationList();
                    if (opList == null || opList.isEmpty()) {
                        // defaults to AbortOperation.
                        opList = Arrays.<BuildTimeOutOperation>asList(new AbortOperation());
                    }
                    for( BuildTimeOutOperation op: getOperationList() ) {
                        try {
                            if (!op.perform(build, listener, effectiveTimeout)) {
                                operationFailed = true;
                                break;
                            }
                        } catch(RuntimeException e) {
                            // if some unexpected exception,
                            // mark the operation failed and pass through the exception.
                            operationFailed = true;
                            throw e;
                        }
                    }
                }
            }

            private TimeoutTimerTask task = null;
            
            private final long effectiveTimeout;
            
            public EnvironmentImpl(AbstractBuild<?,?> build, BuildListener listener)
                    throws InterruptedException, MacroEvaluationException, IOException {
                this.build = build;
                this.listener = listener;
                this.effectiveTimeout = strategy.getTimeOut(build, listener);
                reschedule();
            }

            @Override
            public void buildEnvVars(Map<String, String> env) {
            	if (timeoutEnvVar != null) {
            		env.put(timeoutEnvVar, String.valueOf(effectiveTimeout));
           		}
            }

            public synchronized void reschedule() {
                if (task != null) {
                    task.cancel();
                }
                task = new TimeoutTimerTask();
                Trigger.timer.schedule(task, effectiveTimeout);
            }

            public synchronized void rescheduleIfScheduled() {
                if (task == null) {
                    return;
                }
                reschedule();
            }

            @Override
            public synchronized boolean tearDown(AbstractBuild build, BuildListener listener) throws IOException, InterruptedException {
                if (task != null) {
                    task.cancel();
                    task = null;
                }
                
                // true to continue build.
                return !operationFailed;
            }
    }

    @Override
    public Environment setUp(AbstractBuild build, Launcher launcher, BuildListener listener) throws IOException, InterruptedException {
        try {
            return new EnvironmentImpl(build, listener);
        } catch (MacroEvaluationException e) {
            e.printStackTrace(listener.fatalError("Could not evaluate macro"));
            throw new IOException2(e.getMessage(), e);
        }
    }

    protected Object readResolve() {
        if (strategy != null && getOperationList() != null) {
            // no need to upgrade
            return this;
        }
        
        if ("elastic".equalsIgnoreCase(timeoutType)) {
            strategy = new ElasticTimeOutStrategy(timeoutPercentage,
                    timeoutMinutesElasticDefault != null ? timeoutMinutesElasticDefault.intValue() : 60,
                    3);
        } else if ("likelyStuck".equalsIgnoreCase(timeoutType)) {
            strategy = new LikelyStuckTimeOutStrategy();
        } else if (strategy == null) {
            strategy = new AbsoluteTimeOutStrategy(timeoutMinutes);
        }
        
        List<BuildTimeOutOperation> opList = getOperationList();
        if (opList == null) {
            opList = createCompatibleOperationList(failBuild, writingDescription);
        }
        
        return new BuildTimeoutWrapper(strategy, opList, timeoutEnvVar);
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

        /**
         * create a new instance form user input.
         * 
         * Usually this is performed with {@link StaplerRequest#bindJSON(Class, JSONObject)},
         * but here it is required to construct object manually to call {@link Descriptor#newInstance(StaplerRequest, JSONObject)}
         * of downstream classes.
         * 
         * @param req
         * @param formData
         * @return
         * @throws hudson.model.Descriptor.FormException
         * @see hudson.model.Descriptor#newInstance(org.kohsuke.stapler.StaplerRequest, net.sf.json.JSONObject)
         */
        @Override
        public BuildTimeoutWrapper newInstance(StaplerRequest req, JSONObject formData)
                throws hudson.model.Descriptor.FormException {
            BuildTimeOutStrategy strategy = BuildTimeOutUtility.bindJSONWithDescriptor(req, formData, "strategy", BuildTimeOutStrategy.class);
            List<BuildTimeOutOperation> operationList = newInstancesFromHeteroList(req, formData, "operationList", getOperations());
            String timeoutEnvVar = formData.getString("timeoutEnvVar");
            return new BuildTimeoutWrapper(strategy, operationList, timeoutEnvVar);
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
        
        @SuppressWarnings("unchecked")
        public List<BuildTimeOutOperationDescriptor> getOperations(AbstractProject<?,?> project) {
            return BuildTimeOutOperationDescriptor.all((Class<? extends AbstractProject<?, ?>>)project.getClass());
        }
        
        public List<BuildTimeOutOperationDescriptor> getOperations() {
            return BuildTimeOutOperationDescriptor.all();
        }
    }

    public BuildTimeOutStrategy getStrategy() {
        return strategy;
    }

    public String getTimeoutEnvVar() {
        return timeoutEnvVar;
    }

    /**
     * @param build
     * @param logger
     * @return
     * @throws IOException
     * @throws InterruptedException
     * @throws RunnerAbortedException
     * @see hudson.tasks.BuildWrapper#decorateLogger(hudson.model.AbstractBuild, java.io.OutputStream)
     */
    @Override
    public OutputStream decorateLogger(@SuppressWarnings("rawtypes") final AbstractBuild build, final OutputStream logger)
            throws IOException, InterruptedException, RunnerAbortedException {
        if(!getStrategy().wantsCaptureLog()) {
            // For performance reason, decorates only when
            // the strategy requires that.
            return logger;
        }
        return new OutputStream() {
            @Override
            public void write(int b) throws IOException {
                getStrategy().onWrite(build, b);
                logger.write(b);
            }
            
            @Override
            public void close() throws IOException {
                logger.close();
                super.close();
            }
        };
    }

    // --- legacy attributes, kept for backward compatibility

    public transient int timeoutMinutes;

    public transient int timeoutPercentage;

    public transient String timeoutType;

    public transient Integer timeoutMinutesElasticDefault;
}
