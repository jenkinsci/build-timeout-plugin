package hudson.plugins.build_timeout;

import java.io.IOException;

import hudson.Launcher;
import hudson.model.AbstractBuild;
import hudson.model.AbstractProject;
import hudson.model.BuildListener;
import hudson.model.Descriptor;
import hudson.model.FreeStyleBuild;
import hudson.model.FreeStyleProject;
import hudson.model.Result;
import hudson.model.Run;
import hudson.tasks.BuildStepDescriptor;
import hudson.tasks.BuildStepMonitor;
import hudson.tasks.Builder;
import hudson.tasks.Publisher;
import hudson.tasks.Recorder;

import org.jvnet.hudson.test.Bug;
import org.jvnet.hudson.test.HudsonTestCase;
import org.jvnet.hudson.test.TestExtension;
import org.jvnet.hudson.test.SleepBuilder;

public class BuildTimeoutWrapperIntegrationTest extends HudsonTestCase {
	@Bug(9203)
	public void testIssue9203() throws Exception {
        BuildTimeoutWrapper.MINIMUM_TIMEOUT_MILLISECONDS = 0;
		FreeStyleProject project = createFreeStyleProject();
		project.getBuildWrappersList().add(new BuildTimeoutWrapper(new QuickBuildTimeOutStrategy(), true, false));
        project.getBuildersList().add(new SleepBuilder(9999));
		FreeStyleBuild build = project.scheduleBuild2(0).get();
		assertBuildStatus(Result.FAILURE, build);
        System.err.println(getLog(build));
	}
    private static class QuickBuildTimeOutStrategy extends BuildTimeOutStrategy {
        @Override public long getTimeOut(Run run) {
            return 5000;
        }
        @Override public Descriptor<BuildTimeOutStrategy> getDescriptor() {
            throw new UnsupportedOperationException();
        }
    }
    
    public static class ExecutionCheckBuilder extends Builder {
        public boolean executed = false;
        
        @Override
        public boolean perform(AbstractBuild<?, ?> build, Launcher launcher, BuildListener listener)
                throws InterruptedException, IOException {
            executed = true;
            return true;
        }
        
        @TestExtension
        public static class DescriptorImpl extends BuildStepDescriptor<Builder> {
            @Override
            public boolean isApplicable(@SuppressWarnings("rawtypes") Class<? extends AbstractProject> arg0) {
                return true;
            }
            
            @Override
            public String getDisplayName() {
                return "ExecutionCheckBuilder";
            }
        }
    }
    
    public static class ExecutionCheckPublisher extends Recorder {
        public boolean executed = false;
        
        public BuildStepMonitor getRequiredMonitorService() {
            return BuildStepMonitor.NONE;
        }
        
        @Override
        public boolean perform(AbstractBuild<?, ?> build, Launcher launcher, BuildListener listener)
                throws InterruptedException, IOException {
            executed = true;
            return true;
        }
        
        @SuppressWarnings({"rawtypes", "unchecked"})
        @Override
        public BuildStepDescriptor getDescriptor() {
            return super.getDescriptor();
        }
        
        @TestExtension
        public static class DescriptorImpl extends BuildStepDescriptor<Publisher> {
            @Override
            public boolean isApplicable(@SuppressWarnings("rawtypes") Class<? extends AbstractProject> arg0)
            {
                return true;
            }
            
            @Override
            public String getDisplayName() {
                return "ExecutionCheckPublisher";
            }
        }
    }
    
