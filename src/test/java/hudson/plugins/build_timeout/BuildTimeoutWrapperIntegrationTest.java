package hudson.plugins.build_timeout;

import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import hudson.EnvVars;
import hudson.Launcher;
import hudson.model.AbstractBuild;
import hudson.model.AbstractProject;
import hudson.model.BuildListener;
import hudson.model.FreeStyleBuild;
import hudson.model.FreeStyleProject;
import hudson.model.Result;
import hudson.plugins.build_timeout.impl.AbsoluteTimeOutStrategy;
import hudson.plugins.build_timeout.impl.NoActivityTimeOutStrategy;
import hudson.plugins.build_timeout.operations.AbortOperation;
import hudson.plugins.build_timeout.operations.FailOperation;
import hudson.plugins.build_timeout.operations.WriteDescriptionOperation;
import hudson.tasks.BuildStepDescriptor;
import hudson.tasks.BuildStepMonitor;
import hudson.tasks.Builder;
import hudson.tasks.Publisher;
import hudson.tasks.Recorder;

import org.jvnet.hudson.test.Bug;
import org.jvnet.hudson.test.HudsonTestCase;
import org.jvnet.hudson.test.TestExtension;
import org.jvnet.hudson.test.CaptureEnvironmentBuilder;
import org.jvnet.hudson.test.SleepBuilder;
import org.jvnet.hudson.test.recipes.LocalData;

import com.gargoylesoftware.htmlunit.html.HtmlForm;
import com.gargoylesoftware.htmlunit.html.HtmlPage;
import com.google.common.base.Function;
import com.google.common.collect.Lists;

public class BuildTimeoutWrapperIntegrationTest extends HudsonTestCase {
	
	/*
	 * Method to get environment variables when no timeout env variable is declared
	 */
	public EnvVars getEnvVars() throws Exception {
		BuildTimeoutWrapper.MINIMUM_TIMEOUT_MILLISECONDS = 0;
		FreeStyleProject project = createFreeStyleProject();
		project.getBuildWrappersList().add(new BuildTimeoutWrapper(new QuickBuildTimeOutStrategy(300000), false, false));
		CaptureEnvironmentBuilder captureEnvBuilder = new CaptureEnvironmentBuilder();
		project.getBuildersList().add(captureEnvBuilder);
		project.scheduleBuild2(0).get();
		return captureEnvBuilder.getEnvVars();
	}

	/*
	 * Test to verify setting a timeout environment variable to a valid string
	 */
	public void testBuildTimeoutEnvValid() throws Exception {

		EnvVars expectedEnvVars = getEnvVars();
	  	FreeStyleProject project = createFreeStyleProject();
	  	project.getBuildWrappersList().add(new BuildTimeoutWrapper(
              new QuickBuildTimeOutStrategy(12345),
              Arrays.<BuildTimeOutOperation>asList(),
              "BUILD_TIMEOUT"
	  	));
		CaptureEnvironmentBuilder captureEnvBuilder = new CaptureEnvironmentBuilder();
		project.getBuildersList().add(captureEnvBuilder);
		project.scheduleBuild2(0).get();
		EnvVars envVars = captureEnvBuilder.getEnvVars();

		assertEquals(expectedEnvVars.size()+1, envVars.size());
		assertEquals("12345", envVars.get("BUILD_TIMEOUT"));
	}

	/*
	 * Test to verify setting timeout environment variable to null (this is the default)
	 */
	public void testBuildTimeoutEnvNull() throws Exception {

	    EnvVars expectedEnvVars = getEnvVars();
		FreeStyleProject project = createFreeStyleProject();
		project.getBuildWrappersList().add(new BuildTimeoutWrapper(
                new QuickBuildTimeOutStrategy(12345),
                Arrays.<BuildTimeOutOperation>asList(),
                null
		));
		CaptureEnvironmentBuilder captureEnvBuilder = new CaptureEnvironmentBuilder();
		project.getBuildersList().add(captureEnvBuilder);
		project.scheduleBuild2(0).get();
		EnvVars envVars = captureEnvBuilder.getEnvVars();

  		assertEquals(expectedEnvVars.size(), envVars.size());
	}

