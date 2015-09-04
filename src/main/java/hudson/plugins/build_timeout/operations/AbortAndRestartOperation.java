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

import org.kohsuke.stapler.DataBoundConstructor;
import hudson.Extension;
import hudson.model.AbstractBuild;
import hudson.model.Cause;
import hudson.model.BuildListener;
import hudson.model.ParametersAction;
import hudson.model.Run;
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
        
        new AbortOperation().perform(build, listener, effectiveTimeout);
                
        if (checkMaxRestarts(build)) {

            ParametersAction action = build.getAction(ParametersAction.class);           
            build.getRootBuild().getProject().scheduleBuild(0, new BuildTimeoutAbortAndRestartCause(build), action);
        }
        return true;
    }
    
    public class BuildTimeoutAbortAndRestartCause extends Cause {
        

        Run<?, ?> build;
        
        /**
         * Constructor.
         * 
         * @param s
         *            The reason/cause for restart.
         */
        public BuildTimeoutAbortAndRestartCause(Run<?, ?> up) {
            super();
            this.build = up;
        }
        
        @Override
        public String getShortDescription() {
            return "Build Timeout - Abort and Restart: Aborted by build no.: " + build.getNumber();
        
        }
    }
    
    private boolean checkMaxRestarts(AbstractBuild<?, ?> build) {
        if (this.maxRestarts <= 0) {
            return true;
        }
        int count = 0;

        // count the number of restarts for the current project
        while (build != null) {
                                              
            if(build.getCause(BuildTimeoutAbortAndRestartCause.class) != null){
                count++;
                System.out.println("BuildTimeoutAbortAndRestartCause != null");
                System.out.println("count: " + count);
            }
         
            if (count >= this.maxRestarts) {
                return false;
            }
            
            if(build.getCause(BuildTimeoutAbortAndRestartCause.class) != null){
                System.out.println("getPreviousBuild");
                build = build.getPreviousBuild();
            }else{
                System.out.println("break");
                break;
            }
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
