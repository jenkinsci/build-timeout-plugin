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

import hudson.model.ParametersDefinitionProperty;
import hudson.model.Result;
import hudson.model.Cause;
import hudson.model.FreeStyleProject;
import hudson.model.ParametersAction;
import hudson.model.StringParameterDefinition;
import hudson.model.StringParameterValue;
import hudson.plugins.build_timeout.BuildTimeoutWrapper;
import hudson.plugins.build_timeout.operations.AbortOperation;

import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Calendar;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.jvnet.hudson.test.JenkinsRule;
import org.jvnet.hudson.test.SleepBuilder;
import org.jvnet.hudson.test.junit.jupiter.WithJenkins;

/**
 * Tests for {@link AbsoluteTimeOutStrategy}. Many tests for
 * {@link AbsoluteTimeOutStrategy} are also in
 * {@see BuildTimeoutWrapperIntegrationTest}
 */
@WithJenkins
class DeadlineTimeOutStrategyTest {

    private long origTimeout = 0;

    private static final int TOLERANCE_PERIOD_IN_MINUTES = 2;

    @BeforeEach
    void before() {
        // this allows timeout shorter than 3 minutes.
        origTimeout = BuildTimeoutWrapper.MINIMUM_TIMEOUT_MILLISECONDS;
        BuildTimeoutWrapper.MINIMUM_TIMEOUT_MILLISECONDS = 1000;
    }

    @AfterEach
    void after() {
        BuildTimeoutWrapper.MINIMUM_TIMEOUT_MILLISECONDS = origTimeout;
    }

    @Test
    void configurationWithParameter(JenkinsRule j) throws Exception {
        // Deadline in next three seconds. Job should be aborted in three seconds after start
        testWithParam(3, Result.ABORTED, j);

        // Deadline defined as a past time but inside tolerance period. Job should be aborted immediately
        testWithParam(-TOLERANCE_PERIOD_IN_MINUTES * 60 / 2, Result.ABORTED, j);

        // Deadline defined as a past time outside tolerance period, so effective deadline will be tomorrow. Job should be executed normally.
        testWithParam(-TOLERANCE_PERIOD_IN_MINUTES * 60 * 2, Result.SUCCESS, j);
    }

    @SuppressWarnings("deprecation")
    private void testWithParam(int timeToDeadlineInSecondsFromNow, Result expectedResult, JenkinsRule j) throws Exception {
        String deadline = getDeadlineTimeFromNow(timeToDeadlineInSecondsFromNow);

        FreeStyleProject p = j.createFreeStyleProject();
        // needed since Jenkins 2.3
        p.addProperty(new ParametersDefinitionProperty(new StringParameterDefinition("DEADLINE", null)));
        p.getBuildWrappersList().add(
                new BuildTimeoutWrapper(new DeadlineTimeOutStrategy("${DEADLINE}",
                        TOLERANCE_PERIOD_IN_MINUTES), Arrays.asList(new AbortOperation()), null));
        p.getBuildersList().add(new SleepBuilder(5000));

        j.assertBuildStatus(expectedResult, p.scheduleBuild2(0, new Cause.UserCause(),
                new ParametersAction(new StringParameterValue("DEADLINE", deadline))).get());
    }

    private String getDeadlineTimeFromNow(int offsetInSeconds) {
        Calendar deadline = Calendar.getInstance();
        deadline.add(Calendar.SECOND, offsetInSeconds);

        return new SimpleDateFormat(DeadlineTimeOutStrategy.TIME_LONG_FORMAT_PATTERN).format(deadline.getTime());
    }
}
