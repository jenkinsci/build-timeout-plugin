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

import hudson.model.BuildListener;
import org.jenkinsci.plugins.tokenmacro.MacroEvaluationException;
import org.kohsuke.stapler.DataBoundConstructor;

import hudson.Extension;
import hudson.model.AbstractBuild;
import hudson.plugins.build_timeout.BuildTimeOutStrategy;
import hudson.plugins.build_timeout.BuildTimeOutStrategyDescriptor;
import hudson.plugins.build_timeout.BuildTimeoutWrapper;

import edu.umd.cs.findbugs.annotations.NonNull;
import java.io.IOException;

/**
 * Timeout when specified time passed since the last output.
 */
public class NoActivityTimeOutStrategy extends BuildTimeOutStrategy {
    @Deprecated
    private transient long timeout;
    
    private final String timeoutSecondsString;
    
    /**
     * @deprecated use {@link NoActivityTimeOutStrategy#getTimeoutSecondsString()} instead.
     */
    @Deprecated
    public long getTimeoutSeconds() {
        try {
            return Long.parseLong(timeoutSecondsString);
        } catch(NumberFormatException e) {
            return 0L;
        }
    }
    
    public String getTimeoutSecondsString()
    {
        return timeoutSecondsString;
    }
    
    private Object readResolve() {
        if(timeoutSecondsString == null) {
            return new NoActivityTimeOutStrategy(this.timeout / 1000L);
        }
        
        return this;
    }
    
    @DataBoundConstructor
    public NoActivityTimeOutStrategy(String timeoutSecondsString) {
        this.timeoutSecondsString = timeoutSecondsString;
    }
    
    @Deprecated
    public NoActivityTimeOutStrategy(long timeoutSeconds) {
        this(Long.toString(timeoutSeconds));
    }
    
    @Override
    public long getTimeOut(@NonNull AbstractBuild<?, ?> build, @NonNull BuildListener listener)
            throws InterruptedException, MacroEvaluationException, IOException {
        return Long.parseLong(expandAll(build, listener, getTimeoutSecondsString())) * 1000L;
    }

    @Override
    public void onWrite(AbstractBuild<?,?> build, byte b[], int length) {
        BuildTimeoutWrapper.EnvironmentImpl env = build.getEnvironments().get(BuildTimeoutWrapper.EnvironmentImpl.class);
        if (env != null) {
            env.rescheduleIfScheduled();
        }
    }
    
    @Extension
    public static class DescriptorImpl extends BuildTimeOutStrategyDescriptor {
        @Override
        public String getDisplayName() {
            return Messages.NoActivityTimeOutStrategy_DisplayName();
        }
    }
}
