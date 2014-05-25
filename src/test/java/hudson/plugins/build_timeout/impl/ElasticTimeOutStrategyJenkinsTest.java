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

import hudson.model.FreeStyleProject;
import hudson.plugins.build_timeout.BuildTimeOutJenkinsRule;
import hudson.plugins.build_timeout.BuildTimeOutOperation;
import hudson.plugins.build_timeout.BuildTimeoutWrapper;
import hudson.plugins.build_timeout.operations.AbortOperation;

import org.junit.Rule;
import org.junit.Test;
import org.jvnet.hudson.test.JenkinsRule.WebClient;

import com.gargoylesoftware.htmlunit.html.HtmlForm;
import com.gargoylesoftware.htmlunit.html.HtmlPage;

/**
 * Tests for {@link ElasticTimeOutStrategy} using Jenkins
 */
public class ElasticTimeOutStrategyJenkinsTest {
    @Rule
    public BuildTimeOutJenkinsRule j = new BuildTimeOutJenkinsRule();
    
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
}