	/*
	 * Test to verify setting timeout environment variable to empty string.
	 */
	public void testBuildTimeoutEnvEmpty() throws Exception {

		EnvVars expectedEnvVars = getEnvVars();
		FreeStyleProject project = createFreeStyleProject();
		project.getBuildWrappersList().add(new BuildTimeoutWrapper(
                new QuickBuildTimeOutStrategy(12345),
                Arrays.<BuildTimeOutOperation>asList(),
                "   "
		));
		CaptureEnvironmentBuilder captureEnvBuilder = new CaptureEnvironmentBuilder();
		project.getBuildersList().add(captureEnvBuilder);
		project.scheduleBuild2(0).get();
		EnvVars envVars = captureEnvBuilder.getEnvVars();

  		assertEquals(expectedEnvVars.size(), envVars.size());
	}
	
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
    
    @LocalData
    public void testMigrationFrom_1_12_2() throws Exception {
        BuildTimeoutWrapper.MINIMUM_TIMEOUT_MILLISECONDS = 0;
        // Abort
        {
            FreeStyleProject project = jenkins.getItem("AbortWithoutDescription", jenkins, FreeStyleProject.class);
            assertNotNull(project);
            
            // assert migration of configuration
            BuildTimeoutWrapper buildTimeout = project.getBuildWrappersList().get(BuildTimeoutWrapper.class);
            assertNotNull(buildTimeout);
            assertEquals(QuickBuildTimeOutStrategy.class, buildTimeout.getStrategy().getClass());
            assertEquals(
                    Arrays.asList(AbortOperation.class),
                    Lists.transform(buildTimeout.getOperationList(), new Function<BuildTimeOutOperation, Class<? extends BuildTimeOutOperation>>() {
                        public Class<? extends BuildTimeOutOperation> apply(BuildTimeOutOperation input) {
                            return input.getClass();
                        }
                    })
            );
            
            // assert it works.
            ExecutionCheckBuilder checkBuilder = project.getBuildersList().get(ExecutionCheckBuilder.class);
            ExecutionCheckPublisher checkPublisher = project.getPublishersList().get(ExecutionCheckPublisher.class);
            assertNotNull(checkBuilder);
            assertNotNull(checkPublisher);
            
            assertFalse(checkBuilder.executed);
            assertFalse(checkPublisher.executed);
            
            FreeStyleBuild build = project.scheduleBuild2(0).get();
            
            assertBuildStatus(Result.ABORTED, build);
            assertFalse(checkBuilder.executed);
            assertTrue(checkPublisher.executed);
            assertNull(build.getDescription());
        }
        
        // Abort and Set description
        {
            FreeStyleProject project = jenkins.getItem("AbortWithDescription", jenkins, FreeStyleProject.class);
            assertNotNull(project);
            
            // assert migration of configuration
            BuildTimeoutWrapper buildTimeout = project.getBuildWrappersList().get(BuildTimeoutWrapper.class);
            assertNotNull(buildTimeout);
            assertEquals(QuickBuildTimeOutStrategy.class, buildTimeout.getStrategy().getClass());
            assertEquals(
                    Arrays.asList(WriteDescriptionOperation.class, AbortOperation.class),
                    Lists.transform(buildTimeout.getOperationList(), new Function<BuildTimeOutOperation, Class<? extends BuildTimeOutOperation>>() {
                        public Class<? extends BuildTimeOutOperation> apply(BuildTimeOutOperation input) {
                            return input.getClass();
                        }
                    })
            );
            
            // assert it works.
            ExecutionCheckBuilder checkBuilder = project.getBuildersList().get(ExecutionCheckBuilder.class);
            ExecutionCheckPublisher checkPublisher = project.getPublishersList().get(ExecutionCheckPublisher.class);
            assertNotNull(checkBuilder);
            assertNotNull(checkPublisher);
            
            assertFalse(checkBuilder.executed);
            assertFalse(checkPublisher.executed);
            
            FreeStyleBuild build = project.scheduleBuild2(0).get();
            
            assertBuildStatus(Result.ABORTED, build);
            assertFalse(checkBuilder.executed);
            assertTrue(checkPublisher.executed);
            assertNotNull(build.getDescription());
            assertFalse(build.getDescription().isEmpty());
        }
        
        // Fail
        {
            FreeStyleProject project = jenkins.getItem("FailWithoutDescription", jenkins, FreeStyleProject.class);
            assertNotNull(project);
            
            // assert migration of configuration
            BuildTimeoutWrapper buildTimeout = project.getBuildWrappersList().get(BuildTimeoutWrapper.class);
            assertNotNull(buildTimeout);
            assertEquals(QuickBuildTimeOutStrategy.class, buildTimeout.getStrategy().getClass());
            assertEquals(
                    Arrays.asList(FailOperation.class),
                    Lists.transform(buildTimeout.getOperationList(), new Function<BuildTimeOutOperation, Class<? extends BuildTimeOutOperation>>() {
                        public Class<? extends BuildTimeOutOperation> apply(BuildTimeOutOperation input) {
                            return input.getClass();
                        }
                    })
            );
            
            // assert it works.
            ExecutionCheckBuilder checkBuilder = project.getBuildersList().get(ExecutionCheckBuilder.class);
            ExecutionCheckPublisher checkPublisher = project.getPublishersList().get(ExecutionCheckPublisher.class);
            assertNotNull(checkBuilder);
            assertNotNull(checkPublisher);
            
            assertFalse(checkBuilder.executed);
            assertFalse(checkPublisher.executed);
            
            FreeStyleBuild build = project.scheduleBuild2(0).get();
            
            assertBuildStatus(Result.FAILURE, build);
            assertFalse(checkBuilder.executed);
            assertTrue(checkPublisher.executed);
            assertNull(build.getDescription());
        }
        
        // Fail and Set description
        {
            FreeStyleProject project = jenkins.getItem("FailWithDescription", jenkins, FreeStyleProject.class);
            assertNotNull(project);
            
            // assert migration of configuration
            BuildTimeoutWrapper buildTimeout = project.getBuildWrappersList().get(BuildTimeoutWrapper.class);
            assertNotNull(buildTimeout);
            assertEquals(QuickBuildTimeOutStrategy.class, buildTimeout.getStrategy().getClass());
            assertEquals(
                    Arrays.asList(WriteDescriptionOperation.class, FailOperation.class),
                    Lists.transform(buildTimeout.getOperationList(), new Function<BuildTimeOutOperation, Class<? extends BuildTimeOutOperation>>() {
                        public Class<? extends BuildTimeOutOperation> apply(BuildTimeOutOperation input) {
                            return input.getClass();
                        }
                    })
            );
            
            ExecutionCheckBuilder checkBuilder = project.getBuildersList().get(ExecutionCheckBuilder.class);
            ExecutionCheckPublisher checkPublisher = project.getPublishersList().get(ExecutionCheckPublisher.class);
            assertNotNull(checkBuilder);
            assertNotNull(checkPublisher);
            
            assertFalse(checkBuilder.executed);
            assertFalse(checkPublisher.executed);
            
            FreeStyleBuild build = project.scheduleBuild2(0).get();
            
            assertBuildStatus(Result.FAILURE, build);
            assertFalse(checkBuilder.executed);
            assertTrue(checkPublisher.executed);
            assertNotNull(build.getDescription());
            assertFalse(build.getDescription().isEmpty());
        }
    }
    
