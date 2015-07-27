/*
 * The MIT License
 * 
 * Copyright (c) 2015 Stefan Brausch
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

import static hudson.util.TimeUnit2.MILLISECONDS;
import static hudson.util.TimeUnit2.MINUTES;

import org.kohsuke.stapler.DataBoundConstructor;

import hudson.Extension;
import hudson.model.AbstractBuild;
import hudson.model.Cause;
import hudson.model.BuildListener;
import hudson.model.Executor;
import hudson.model.Result;
import hudson.plugins.build_timeout.BuildTimeOutOperation;
import hudson.plugins.build_timeout.BuildTimeOutOperationDescriptor;

/**
 * Abort the build.
 */
public class AbortAndRestartOperation extends BuildTimeOutOperation {
    private final int maxRestarts;
    
    /**
     * @return max restarts.
     */
    public int getMaxRestarts() {
        return maxRestarts;
    }
    
    @DataBoundConstructor
    public AbortAndRestartOperation(int maxRestarts){
        this.maxRestarts = maxRestarts;
    }
       
    /**
     * @param build
     * @param listener
     * @param effectiveTimeout
     * @return
     * @see hudson.plugins.build_timeout.BuildTimeOutOperation#perform(hudson.model.AbstractBuild, hudson.model.BuildListener, long)
     */
    @Override
    public boolean perform(AbstractBuild<?, ?> build, BuildListener listener, long effectiveTimeout) {
        long effectiveTimeoutMinutes = MINUTES.convert(effectiveTimeout,MILLISECONDS);
        // Use messages in hudson.plugins.build_timeout.Messages for historical reason.
        listener.getLogger().println(hudson.plugins.build_timeout.Messages.Timeout_Message(
                effectiveTimeoutMinutes,
                hudson.plugins.build_timeout.Messages.Timeout_Aborted())
        );
        
        Executor e = build.getExecutor();
        if (e != null) {
            e.interrupt(Result.ABORTED);
        }
        
        if (checkMaxRestarts(build)) {
            build.getRootBuild().getProject().scheduleBuild(new BuildTimeoutAbortAndRestartCause());
        }
        return true;
    }
    
    public class BuildTimeoutAbortAndRestartCause extends Cause {
        

        /**
         * Constructor.
         * 
         * @param s
         *            The reason/cause for restart.
         */
        public BuildTimeoutAbortAndRestartCause() {
            super();
            
        }

        @Override
        public String getShortDescription() {
            return "Build Timeout - Abort and Restart";
        
        }
    }
    
    private boolean checkMaxRestarts(AbstractBuild<?, ?> build) {
        if (this.maxRestarts <= 0) {
            return true;
        }
        int count = 0;

        // count the number of restarts for the current project
        while (build != null && build.getCause(BuildTimeoutAbortAndRestartCause.class) != null) {
            
            count++;
         
            if (count >= this.maxRestarts) {
                return false;
            }
            build = build.getPreviousBuild();
        }
        return true;
    }
    @Extension 
    public static class DescriptorImpl extends BuildTimeOutOperationDescriptor {
        @Override
        public String getDisplayName() {
            return "Abort and restart the build";//Messages.AbortAndRestartOperation_DisplayName();
        }
    }
}
