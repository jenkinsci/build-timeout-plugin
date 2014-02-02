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

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import hudson.Launcher;
import hudson.model.AbstractBuild;
import hudson.model.FreeStyleProject;
import hudson.model.BuildListener;
import hudson.model.Result;
import hudson.plugins.build_timeout.BuildTimeOutJenkinsRule;
import hudson.plugins.build_timeout.BuildTimeoutWrapper;
import hudson.tasks.Builder;

/**
 *
 */
public class NoActivityTimeOutStrategyTest {
    @Rule
    public BuildTimeOutJenkinsRule j = new BuildTimeOutJenkinsRule();
    
    private long origTimeout = 0;
    
    @Before
    public void before() {
        origTimeout = BuildTimeoutWrapper.MINIMUM_TIMEOUT_MILLISECONDS;
        BuildTimeoutWrapper.MINIMUM_TIMEOUT_MILLISECONDS = 0;
    }
    
    @After
    public void after() {
        BuildTimeoutWrapper.MINIMUM_TIMEOUT_MILLISECONDS = origTimeout;
    }
    
    public static class PollingBuilder extends Builder {
        private final long pollingMilliseconds;
        private final long exitMilliseconds;
        
        public PollingBuilder(long pollingMilliseconds, long exitMilliseconds) {
            this.pollingMilliseconds = pollingMilliseconds;
            this.exitMilliseconds = exitMilliseconds;
        }
        
        private void log(BuildListener listener, long cur, String message) {
            String str = String.format(
                    "[%s] %s",
                    (new SimpleDateFormat("HH:mm:ss.SSS")).format(new Date(cur)),
                    message
            );
            listener.getLogger().println(str);
            System.out.println(str);
        }
        
        @Override
        public boolean perform(AbstractBuild<?, ?> build, Launcher launcher,
                BuildListener listener) throws InterruptedException,
                IOException {
            long startAt = System.currentTimeMillis();
            long pollAt = startAt + pollingMilliseconds;
            long exitAt = startAt + exitMilliseconds;
            
            log(listener, startAt, "----start----");
            
            while (true) {
                Thread.sleep(10);
                long cur = System.currentTimeMillis();
                if (pollAt < cur) {
                    log(listener, cur, "----polling----");
                    pollAt += pollingMilliseconds;
                }
                if (exitAt < cur) {
                    log(listener, cur, "----exit----");
                    break;
                }
            }
            
            return true;
        }
    }
    
    @Test
    public void testTimeout() throws Exception {
        FreeStyleProject p = j.createFreeStyleProject();
        p.getBuildWrappersList().add(new BuildTimeoutWrapper(
                new NoActivityTimeOutStrategy(5),
                true,
                false
        ));
        p.getBuildersList().add(new PollingBuilder(10 * 1000, 30 * 1000));
        
        j.assertBuildStatus(Result.FAILURE, p.scheduleBuild2(0).get());
    }
    
    @Test
    public void testNoTimeout() throws Exception {
        FreeStyleProject p = j.createFreeStyleProject();
        p.getBuildWrappersList().add(new BuildTimeoutWrapper(
                new NoActivityTimeOutStrategy(15),
                true,
                false
        ));
        p.getBuildersList().add(new PollingBuilder(10 * 1000, 30 * 1000));
        
        j.assertBuildStatusSuccess(p.scheduleBuild2(0));
    }
}
