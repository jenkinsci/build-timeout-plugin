/*
 * The MIT License
 * 
 * Copyright (c) 2015 Stefan Brausch, Jochen A. Fuerbacher
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

import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.kohsuke.stapler.DataBoundConstructor;

import hudson.Extension;
import hudson.model.AbstractBuild;
import hudson.model.AbstractProject;
import hudson.model.Executor;
import hudson.model.Result;
import hudson.model.BuildListener;
import hudson.plugins.build_timeout.BuildTimeOutOperation;
import hudson.plugins.build_timeout.BuildTimeOutOperationDescriptor;

import com.chikli.hudson.plugin.naginator.FixedDelay;

/**
 * Abort the build.
 */
public class AbortAndRestartOperation extends BuildTimeOutOperation {
    
    private final String maxRestarts;
    
    private static final Logger log = Logger.getLogger(AbortAndRestartOperation.class.getName());
    
    /**
     * @return max restarts.
     */
    public String getMaxRestarts() {
        return maxRestarts;
    }
        
    @DataBoundConstructor
    public AbortAndRestartOperation(String maxRestarts){
        this.maxRestarts = maxRestarts;
    }
    
    private static boolean isPresent() {
        try {
            Class.forName("com.chikli.hudson.plugin.naginator.NaginatorScheduleAction");
            return true;
        } catch (ClassNotFoundException ex) {
            log.log(Level.WARNING, "Naginator not available. ", ex);
            return false;
        }
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
        
        if(isPresent()){
            FixedDelay sd = new FixedDelay(0); //Reschedule now!
            int maxRestarts = 0;
            try {
                maxRestarts = Integer.parseInt(build.getEnvironment(listener).expand(this.maxRestarts));
                build.addAction(new com.chikli.hudson.plugin.naginator.NaginatorScheduleAction(maxRestarts, sd, false));
            } catch (IOException e1) {
                log.log(Level.WARNING, "Failed to expand environment variables. ", e1);
            } catch (InterruptedException e1) {
                log.log(Level.WARNING, "Failed to expand environment variables. ", e1);
            }
        }
        return true;
    }
        
    @Extension 
    public static class DescriptorImpl extends BuildTimeOutOperationDescriptor {
        @Override
        public String getDisplayName() {
            return Messages.AbortAndRestartOperation_DisplayName();
        }
        
        @Override
        public boolean isApplicable(Class<? extends AbstractProject<?,?>> jobType) {
            return true;
        }
    }
}
