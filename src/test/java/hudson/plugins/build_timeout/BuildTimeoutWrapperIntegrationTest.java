package hudson.plugins.build_timeout;

import hudson.model.FreeStyleBuild;
import hudson.model.FreeStyleProject;
import hudson.model.Result;
import org.jvnet.hudson.test.Bug;
import org.jvnet.hudson.test.HudsonTestCase;
import org.jvnet.hudson.test.recipes.LocalData;

/**
 * @author gold.dudu@gmail.com
 */
public class BuildTimeoutWrapperIntegrationTest extends HudsonTestCase {

    @Bug(9203)
    @LocalData
    public void testIssue9203() throws Exception {
        FreeStyleProject project = (FreeStyleProject) hudson.getItem("9203");

        BuildTimeoutWrapper buildWrapper = (BuildTimeoutWrapper) project.getBuildWrappersList().get(0);
        buildWrapper.thresholdPercentage = 100;
        buildWrapper.buildsToCalculateAverage = 3;
        buildWrapper.failBuild = true;
        buildWrapper.setDelaySecondsForTest(1);

        //build 1
        System.out.println("toFail = true, build to average = 3, threshold = 100, hard-coded = 0, no average");
        FreeStyleBuild build = project.scheduleBuild2(0).get();
        assertBuildStatus(Result.SUCCESS, build);

        //build 2
        System.out.println("toFail = true, build to average = 3, threshold = 100, hard-coded = 0, no average");
        build = project.scheduleBuild2(0).get();
        assertBuildStatus(Result.SUCCESS, build);

        //build 3
        System.out.println("toFail = true, build to average = 3, threshold = 100, hard-coded = 0, no average");
        build = project.scheduleBuild2(0).get();
        assertBuildStatus(Result.SUCCESS, build);

        //build 4
        System.out.println("toFail = true, build to average = 3, threshold = 100, hard-coded = 0, average = 1");
        build = project.scheduleBuild2(0).get();
        assertBuildStatus(Result.SUCCESS, build);

        //build 5
        System.out.println("toFail = true, build to average = 3, threshold = 100, hard-coded = 0, average = 1, build time = 3");
        buildWrapper.setDelaySecondsForTest(3);
        build = project.scheduleBuild2(0).get();
        assertBuildStatus(Result.FAILURE, build);


        //build 6
        System.out.println("toFail = false, build to average = 3, threshold = 100, hard-coded = 0, average = 2, build time = 3");
        buildWrapper.failBuild = false;
        buildWrapper.setDelaySecondsForTest(3);
        build = project.scheduleBuild2(0).get();
        assertBuildStatus(Result.UNSTABLE, build);

        //build 7
        System.out.println("toFail = false, build to average = 3, threshold = 300, hard-coded = 0, average = 2, build time = 2");
        buildWrapper.thresholdPercentage = 300;
        buildWrapper.setDelaySecondsForTest(2);
        build = project.scheduleBuild2(0).get();
        assertBuildStatus(Result.SUCCESS, build);

        //build 8
        System.out.println("toFail = false, build to average = 3, threshold = 100, hard-coded = 1, average = 2, build time = 2");
        buildWrapper.timeoutSeconds = 1;
        buildWrapper.setDelaySecondsForTest(2);
        build = project.scheduleBuild2(0).get();
        assertBuildStatus(Result.UNSTABLE, build);

    }
}