    private static class TestBuildTimeOutOperation extends BuildTimeOutOperation {
        public boolean executed = false;
        private final boolean result;
        public TestBuildTimeOutOperation(boolean result) {
            this.result = result;
        }
        public TestBuildTimeOutOperation() {
            this(true);
        }
        @Override
        public boolean perform(AbstractBuild<?, ?> build, BuildListener listener, long effectiveTimeout) {
            executed = true;
            return result;
        }
    }
    
    public void testMultipleOperations() throws Exception {
        BuildTimeoutWrapper.MINIMUM_TIMEOUT_MILLISECONDS = 0;
        FreeStyleProject project = createFreeStyleProject();
        TestBuildTimeOutOperation op1 = new TestBuildTimeOutOperation();
        TestBuildTimeOutOperation op2 = new TestBuildTimeOutOperation();
        TestBuildTimeOutOperation op3 = new TestBuildTimeOutOperation();
        project.getBuildWrappersList().add(new BuildTimeoutWrapper(
                new QuickBuildTimeOutStrategy(1000),
                Arrays.<BuildTimeOutOperation>asList(op1, op2, op3)
        ));
        project.getBuildersList().add(new SleepBuilder(5000));
        
        ExecutionCheckBuilder checkBuilder = new ExecutionCheckBuilder();
        ExecutionCheckPublisher checkPublisher = new ExecutionCheckPublisher();
        project.getBuildersList().add(checkBuilder);
        project.getPublishersList().add(checkPublisher);
        
        assertFalse(checkBuilder.executed);
        assertFalse(checkPublisher.executed);
        assertFalse(op1.executed);
        assertFalse(op2.executed);
        assertFalse(op3.executed);
        
        FreeStyleBuild build = project.scheduleBuild2(0).get();
        
        assertBuildStatusSuccess(build);
        assertTrue(checkBuilder.executed);
        assertTrue(checkPublisher.executed);
        assertTrue(op1.executed);
        assertTrue(op2.executed);
        assertTrue(op3.executed);
    }
    
