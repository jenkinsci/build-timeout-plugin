package hudson.plugins.build_timeout;

import hudson.model.Cause;
import hudson.model.FreeStyleBuild;
import hudson.model.FreeStyleProject;
import hudson.model.Result;
import hudson.plugins.build_timeout.operations.AbortOperation;
import hudson.plugins.build_timeout.operations.FailOperation;
import hudson.tasks.Builder;
import org.junit.Rule;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.jvnet.hudson.test.JenkinsRule;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class BuildStepWithTimeoutTest {

    @Rule
    public JenkinsRule j = new JenkinsRule();

    private final static long TINY_DELAY = 100L;
    private final static long HUGE_DELAY = 5000L;

    @BeforeEach
    public void before() {
        // this allows timeout shorter than 3 minutes.
        BuildTimeoutWrapper.MINIMUM_TIMEOUT_MILLISECONDS = 100;
    }

    @Test
    void timeoutWasNotTriggered() throws Exception {
        final FreeStyleProject project = createProjectWithBuildStepWithTimeout(TINY_DELAY, null);

        final FreeStyleBuild build = project.scheduleBuild2(0, new Cause.UserIdCause()).get();

        j.assertBuildStatusSuccess(build);
        j.assertLogContains(FakeBuildStep.FAKE_BUILD_STEP_OUTPUT, build);
    }

    @Test
    void timeoutWasTriggeredWithoutAction() throws Exception {
        final FreeStyleProject project = createProjectWithBuildStepWithTimeout(HUGE_DELAY, null);

        final FreeStyleBuild build = project.scheduleBuild2(0, new Cause.UserIdCause()).get();

        j.assertBuildStatus(Result.ABORTED, build);
        j.assertLogNotContains(FakeBuildStep.FAKE_BUILD_STEP_OUTPUT, build);
    }

    @Test
    void timeoutWasTriggeredWithAbortOperation() throws Exception {
        final FreeStyleProject project = createProjectWithBuildStepWithTimeout(HUGE_DELAY, new AbortOperation());

        final FreeStyleBuild build = project.scheduleBuild2(0, new Cause.UserIdCause()).get();

        j.assertBuildStatus(Result.ABORTED, build);
        j.assertLogNotContains(FakeBuildStep.FAKE_BUILD_STEP_OUTPUT, build);
    }

    @Test
    void timeoutWasTriggeredWithFailOperation() throws Exception {
        final FreeStyleProject project = createProjectWithBuildStepWithTimeout(HUGE_DELAY, new FailOperation());

        final FreeStyleBuild build = project.scheduleBuild2(0, new Cause.UserIdCause()).get();

        j.assertBuildStatus(Result.FAILURE, build);
        j.assertLogNotContains(FakeBuildStep.FAKE_BUILD_STEP_OUTPUT, build);
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
