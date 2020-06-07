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

import static org.junit.Assert.*;

import java.util.Arrays;

import hudson.model.FreeStyleBuild;
import hudson.model.Cause;
import hudson.model.FreeStyleProject;
import hudson.model.ParametersAction;
import hudson.model.ParametersDefinitionProperty;
import hudson.model.StringParameterDefinition;
import hudson.model.StringParameterValue;
import hudson.plugins.build_timeout.BuildTimeOutOperation;
import hudson.plugins.build_timeout.BuildTimeoutWrapper;
import hudson.plugins.build_timeout.operations.AbortOperation;

import org.junit.Rule;
import org.junit.Test;
import org.jvnet.hudson.test.JenkinsRule;
import org.jvnet.hudson.test.JenkinsRule.WebClient;
import org.jvnet.hudson.test.CaptureEnvironmentBuilder;

import com.gargoylesoftware.htmlunit.html.HtmlForm;
import com.gargoylesoftware.htmlunit.html.HtmlPage;

/**
 * Tests for {@link ElasticTimeOutStrategy} using Jenkins
 */
public class ElasticTimeOutStrategyJenkinsTest {

    @Rule
    public JenkinsRule j = new JenkinsRule();

    @Test
    public void testCanConfigureWithWebPage() throws Exception {
        FreeStyleProject p = j.createFreeStyleProject();
        p.getBuildWrappersList().add(
                new BuildTimeoutWrapper(
                        new ElasticTimeOutStrategy("300", "3", "10"),
                        Arrays.<BuildTimeOutOperation>asList(new AbortOperation()),
                        null
                )
        );
        p.save();
        
        String projectName = p.getFullName();
        
        // test configuration before configure on configuration page.
        {
            ElasticTimeOutStrategy strategy = (ElasticTimeOutStrategy)p.getBuildWrappersList().get(BuildTimeoutWrapper.class).getStrategy();
            assertEquals("300", strategy.getTimeoutPercentage());
            assertEquals("3", strategy.getTimeoutMinutesElasticDefault());
            assertEquals("10", strategy.getNumberOfBuilds());
        }
        
        // reconfigure.
        // This should preserve configuration.
        WebClient wc = j.createWebClient();
        HtmlPage page = wc.getPage(p, "configure");
        HtmlForm form = page.getFormByName("config");
        j.submit(form);
        p = j.jenkins.getItemByFullName(projectName, FreeStyleProject.class);
        
        // test configuration before configure on configuration page.
        {
            ElasticTimeOutStrategy strategy = (ElasticTimeOutStrategy)p.getBuildWrappersList().get(BuildTimeoutWrapper.class).getStrategy();
            assertEquals("300", strategy.getTimeoutPercentage());
            assertEquals("3", strategy.getTimeoutMinutesElasticDefault());
            assertEquals("10", strategy.getNumberOfBuilds());
        }
    }
    
    @Test
    public void testFailSafeTimeoutWithVariable() throws Exception {
        FreeStyleProject p = j.createFreeStyleProject();
        // needed since Jenkins 2.3
        p.addProperty(new ParametersDefinitionProperty(new StringParameterDefinition("FailSafeTimeout", null)));
        p.getBuildWrappersList().add(new BuildTimeoutWrapper(
                new ElasticTimeOutStrategy("200", "${FailSafeTimeout}", "3", true),
                null,
                "TIMEOUT"
        ));
        CaptureEnvironmentBuilder ceb = new CaptureEnvironmentBuilder();
        p.getBuildersList().add(ceb);
        
        FreeStyleBuild b = j.assertBuildStatusSuccess(
                p.scheduleBuild2(
                        0,
                        new Cause.UserIdCause(),
                        new ParametersAction(new StringParameterValue(
                                "FailSafeTimeout",
                                "30",   // 30 minutes
                                ""
                        ))
               )
        );
        
        assertEquals(
                "1800000",      // value specified with FailSafeTimeout
                ceb.getEnvVars().get("TIMEOUT")
        );
    }
}