    public void testAbort() throws Exception
    {
        BuildTimeoutWrapper.MINIMUM_TIMEOUT_MILLISECONDS = 0;
        // No description
        {
            FreeStyleProject project = createFreeStyleProject();
            project.getBuildWrappersList().add(new BuildTimeoutWrapper(new QuickBuildTimeOutStrategy(), false, false));
            project.getBuildersList().add(new SleepBuilder(9999));
            
            ExecutionCheckBuilder checkBuilder = new ExecutionCheckBuilder();
            ExecutionCheckPublisher checkPublisher = new ExecutionCheckPublisher();
            project.getBuildersList().add(checkBuilder);
            project.getPublishersList().add(checkPublisher);
            
            assertFalse(checkBuilder.executed);
            assertFalse(checkPublisher.executed);
            
            FreeStyleBuild build = project.scheduleBuild2(0).get();
            
            assertBuildStatus(Result.ABORTED, build);
            assertFalse(checkBuilder.executed);
            assertTrue(checkPublisher.executed);
            assertNull(build.getDescription());
        }
        
        // Set description
        {
            FreeStyleProject project = createFreeStyleProject();
            project.getBuildWrappersList().add(new BuildTimeoutWrapper(new QuickBuildTimeOutStrategy(), false, true));
            project.getBuildersList().add(new SleepBuilder(9999));
            
            ExecutionCheckBuilder checkBuilder = new ExecutionCheckBuilder();
            ExecutionCheckPublisher checkPublisher = new ExecutionCheckPublisher();
            project.getBuildersList().add(checkBuilder);
            project.getPublishersList().add(checkPublisher);
            
            assertFalse(checkBuilder.executed);
            assertFalse(checkPublisher.executed);
            
            FreeStyleBuild build = project.scheduleBuild2(0).get();
            
            assertBuildStatus(Result.ABORTED, build);
            assertFalse(checkBuilder.executed);
            assertTrue(checkPublisher.executed);
            assertNotNull(build.getDescription());
            assertFalse(build.getDescription().isEmpty());
        }
        
        // Not aborted
        {
            FreeStyleProject project = createFreeStyleProject();
            project.getBuildWrappersList().add(new BuildTimeoutWrapper(new QuickBuildTimeOutStrategy(), false, true));
            project.getBuildersList().add(new SleepBuilder(1000));
            
            ExecutionCheckBuilder checkBuilder = new ExecutionCheckBuilder();
            ExecutionCheckPublisher checkPublisher = new ExecutionCheckPublisher();
            project.getBuildersList().add(checkBuilder);
            project.getPublishersList().add(checkPublisher);
            
            assertFalse(checkBuilder.executed);
            assertFalse(checkPublisher.executed);
            
            FreeStyleBuild build = project.scheduleBuild2(0).get();
            
            assertBuildStatusSuccess(build);
            assertTrue(checkBuilder.executed);
            assertTrue(checkPublisher.executed);
            assertNull(build.getDescription());
        }
    }
    
    public void testFail() throws Exception
    {
        BuildTimeoutWrapper.MINIMUM_TIMEOUT_MILLISECONDS = 0;
        // No description
        {
            FreeStyleProject project = createFreeStyleProject();
            project.getBuildWrappersList().add(new BuildTimeoutWrapper(new QuickBuildTimeOutStrategy(), true, false));
            project.getBuildersList().add(new SleepBuilder(9999));
            
            ExecutionCheckBuilder checkBuilder = new ExecutionCheckBuilder();
            ExecutionCheckPublisher checkPublisher = new ExecutionCheckPublisher();
            project.getBuildersList().add(checkBuilder);
            project.getPublishersList().add(checkPublisher);
            
            assertFalse(checkBuilder.executed);
            assertFalse(checkPublisher.executed);
            
            FreeStyleBuild build = project.scheduleBuild2(0).get();
            
            assertBuildStatus(Result.FAILURE, build);
            assertFalse(checkBuilder.executed);
            assertTrue(checkPublisher.executed);
            assertNull(build.getDescription());
        }
        
        // Set description
        {
            FreeStyleProject project = createFreeStyleProject();
            project.getBuildWrappersList().add(new BuildTimeoutWrapper(new QuickBuildTimeOutStrategy(), true, true));
            project.getBuildersList().add(new SleepBuilder(9999));
            
            ExecutionCheckBuilder checkBuilder = new ExecutionCheckBuilder();
            ExecutionCheckPublisher checkPublisher = new ExecutionCheckPublisher();
            project.getBuildersList().add(checkBuilder);
            project.getPublishersList().add(checkPublisher);
            
            assertFalse(checkBuilder.executed);
            assertFalse(checkPublisher.executed);
            
            FreeStyleBuild build = project.scheduleBuild2(0).get();
            
            assertBuildStatus(Result.FAILURE, build);
            assertFalse(checkBuilder.executed);
            assertTrue(checkPublisher.executed);
            assertNotNull(build.getDescription());
            assertFalse(build.getDescription().isEmpty());
        }
        
        // Not aborted
        {
            FreeStyleProject project = createFreeStyleProject();
            project.getBuildWrappersList().add(new BuildTimeoutWrapper(new QuickBuildTimeOutStrategy(), true, true));
            project.getBuildersList().add(new SleepBuilder(1000));
            
            ExecutionCheckBuilder checkBuilder = new ExecutionCheckBuilder();
            ExecutionCheckPublisher checkPublisher = new ExecutionCheckPublisher();
            project.getBuildersList().add(checkBuilder);
            project.getPublishersList().add(checkPublisher);
            
            assertFalse(checkBuilder.executed);
            assertFalse(checkPublisher.executed);
            
            FreeStyleBuild build = project.scheduleBuild2(0).get();
            
            assertBuildStatusSuccess(build);
            assertTrue(checkBuilder.executed);
            assertTrue(checkPublisher.executed);
            assertNull(build.getDescription());
        }
    }
}
