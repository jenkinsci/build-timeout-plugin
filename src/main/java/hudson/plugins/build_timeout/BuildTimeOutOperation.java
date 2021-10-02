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

package hudson.plugins.build_timeout;

import jenkins.model.Jenkins;
import hudson.ExtensionPoint;
import hudson.model.AbstractBuild;
import hudson.model.Action;
import hudson.model.BuildListener;
import hudson.model.Describable;

import javax.annotation.Nonnull;

/**
 * Defines an operation performed when timeout occurs.
 * They are called "Timeout Actions", but the class is BuildTimeOutOperation
 * not to be confused with {@link Action}
 */
public abstract class BuildTimeOutOperation
        implements ExtensionPoint, Describable<BuildTimeOutOperation> {
    
    /**
     * Perform operation.
     * 
     * @param build             build timed out
     * @param listener          build listener. can be used to print log.
     * @param effectiveTimeout  timeout (milliseconds)
     * @return false not to run subsequent operations. It also mark the build as failure.
     */
    public abstract boolean perform(@Nonnull AbstractBuild<?,?> build, @Nonnull BuildListener listener, long effectiveTimeout);
    
    /**
     * @see hudson.model.Describable#getDescriptor()
     */
    public BuildTimeOutOperationDescriptor getDescriptor() {
        return (BuildTimeOutOperationDescriptor)Jenkins.getActiveInstance().getDescriptorOrDie(getClass());
    }

    public void addAction(@Nonnull AbstractBuild<?,?> build, @Nonnull String reason) {
        BuildTimeOutAction buildTimeoutAction = build.getAction(BuildTimeOutAction.class);
        if(buildTimeoutAction == null) {
            buildTimeoutAction = new BuildTimeOutAction(reason);
        }
        build.addAction(buildTimeoutAction);
    }
}
