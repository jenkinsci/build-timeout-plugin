package hudson.plugins.build_timeout.operations;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.awt.List;
import java.util.LinkedList;

import org.junit.Rule;
import org.junit.Test;
import org.jvnet.hudson.test.JenkinsRule;
import org.jvnet.hudson.test.recipes.LocalData;

import hudson.model.Cause;
import hudson.model.FreeStyleBuild;
import hudson.model.FreeStyleProject;
import hudson.model.Result;
import hudson.plugins.build_timeout.*;
import hudson.plugins.build_timeout.BuildTimeOutJenkinsRule;
import hudson.plugins.build_timeout.BuildTimeOutOperationDescriptor;
import hudson.plugins.build_timeout.BuildTimeoutWrapper;
import hudson.plugins.build_timeout.impl.AbsoluteTimeOutStrategy;
import hudson.tasks.Builder;
import hudson.tasks.BatchFile;

public class AbortAndRestartOperationTest {
    
    @Rule
    public BuildTimeOutJenkinsRule j = new BuildTimeOutJenkinsRule();

    @Test
    public void testAbortAndRestart() throws Exception {
        
        FreeStyleProject testproject = j.createFreeStyleProject();

        AbsoluteTimeOutStrategy strategy = new AbsoluteTimeOutStrategy(Integer.toString(3)); //timeoutMinutes
        AbortAndRestartOperation operation = new AbortAndRestartOperation(1); //Number of restarts
        LinkedList<BuildTimeOutOperation> list = new LinkedList<BuildTimeOutOperation>();
        list.add(operation);
        
        BuildTimeoutWrapper wrapper = new BuildTimeoutWrapper(strategy,list,"");
        testproject.getBuildWrappersList().add(wrapper);
        
        
        BatchFile builder = new BatchFile("ping -n 500 127.0.0.1 &amp;gt;nul");
        testproject.getBuildersList().add(builder);
        
        assertTrue(testproject.getBuilds().size()==0);
                
        
  

        FreeStyleBuild build = testproject.scheduleBuild2(0,new Cause.UserIdCause()).get();
        j.assertBuildStatus(Result.ABORTED, build);
//        Thread.sleep((3*60*1000)+20);
//        assertTrue(testproject.getFirstBuild() != null);
//        assertTrue(testproject.getFirstBuild().getResult() != null);
//        assertEquals(testproject.getFirstBuild().getResult(), Result.ABORTED);
        
        //j.assertBuildStatus(Result.ABORTED,testproject.scheduleBuild2(0, new Cause.UserIdCause()).get());
        
        //assertBuildStatus(Result.ABORTED, testproject.scheduleBuild2(0, new Cause.UserIdCause()).get());
        
//        Thread.sleep(150000);
//        assertTrue(testproject.getFirstBuild() != null);
//        assertTrue(testproject.getFirstBuild().getResult() != null);
//        assertEquals(testproject.getFirstBuild().getResult(), Result.ABORTED);
        //Thread.sleep(70000);
//        assertEquals(testproject.getLastBuild().getResult(), Result.ABORTED);
//        assertEquals(testproject.getBuilds().size(), 2);
    }
}
