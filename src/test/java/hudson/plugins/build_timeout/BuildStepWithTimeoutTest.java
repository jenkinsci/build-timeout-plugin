package hudson.plugins.build_timeout;

import hudson.model.Cause;
import hudson.model.FreeStyleBuild;
import hudson.model.FreeStyleProject;
import hudson.tasks.Builder;
import hudson.util.IOUtils;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import java.io.IOException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class BuildStepWithTimeoutTest {
    @Rule
    public BuildTimeOutJenkinsRule j = new BuildTimeOutJenkinsRule();

    @Before
    public void before() {
        // this allows timeout shorter than 3 minutes.
        BuildTimeoutWrapper.MINIMUM_TIMEOUT_MILLISECONDS = 100;
    }

    @Test
    public void testTimeoutWasNotTriggered() throws IOException, ExecutionException, InterruptedException {
        FreeStyleProject project = j.createFreeStyleProject();
        project.setDisplayName("Check timeout");

        Builder step = new BuildStepWithTimeout(new FakeBuildStep(0), new QuickBuildTimeOutStrategy(500), null);

        project.getBuildersList().add(step);

        Future<FreeStyleBuild> future = project.scheduleBuild2(0, new Cause.UserIdCause());
        FreeStyleBuild build = future.get();

        assertTrue(IOUtils.toString(build.getLogReader()).contains("Test"));
    }

    @Test
    public void testTimeoutWasTriggered() throws IOException, ExecutionException, InterruptedException {
        FreeStyleProject project = j.createFreeStyleProject();
        project.setDisplayName("Check timeout");

        Builder step = new BuildStepWithTimeout(new FakeBuildStep(5000), new QuickBuildTimeOutStrategy(500), null);

        project.getBuildersList().add(step);

        Future<FreeStyleBuild> future = project.scheduleBuild2(0, new Cause.UserIdCause());
        FreeStyleBuild build = future.get();

        assertFalse(IOUtils.toString(build.getLogReader()).contains("Test"));
    }

}
