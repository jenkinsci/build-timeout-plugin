package hudson.plugins.build_timeout;

import hudson.model.Cause;
import hudson.model.FreeStyleBuild;
import hudson.model.FreeStyleProject;
import hudson.model.Result;
import hudson.plugins.build_timeout.operations.AbortOperation;
import hudson.plugins.build_timeout.operations.FailOperation;
import hudson.tasks.Builder;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.jvnet.hudson.test.JenkinsRule;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

public class BuildStepWithTimeoutTest {

    @Rule
    public JenkinsRule j = new JenkinsRule();

    private final static long TINY_DELAY = 100L;
    private final static long HUGE_DELAY = 5000L;

    @Before
    public void before() {
        // this allows timeout shorter than 3 minutes.
        BuildTimeoutWrapper.MINIMUM_TIMEOUT_MILLISECONDS = 100;
    }

    @Test
    public void timeoutWasNotTriggered() throws Exception {
        final FreeStyleProject project = createProjectWithBuildStepWithTimeout(TINY_DELAY, null);

        final FreeStyleBuild build = project.scheduleBuild2(0, new Cause.UserIdCause()).get();

        j.assertBuildStatusSuccess(build);
        j.assertLogContains(FakeBuildStep.FAKE_BUILD_STEP_OUTPUT, build);
        assertNull(build.getAction(BuildTimeOutAction.class));
    }

    @Test
    public void timeoutWasTriggeredWithoutAction() throws Exception {
        final FreeStyleProject project = createProjectWithBuildStepWithTimeout(HUGE_DELAY, null);

        final FreeStyleBuild build = project.scheduleBuild2(0, new Cause.UserIdCause()).get();

        j.assertBuildStatus(Result.ABORTED, build);
        j.assertLogNotContains(FakeBuildStep.FAKE_BUILD_STEP_OUTPUT, build);
    }

    @Test
    public void timeoutWasTriggeredWithAbortOperation() throws Exception {
        final FreeStyleProject project = createProjectWithBuildStepWithTimeout(HUGE_DELAY, new AbortOperation());

        final FreeStyleBuild build = project.scheduleBuild2(0, new Cause.UserIdCause()).get();

        j.assertBuildStatus(Result.ABORTED, build);
        j.assertLogNotContains(FakeBuildStep.FAKE_BUILD_STEP_OUTPUT, build);
        BuildTimeOutAction action = build.getAction(BuildTimeOutAction.class);
        assertNotNull(action);
        assertEquals(AbortOperation.class.getSimpleName(), action.getReason());
    }

    @Test
    public void timeoutWasTriggeredWithFailOperation() throws Exception {
        final FreeStyleProject project = createProjectWithBuildStepWithTimeout(HUGE_DELAY, new FailOperation());

        final FreeStyleBuild build = project.scheduleBuild2(0, new Cause.UserIdCause()).get();

        j.assertBuildStatus(Result.FAILURE, build);
        j.assertLogNotContains(FakeBuildStep.FAKE_BUILD_STEP_OUTPUT, build);
        BuildTimeOutAction action = build.getAction(BuildTimeOutAction.class);
        assertNotNull(action);
        assertEquals(FailOperation.class.getSimpleName(), action.getReason());
    }

    private FreeStyleProject createProjectWithBuildStepWithTimeout(long delay, BuildTimeOutOperation operation) throws IOException {
        final FreeStyleProject project = j.createFreeStyleProject();
        final List<BuildTimeOutOperation> operations;

        if (operation!=null) {
            operations = new ArrayList<>();
            operations.add(operation);
        } else {
            operations = null;
        }

        final Builder step = new BuildStepWithTimeout(new FakeBuildStep(delay),
                new QuickBuildTimeOutStrategy(500), operations);

        project.getBuildersList().add(step);

        return project;
    }
}
