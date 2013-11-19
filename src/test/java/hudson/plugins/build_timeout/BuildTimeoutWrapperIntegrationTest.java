package hudson.plugins.build_timeout;

import hudson.model.Descriptor;
import hudson.model.FreeStyleBuild;
import hudson.model.FreeStyleProject;
import hudson.model.Result;
import hudson.model.Run;

import org.jvnet.hudson.test.Bug;
import org.jvnet.hudson.test.HudsonTestCase;
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
}
