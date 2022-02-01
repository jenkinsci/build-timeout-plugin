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

package hudson.plugins.build_timeout.impl;

import java.util.Arrays;

import hudson.model.ParametersDefinitionProperty;
import hudson.model.Result;
import hudson.model.Cause;
import hudson.model.FreeStyleProject;
import hudson.model.ParametersAction;
import hudson.model.StringParameterDefinition;
import hudson.model.StringParameterValue;
import hudson.plugins.build_timeout.BuildTimeOutOperation;
import hudson.plugins.build_timeout.BuildTimeoutWrapper;
import hudson.plugins.build_timeout.BuildTimeoutWrapperIntegrationTest;
import hudson.plugins.build_timeout.operations.AbortOperation;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.jvnet.hudson.test.JenkinsRule;
import org.jvnet.hudson.test.SleepBuilder;

/**
 * Tests for {@link AbsoluteTimeOutStrategy}.
 * Many tests for {@link AbsoluteTimeOutStrategy} are also in {@link BuildTimeoutWrapperIntegrationTest}
 */
public class AbsoluteTimeOutStrategyTest {

    @Rule
    public JenkinsRule j = new JenkinsRule();

    private long origTimeout = 0;
    
    @Before
    public void before() {
        // this allows timeout shorter than 3 minutes.
        origTimeout = BuildTimeoutWrapper.MINIMUM_TIMEOUT_MILLISECONDS;
        BuildTimeoutWrapper.MINIMUM_TIMEOUT_MILLISECONDS = 1000;
    }
    
    @After
    public void after() {
        BuildTimeoutWrapper.MINIMUM_TIMEOUT_MILLISECONDS = origTimeout;
    }
    
    @Test
    public void configurationWithParameter() throws Exception {
        FreeStyleProject p = j.createFreeStyleProject();
        // needed since Jenkins 2.3
        p.addProperty(new ParametersDefinitionProperty(new StringParameterDefinition("TIMEOUT", null)));
        p.getBuildWrappersList().add(
                new BuildTimeoutWrapper(
                        new AbsoluteTimeOutStrategy("${TIMEOUT}"),
                        Arrays.<BuildTimeOutOperation>asList(new AbortOperation()),
                        null
                )
        );
        p.getBuildersList().add(new SleepBuilder(5000));
        
        // If called with TIMEOUT=1, the build succeeds.
        j.assertBuildStatus(Result.SUCCESS, p.scheduleBuild2(
                0,
                new Cause.UserCause(),
                new ParametersAction(new StringParameterValue("TIMEOUT", "1"))
        ).get());
        
        // If called with TIMEOUT=0, the build is aborted immediately.
        j.assertBuildStatus(Result.ABORTED, p.scheduleBuild2(
                0,
                new Cause.UserCause(),
                new ParametersAction(new StringParameterValue("TIMEOUT", "0"))
        ).get());
    }
}
