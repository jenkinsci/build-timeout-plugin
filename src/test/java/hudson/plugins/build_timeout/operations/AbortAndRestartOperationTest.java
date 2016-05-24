/*
 * The MIT License
 * 
 * Copyright (c) 2015 Jochen A. Fuerbacher
 * 
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 * 
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 * 
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */

package hudson.plugins.build_timeout.operations;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.util.LinkedList;

import org.junit.Rule;
import org.junit.Test;
import org.jvnet.hudson.test.SleepBuilder;

import hudson.model.Cause;
import hudson.model.FreeStyleProject;
import hudson.model.Result;
import hudson.plugins.build_timeout.*;
import hudson.plugins.build_timeout.BuildTimeOutJenkinsRule;
import hudson.plugins.build_timeout.BuildTimeoutWrapper;


public class AbortAndRestartOperationTest {
    
    @Rule
    public BuildTimeOutJenkinsRule j = new BuildTimeOutJenkinsRule();

    @Test
    public void testAbortAndRestartOnce() throws Exception {
        
        FreeStyleProject testproject = j.createFreeStyleProject();

        QuickBuildTimeOutStrategy strategy = new QuickBuildTimeOutStrategy(5000);
        AbortAndRestartOperation operation = new AbortAndRestartOperation("1"); //Number of restarts
        LinkedList<BuildTimeOutOperation> list = new LinkedList<BuildTimeOutOperation>();
        list.add(operation);
        
        BuildTimeoutWrapper wrapper = new BuildTimeoutWrapper(strategy,list,"");
        testproject.getBuildWrappersList().add(wrapper);
        
        
        testproject.getBuildersList().add(new SleepBuilder(5*60*1000)); //5 minutes
                       
        
        testproject.scheduleBuild(new Cause.UserIdCause());

        j.waitUntilNoActivityUpTo(25000);
            
        assertNotNull(testproject.getFirstBuild());
        assertFalse(testproject.getFirstBuild().equals(testproject.getLastBuild()));
        assertEquals(testproject.getBuilds().size(), 2);
        
        assertEquals(Result.ABORTED, testproject.getFirstBuild().getResult());
        assertEquals(Result.ABORTED, testproject.getLastBuild().getResult());
    }
    
    @Test
    public void testAbortAndRestartTwice() throws Exception {
        
        FreeStyleProject testproject = j.createFreeStyleProject();

        QuickBuildTimeOutStrategy strategy = new QuickBuildTimeOutStrategy(5000);
        AbortAndRestartOperation operation = new AbortAndRestartOperation("2"); //Number of restarts
        LinkedList<BuildTimeOutOperation> list = new LinkedList<BuildTimeOutOperation>();
        list.add(operation);
        
        BuildTimeoutWrapper wrapper = new BuildTimeoutWrapper(strategy,list,"");
        testproject.getBuildWrappersList().add(wrapper);
        

        testproject.getBuildersList().add(new SleepBuilder(5*60*1000)); //5 minutes
        
        assertTrue(testproject.getBuilds().size()==0);
                
        
        testproject.scheduleBuild(new Cause.UserIdCause());
        
        j.waitUntilNoActivityUpTo(25000);
        
        assertNotNull(testproject.getFirstBuild());
        assertFalse(testproject.getFirstBuild().equals(testproject.getLastBuild()));
        assertEquals(testproject.getBuilds().size(), 3);
        
        assertEquals(Result.ABORTED, testproject.getBuildByNumber(1).getResult());
        assertEquals(Result.ABORTED, testproject.getBuildByNumber(2).getResult());
        assertEquals(Result.ABORTED, testproject.getBuildByNumber(3).getResult());
    }
}

