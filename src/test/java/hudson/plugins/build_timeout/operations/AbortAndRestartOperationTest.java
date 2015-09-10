package hudson.plugins.build_timeout.operations;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;

import org.junit.Rule;
import org.junit.Test;
import org.jvnet.hudson.test.JenkinsRule;
import org.jvnet.hudson.test.recipes.LocalData;
import org.jvnet.hudson.test.recipes.WithPlugin;

import hudson.model.AbstractProject;
import hudson.model.Cause;
import hudson.model.FreeStyleProject;
import hudson.model.Result;
import hudson.plugins.build_timeout.BuildTimeOutJenkinsRule;
import hudson.plugins.build_timeout.BuildTimeOutOperationDescriptor;

public class AbortAndRestartOperationTest {
    
//    @Rule
//    public BuildTimeOutJenkinsRule j = new BuildTimeOutJenkinsRule();
    
    @Rule
    public JenkinsRule j = new JenkinsRule();

    @Test
    @WithPlugin("naginator")
    @LocalData
    public void testAbortAndRestart() throws Exception {
                
        AbstractProject<?,?> testProject = (AbstractProject<?,?>)j.getInstance().getItem("Test01");
        
        Cause cause = new Cause.UserIdCause();
        assertEquals(testProject.getBuilds().size(), 0);
        testProject.scheduleBuild(cause);
        Thread.sleep(20000); //3 minutes + buffer
        assertEquals(testProject.getFirstBuild().getResult(), Result.ABORTED);
        Thread.sleep(20000); //3 minutes + buffer 
        assertEquals(testProject.getLastBuild().getResult(), Result.ABORTED);
        assertEquals(testProject.getBuilds().size(), 2);
    }
}
