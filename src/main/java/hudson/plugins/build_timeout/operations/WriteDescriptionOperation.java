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

import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static java.util.concurrent.TimeUnit.MINUTES;

import java.io.IOException;
import java.text.MessageFormat;

import org.kohsuke.stapler.DataBoundConstructor;

import hudson.Extension;
import hudson.model.AbstractBuild;
import hudson.model.BuildListener;
import hudson.plugins.build_timeout.BuildTimeOutOperation;
import hudson.plugins.build_timeout.BuildTimeOutOperationDescriptor;

/**
 * Set Description for the build timed out.
 */
public class WriteDescriptionOperation extends BuildTimeOutOperation {
    private final String description;
    
    /**
     * @return description to set.
     */
    public String getDescription() {
        return description;
    }
    
    @DataBoundConstructor
    public WriteDescriptionOperation(String description) {
        this.description = description;
    }
    
    @Override
    public boolean perform(AbstractBuild<?, ?> build, BuildListener listener, long effectiveTimeout) {
        // timed out
        long effectiveTimeoutMinutes = MINUTES.convert(effectiveTimeout,MILLISECONDS);
        String msg = getDescription();
        try {
            msg = build.getEnvironment(listener).expand(msg);
        } catch (IOException e) {
            listener.getLogger().println(String.format("failed to expand string: %s", msg));
            e.printStackTrace(listener.getLogger());
        } catch (InterruptedException e) {
            listener.getLogger().println(String.format("failed to expand string: %s", msg));
            e.printStackTrace(listener.getLogger());
        }
        
        msg = MessageFormat.format(msg, effectiveTimeoutMinutes);
        
        try {
            build.setDescription(msg);
        } catch (IOException e) {
            listener.getLogger().println("failed to write to the build description!");
            e.printStackTrace(listener.getLogger());
        }
        
        return true;
    }
    
    @Extension
    public static class DescriptorImpl extends BuildTimeOutOperationDescriptor {
        @Override
        public String getDisplayName() {
            return Messages.WriteDescriptionOperation_DisplayName();
        }
    }
}