    public void testFailingOperations() throws Exception {
        BuildTimeoutWrapper.MINIMUM_TIMEOUT_MILLISECONDS = 0;
        FreeStyleProject project = createFreeStyleProject();
        TestBuildTimeOutOperation op1 = new TestBuildTimeOutOperation();
        TestBuildTimeOutOperation op2 = new TestBuildTimeOutOperation(false); // Fail!
        TestBuildTimeOutOperation op3 = new TestBuildTimeOutOperation();
        project.getBuildWrappersList().add(new BuildTimeoutWrapper(
                new QuickBuildTimeOutStrategy(1000),
                Arrays.<BuildTimeOutOperation>asList(op1, op2, op3)
        ));
        project.getBuildersList().add(new SleepBuilder(5000));
        
        ExecutionCheckBuilder checkBuilder = new ExecutionCheckBuilder();
        ExecutionCheckPublisher checkPublisher = new ExecutionCheckPublisher();
        project.getBuildersList().add(checkBuilder);
        project.getPublishersList().add(checkPublisher);
        
        assertFalse(checkBuilder.executed);
        assertFalse(checkPublisher.executed);
        assertFalse(op1.executed);
        assertFalse(op2.executed);
        assertFalse(op3.executed);
        
        FreeStyleBuild build = project.scheduleBuild2(0).get();
        
        assertBuildStatus(Result.FAILURE, build);
        assertTrue(checkBuilder.executed);
        assertTrue(checkPublisher.executed);
        assertTrue(op1.executed);
        assertTrue(op2.executed);
        assertFalse(op3.executed);
    }
    
    public void testConfigurationNoOperation() throws Exception {
        FreeStyleProject p = createFreeStyleProject();
        p.getBuildWrappersList().add(new BuildTimeoutWrapper(
                new AbsoluteTimeOutStrategy(3),
                Collections.<BuildTimeOutOperation>emptyList(),
                "TESTVAR"
        ));
        p.save();
        
        String fullname = p.getFullName();
        
        // reconfigure.
        // This should preserve configuration.
        WebClient wc = createWebClient();
        HtmlPage page = wc.getPage(p, "configure");
        HtmlForm form = page.getFormByName("config");
        submit(form);
        
        p = jenkins.getItemByFullName(fullname, FreeStyleProject.class);
        
        // assert strategy is preserved.
        assertEquals(
                AbsoluteTimeOutStrategy.class,
                p.getBuildWrappersList().get(BuildTimeoutWrapper.class).getStrategy().getClass()
        );
        assertEquals(
                "3",
                ((AbsoluteTimeOutStrategy)p.getBuildWrappersList().get(BuildTimeoutWrapper.class).getStrategy()).getTimeoutMinutes()
        );
        
        // assert operation is preserved
        assertEquals(
                Collections.emptyList(),
                p.getBuildWrappersList().get(BuildTimeoutWrapper.class).getOperationList()
        );
        
        assertEquals(
                "TESTVAR",
                p.getBuildWrappersList().get(BuildTimeoutWrapper.class).getTimeoutEnvVar()
        );
    }
    
