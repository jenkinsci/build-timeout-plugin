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

import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import hudson.model.Node;
import net.sf.json.JSONObject;

import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.StaplerRequest;

import hudson.Extension;
import hudson.FilePath;
import hudson.Launcher;
import hudson.Proc;
import hudson.model.AbstractProject;
import hudson.model.BuildListener;
import hudson.model.BuildableItemWithBuildWrappers;
import hudson.model.AbstractBuild;
import hudson.model.Descriptor;
import hudson.model.Run.RunnerAbortedException;
import hudson.plugins.build_timeout.BuildTimeOutOperation;
import hudson.plugins.build_timeout.BuildTimeOutOperationDescriptor;
import hudson.plugins.build_timeout.BuildTimeOutUtility;
import hudson.remoting.Channel;
import hudson.tasks.BuildStep;
import hudson.tasks.BuildStepDescriptor;
import hudson.tasks.BuildWrapper;
import hudson.tasks.Builder;
import hudson.tasks.Publisher;

import javax.annotation.Nonnull;

/**
 * Timeout Action to perform any specified {@link BuildStep}, which includes {@link Builder} and {@link Publisher}.
 * 
 * This does not ensure all {@link BuildStep} works correct.
 * Especially, {@link BuildStep}s that launch processes on build nodes
 * does not work at all.
 */
public class BuildStepOperation extends BuildTimeOutOperation {
    private final BuildStep buildstep;
    private final boolean continueEvenFailed;
    private final boolean createLauncher;
    
    /**
     * @return build step to perform.
     */
    public BuildStep getBuildstep() {
        return buildstep;
    }
    
     /**
     * @return true to ignore build step failure.
     */
    public boolean isContinueEvenFailed() {
        return continueEvenFailed;
    }
    
    /**
     * @return whether to create a launcher for this build step
     */
    public boolean isCreateLauncher() {
        return createLauncher;
    }
    
    /**
     * @param buildstep
     * @param continueEvenFailed
     */
    @DataBoundConstructor
    public BuildStepOperation(BuildStep buildstep, boolean continueEvenFailed, boolean createLauncher) {
        this.buildstep = buildstep;
        this.continueEvenFailed = continueEvenFailed;
        this.createLauncher = createLauncher;
    }
    
    @Deprecated
    public BuildStepOperation(BuildStep buildstep, boolean continueEvenFailed) {
        this(buildstep, continueEvenFailed, false);
    }
    
    /**
     * {@link Launcher} that cannot launch anything.
     */
    private static class DummyLauncher extends Launcher {
        public DummyLauncher() {
            super(null, null);
        }
        
        @Override
        public void kill(Map<String, String> arg0) throws IOException, InterruptedException {
            throw new UnsupportedOperationException("Launcher does not supported in BuildStep timeout operation");
        }
        
        @Override
        public Proc launch(ProcStarter arg0) throws IOException {
            throw new UnsupportedOperationException("Launcher does not supported in BuildStep timeout operation");
        }
        
        @Override
        public Channel launchChannel(String[] arg0, OutputStream arg1,
                FilePath arg2, Map<String, String> arg3) throws IOException, InterruptedException {
            throw new UnsupportedOperationException("Launcher does not supported in BuildStep timeout operation");
        }
    }
    
    /**
     * @return launcher specified with launcherOption.
     */
    protected Launcher createLauncher(@Nonnull AbstractBuild<?, ?> build, @Nonnull BuildListener listener) {
        if(!isCreateLauncher()) {
            return new DummyLauncher();
        }

        Node builtOn = build.getBuiltOn();
        if (builtOn == null) {
            throw new IllegalStateException("current build's builtOn is null");
        }
        Launcher l = builtOn.createLauncher(listener);
        if (build.getParent() instanceof BuildableItemWithBuildWrappers) {
            BuildableItemWithBuildWrappers biwbw = (BuildableItemWithBuildWrappers)build.getParent();
            for (BuildWrapper bw : biwbw.getBuildWrappersList()) {
                try {
                    l = bw.decorateLauncher(build,l,listener);
                } catch(RunnerAbortedException|IOException|InterruptedException e) {
                    e.printStackTrace(listener.getLogger());
                }
            }
        }
        
        return l;
    }
    
    @Override
    public boolean perform(@Nonnull AbstractBuild<?, ?> build, @Nonnull BuildListener listener, long effectiveTimeout) {
        boolean result;
        try {
            result = getBuildstep().perform(build, createLauncher(build, listener), listener);
        } catch(InterruptedException|IOException e) {
            e.printStackTrace(listener.getLogger());
            result = false;
        }
        return isContinueEvenFailed() || result;
    }
    
    @Extension
    public static class DescriptorImpl extends BuildTimeOutOperationDescriptor {
        private boolean enabled = false;
        
        public DescriptorImpl() {
            load();
        }
        
        /**
         * Returns whether {@link BuildStepOperation} is enabled.
         * 
         * As {@link BuildStepOperation} does not ensure to work with all {@link BuildStep},
         * it is provided as an "advanced" feature, and disabled by default.
         * 
         * @return whether {@link BuildStepOperation} is enabled
         */
        public boolean isEnabled() {
            return enabled;
        }
        
        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }
        
        @Override
        public boolean configure(StaplerRequest req, JSONObject json)
                throws hudson.model.Descriptor.FormException {
            setEnabled(json.containsKey("enabled"));
            save();
            return true;
        }
        
        /**
         * Create a new instance from user input.
         * 
         * As there are builders or publishers who does not use {@link DataBoundConstructor},
         * it is required to handle to call {@link Descriptor#newInstance(StaplerRequest, JSONObject)}
         * manually.
         * 
         */
        @Override
        public BuildStepOperation newInstance(StaplerRequest req, JSONObject formData)
                throws hudson.model.Descriptor.FormException {
            BuildStep buildstep = BuildTimeOutUtility.bindJSONWithDescriptor(req, formData, "buildstep", BuildStep.class);
            boolean continueEvenFailed = formData.getBoolean("continueEvenFailed");
            boolean createLauncher = formData.getBoolean("createLauncher");
            return new BuildStepOperation(buildstep, continueEvenFailed, createLauncher);
        }
        
        /**
         * @return true when {@link BuildStepOperation} is enabled in system configuration.
         */
        @Override
        public boolean isApplicable(Class<? extends AbstractProject<?, ?>> jobType) {
            return isEnabled();
        }

        @Override
        public String getDisplayName() {
            return Messages.BuildStepOperation_DisplayName();
        }

        public List<Descriptor<?>> getBuildStepDescriptors(AbstractProject<?,?> project) {
            List<Descriptor<?>> buildsteps = new ArrayList<>();
            buildsteps.addAll(BuildStepDescriptor.filter(Builder.all(), project.getClass()));
            buildsteps.addAll(BuildStepDescriptor.filter(Publisher.all(), project.getClass()));
            
            return buildsteps;
        }
    }
}
