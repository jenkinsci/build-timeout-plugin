/*
 * The MIT License
 * 
 * Copyright (c) 2014 IKEDA Yasuyuki
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

import static org.junit.Assert.*;

import java.util.Arrays;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.jvnet.hudson.test.SleepBuilder;

import hudson.model.FreeStyleBuild;
import hudson.model.FreeStyleProject;
import hudson.model.Result;
import hudson.plugins.build_timeout.BuildTimeOutJenkinsRule;
import hudson.plugins.build_timeout.BuildTimeOutOperation;
import hudson.plugins.build_timeout.QuickBuildTimeOutStrategy;
import hudson.plugins.build_timeout.BuildTimeoutWrapper;

/**
 *
 */
public class WriteDescriptionOperationTest {
    @Rule
    public BuildTimeOutJenkinsRule j = new BuildTimeOutJenkinsRule();
    
    @Before
    public void setUp() {
        BuildTimeoutWrapper.MINIMUM_TIMEOUT_MILLISECONDS = 0;
    }
    
    @Test
    public void testSetDescription() throws Exception {
        final String DESCRIPTION = "description to test: {0}, {0}.";
        final String EXPECTED = "description to test: 0, 0.";
        
        FreeStyleProject p = j.createFreeStyleProject();
        p.getBuildWrappersList().add(new BuildTimeoutWrapper(
                new QuickBuildTimeOutStrategy(5000),
                Arrays.<BuildTimeOutOperation>asList(
                        new WriteDescriptionOperation(DESCRIPTION),
                        new AbortOperation()
                ),
                "BUILD_TIMEOUT"
        ));
        p.getBuildersList().add(new SleepBuilder(10000));
        
        FreeStyleBuild b = p.scheduleBuild2(0).get();
        j.assertBuildStatus(Result.ABORTED, b);
        
        assertEquals(EXPECTED, b.getDescription());
    }
    
    @Test
    public void testSetDescriptionWithoutAborting() throws Exception {
        final String DESCRIPTION = "description to test: {0}, {0}.";
        final String EXPECTED = "description to test: 0, 0.";
        
        FreeStyleProject p = j.createFreeStyleProject();
        p.getBuildWrappersList().add(new BuildTimeoutWrapper(
                new QuickBuildTimeOutStrategy(5000),
                Arrays.<BuildTimeOutOperation>asList(
                        new WriteDescriptionOperation(DESCRIPTION)
                ),
                "BUILD_TIMEOUT"
        ));
        p.getBuildersList().add(new SleepBuilder(10000));
        
        FreeStyleBuild b = p.scheduleBuild2(0).get();
        j.assertBuildStatusSuccess(b);
        
        assertEquals(EXPECTED, b.getDescription());
    }
    
    @Test
    public void testSetDescriptionTwice() throws Exception {
        final String DESCRIPTION1 = "description to test: {0}, {0}.";
        final String DESCRIPTION2 = "Another message.";
        final String EXPECTED = "Another message.";
        
        FreeStyleProject p = j.createFreeStyleProject();
        p.getBuildWrappersList().add(new BuildTimeoutWrapper(
                new QuickBuildTimeOutStrategy(5000),
                Arrays.<BuildTimeOutOperation>asList(
                        new WriteDescriptionOperation(DESCRIPTION1),
                        new AbortOperation(),
                        new WriteDescriptionOperation(DESCRIPTION2)
                ),
                "BUILD_TIMEOUT"
        ));
        p.getBuildersList().add(new SleepBuilder(10000));
        
        FreeStyleBuild b = p.scheduleBuild2(0).get();
        j.assertBuildStatus(Result.ABORTED, b);
        
        assertEquals(EXPECTED, b.getDescription());
    }
}
