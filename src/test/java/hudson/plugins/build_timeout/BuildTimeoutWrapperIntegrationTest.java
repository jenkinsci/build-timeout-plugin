package hudson.plugins.build_timeout;

import hudson.model.FreeStyleBuild;
import hudson.model.FreeStyleProject;
import hudson.model.Result;

import org.jvnet.hudson.test.Bug;
import org.jvnet.hudson.test.HudsonTestCase;
import org.jvnet.hudson.test.recipes.LocalData;

public class BuildTimeoutWrapperIntegrationTest extends HudsonTestCase {

	@Bug(9203)
	@LocalData
	public void testIssue9203() throws Exception {
		FreeStyleProject project = (FreeStyleProject) hudson.getItem("9203");
		
		// Force a timeout of 1, plugin will not accept less than 3 minutes.
		BuildTimeoutWrapper buildWrapper = (BuildTimeoutWrapper) project.getBuildWrappersList().get(0);
		buildWrapper.timeoutMinutes = 1;
		
		FreeStyleBuild build = project.scheduleBuild2(0).get();
		assertBuildStatus(Result.FAILURE, build);
	}
}
