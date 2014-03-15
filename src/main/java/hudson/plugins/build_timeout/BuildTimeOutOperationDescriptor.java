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

import java.util.ArrayList;
import java.util.List;

import jenkins.model.Jenkins;
import hudson.model.AbstractProject;
import hudson.model.Descriptor;
import hudson.tasks.BuildStepDescriptor;

/**
 * Descriptor for {@link BuildTimeOutOperation}
 */
public abstract class BuildTimeOutOperationDescriptor extends Descriptor<BuildTimeOutOperation> {
    /**
     * Returns true if this task is applicable to the given project.
     * 
     * Override this to restrict project types this action can be applied.
     * 
     * @param jobType
     * @return
     *      true to allow user to configure this timeout action to given project.
     * @see BuildStepDescriptor#isApplicable(Class)
     */
    public boolean isApplicable(Class<? extends AbstractProject<?,?>> jobType) {
        return true;
    }
    
    public static List<BuildTimeOutOperationDescriptor> all(Class<? extends AbstractProject<?,?>> jobType) {
        List<BuildTimeOutOperationDescriptor> alldescs = Jenkins.getInstance().getDescriptorList(BuildTimeOutOperation.class);
        List<BuildTimeOutOperationDescriptor> descs = new ArrayList<BuildTimeOutOperationDescriptor>();
        for (BuildTimeOutOperationDescriptor d: alldescs) {
            if (d.isApplicable(jobType)) {
                descs.add(d);
            }
        }
        return descs;
    }
}