    public void testConfigurationSingleOperation() throws Exception {
        FreeStyleProject p = createFreeStyleProject();
        p.getBuildWrappersList().add(new BuildTimeoutWrapper(
                new AbsoluteTimeOutStrategy(3),
                Arrays.<BuildTimeOutOperation>asList(
                        new WriteDescriptionOperation("test")
                ),
                "TESTVAR"
        ));
        p.save();
        
        String fullname = p.getFullName();
        
        // reconfigure.
        // This should preserve configuration.
        WebClient wc = createWebClient();
        HtmlPage page = wc.getPage(p, "configure");
        HtmlForm form = page.getFormByName("config");
        submit(form);
        
        p = jenkins.getItemByFullName(fullname, FreeStyleProject.class);
        
        // assert strategy is preserved.
        assertEquals(
                AbsoluteTimeOutStrategy.class,
                p.getBuildWrappersList().get(BuildTimeoutWrapper.class).getStrategy().getClass()
        );
        assertEquals(
                "3",
                ((AbsoluteTimeOutStrategy)p.getBuildWrappersList().get(BuildTimeoutWrapper.class).getStrategy()).getTimeoutMinutes()
        );
        
        // assert operation is preserved
        assertEquals(
                Arrays.asList(WriteDescriptionOperation.class),
                Lists.transform(
                        p.getBuildWrappersList().get(BuildTimeoutWrapper.class).getOperationList(),
                        new Function<BuildTimeOutOperation, Class<? extends BuildTimeOutOperation>>() {
                            public Class<? extends BuildTimeOutOperation> apply(BuildTimeOutOperation input) {
                                return input.getClass();
                            }
                        }
                )
        );
        assertEquals(
                "test",
                ((WriteDescriptionOperation)p.getBuildWrappersList().get(BuildTimeoutWrapper.class).getOperationList().get(0)).getDescription()
        );
        
        assertEquals(
                "TESTVAR",
                p.getBuildWrappersList().get(BuildTimeoutWrapper.class).getTimeoutEnvVar()
        );
    }
    
    
    public void testConfigurationMultipleOperation() throws Exception {
        FreeStyleProject p = createFreeStyleProject();
        p.getBuildWrappersList().add(new BuildTimeoutWrapper(
                new AbsoluteTimeOutStrategy(3),
                Arrays.<BuildTimeOutOperation>asList(
                        new WriteDescriptionOperation("test"),
                        new AbortOperation()
                ),
                "TESTVAR"
        ));
        p.save();
        
        String fullname = p.getFullName();
        
        // reconfigure.
        // This should preserve configuration.
        WebClient wc = createWebClient();
        HtmlPage page = wc.getPage(p, "configure");
        HtmlForm form = page.getFormByName("config");
        submit(form);
        
        p = jenkins.getItemByFullName(fullname, FreeStyleProject.class);
        
        // assert strategy is preserved.
        assertEquals(
                AbsoluteTimeOutStrategy.class,
                p.getBuildWrappersList().get(BuildTimeoutWrapper.class).getStrategy().getClass()
        );
        assertEquals(
                "3",
                ((AbsoluteTimeOutStrategy)p.getBuildWrappersList().get(BuildTimeoutWrapper.class).getStrategy()).getTimeoutMinutes()
        );
        
        // assert operation is preserved
        assertEquals(
                Arrays.asList(
                        WriteDescriptionOperation.class,
                        AbortOperation.class
                ),
                Lists.transform(
                        p.getBuildWrappersList().get(BuildTimeoutWrapper.class).getOperationList(),
                        new Function<BuildTimeOutOperation, Class<? extends BuildTimeOutOperation>>() {
                            public Class<? extends BuildTimeOutOperation> apply(BuildTimeOutOperation input) {
                                return input.getClass();
                            }
                        }
                )
        );
        assertEquals(
                "test",
                ((WriteDescriptionOperation)p.getBuildWrappersList().get(BuildTimeoutWrapper.class).getOperationList().get(0)).getDescription()
        );
        
        assertEquals(
                "TESTVAR",
                p.getBuildWrappersList().get(BuildTimeoutWrapper.class).getTimeoutEnvVar()
        );
    }
    
    @LocalData
    public void testMigrationFrom_1_13() throws Exception {
        Thread.sleep(60000);
        FreeStyleProject p = jenkins.getItemByFullName("NoActivityTimeOutStrategy", FreeStyleProject.class);
        assertNotNull(p);
        NoActivityTimeOutStrategy strategy = (NoActivityTimeOutStrategy)p.getBuildWrappersList().get(BuildTimeoutWrapper.class).getStrategy();
        assertEquals("5", strategy.getTimeoutSecondsString());
        assertEquals(5, strategy.getTimeoutSeconds());
        
        assertBuildStatus(Result.ABORTED, p.scheduleBuild2(0).get());
    }
}
